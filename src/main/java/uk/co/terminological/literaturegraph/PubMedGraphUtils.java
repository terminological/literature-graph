package uk.co.terminological.literaturegraph;


import uk.co.terminological.bibliography.crossref.CrossRefReference;
import uk.co.terminological.bibliography.crossref.CrossRefWork;
import uk.co.terminological.bibliography.entrez.EntrezLink;
import uk.co.terminological.bibliography.entrez.EntrezMeshCode;
import uk.co.terminological.bibliography.entrez.EntrezEntry;
import uk.co.terminological.bibliography.record.Author;
import uk.co.terminological.bibliography.unpaywall.UnpaywallResult;
import uk.co.terminological.literaturegraph.PubMedGraphSchema.Labels;
import uk.co.terminological.literaturegraph.PubMedGraphSchema.Prop;
import uk.co.terminological.literaturegraph.PubMedGraphSchema.Rel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubMedGraphUtils {

	private static Node lockNode;
	private static final Logger logger = LoggerFactory.getLogger(PubMedGraphUtils.class);

	public static Node doMerge(Label label, String indexName, String indexValue, GraphDatabaseService graphDb) {
		return doMerge(label,indexName,indexValue,graphDb, null);
	}
	public static Node doMerge(Label label, String indexName, String indexValue, GraphDatabaseService graphDb, Label label2) {
		String queryString = "MERGE (n:"+label.name()+" {"+indexName+": $"+indexName+"})"+
				(label2!=null ? " ON CREATE SET n:"+label2.name():"")+" RETURN n";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put( indexName, indexValue );
		ResourceIterator<Node> resultIterator = graphDb.execute( queryString, parameters ).columnAs( "n" );
		Node result = resultIterator.next();
		return result;	
	}
	
	public static Relationship doMerge(Label srcLabel, String srcIndexName, String srcIndexValue,RelationshipType relType, Label targetLabel, String targetIndexName, String targetIndexValue, GraphDatabaseService graphDb) {
		String queryString = 
				"MATCH "+
						"(n:"+srcLabel.name()+" {"+srcIndexName+": $src_"+srcIndexName+"}), "+
						"(m:"+targetLabel.name()+" {"+targetIndexName+": $target_"+targetIndexName+"}) "+
				"MERGE "+
						"(n)-[r:"+relType.name()+"]->(m) "+
				"RETURN r";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put( "src_"+srcIndexName, srcIndexValue );
		parameters.put( "target_"+targetIndexName, targetIndexValue );
		ResourceIterator<Relationship> resultIterator = graphDb.execute( queryString, parameters ).columnAs( "r" );
		Relationship result = resultIterator.next();
		return result;	
	}

	public static void addLabelsByIds(Label existingLabel, String indexProp, Set<?> values, Label newLabel, GraphDatabaseApi graph) {
		try ( Transaction tx = graph.get().beginTx() ) {
			tx.acquireWriteLock(getLockNode());
			values.forEach(v -> {
				Node tmp = graph.get().findNode(existingLabel, indexProp, v);
				if (tmp != null) {	
					tmp.addLabel(newLabel);
				} else {
					logger.debug("No {} found for {} with value of {}", existingLabel.name(),indexProp,v.toString());
				}
			});
			tx.success();
		}
	}
	
	private static Node mergeNodes(Node tmp1, Node tmp2, Transaction tx) {
		tmp2.getAllProperties().forEach((k,v) -> tmp1.setProperty(k, v));
		tmp2.getRelationships(Direction.INCOMING).forEach(r -> {
			Node other = r.getOtherNode(tmp2);
			Relationship r2 = other.createRelationshipTo(tmp1, r.getType());
			r.getAllProperties().forEach((k,v) -> r2.setProperty(k, v));
			r.delete();
		});
		tmp2.getRelationships(Direction.OUTGOING).forEach(r -> {
			Node other = r.getOtherNode(tmp2);
			Relationship r2 = tmp1.createRelationshipTo(other, r.getType());
			r.getAllProperties().forEach((k,v) -> r2.setProperty(k, v));
			r.delete();
		});
		tmp2.getLabels().forEach(l -> tmp1.addLabel(l));
		tmp2.delete();
		return tmp1;
	}
	
	
	public static List<Node> mapEntriesToNode(Stream<EntrezEntry> entries, GraphDatabaseApi graph, LocalDate earliest, LocalDate latest, Label... additional) {

		List<Node> out = new ArrayList<>();

		try ( Transaction tx = graph.get().beginTx() ) {
			tx.acquireWriteLock(getLockNode());
			
			entries.forEach(entry -> {
				Node tmp = null;
				Node tmp1 = entry.getPMID().isPresent() ? graph.get().findNode(Labels.ARTICLE, Prop.PMID, entry.getPMID().get()) : null;
				Node tmp2 = entry.getDoi().isPresent() ? graph.get().findNode(Labels.ARTICLE, Prop.DOI, entry.getDoi().get().toLowerCase()) : null;
				//Node tmp3 = entry.getPMCID().isPresent() ? graph.get().findNode(ARTICLE, Prop.PMCID, entry.getPMCID().get()) : null;
				
				if (tmp1 == null && tmp2 == null) {// && tmp3 == null) {
					logger.debug("Creating new article record: pmid"+entry.getPMID().orElse("none")+" doi:"+entry.getDoi().orElse("none"));
					Node newNode = graph.get().createNode(Labels.ARTICLE);
					entry.getPMID().ifPresent(pmid -> newNode.setProperty(Prop.PMID, pmid));
					entry.getDoi().ifPresent(doi -> newNode.setProperty(Prop.DOI, doi.toLowerCase()));
					entry.getPMCID().ifPresent(pmcid -> newNode.setProperty(Prop.PMCID, pmcid));
					tmp = newNode;
				} else {
				
					//Merge pubmed and doi stubs
					if (tmp1 != null && tmp2 != null && tmp1.getId() != tmp2.getId()) {
						logger.debug("Merging article pubmed: "+entry.getPMID().get()+" with doi: "+entry.getDoi().get());
						//merge tmp1 and tmp2
						entry.getPMCID().ifPresent(pmcid -> tmp1.setProperty(Prop.PMCID, pmcid));
						tmp = mergeNodes(tmp1,tmp2,tx);
					} 
					
				
					if (tmp == null) {
						if (tmp1!=null) {
							logger.debug("Updating pubmed article: "+entry.getPMID().get());
							entry.getDoi().ifPresent(doi -> tmp1.setProperty(Prop.DOI, doi.toLowerCase()));
							entry.getPMCID().ifPresent(pmcid -> tmp1.setProperty(Prop.PMCID, pmcid));
							tmp = tmp1;
						} else if (tmp2!=null) {
							logger.debug("Updating doi article: "+entry.getDoi().get());
							entry.getPMID().ifPresent(pmid -> tmp2.setProperty(Prop.PMID, pmid));
							entry.getPMCID().ifPresent(pmcid -> tmp2.setProperty(Prop.PMCID, pmcid));
							tmp = tmp2;
						} else {
							//cannot happen because of check at beginning
						}
					}
				}
				Node node = tmp;
				
				entry.getPMCID().ifPresent(pmc -> node.setProperty(Prop.PMCID, pmc));
				entry.getPdfUri().ifPresent(url -> node.setProperty(Prop.PDF_URL, url.toString()));
				entry.getDate().ifPresent(dt -> {
					node.setProperty(Prop.DATE, dt);
					/*if (dt.isAfter(earliest) && dt.isBefore(latest)) {
						node.addLabel(EXPAND);
					} else {
						logger.debug("not expanding: date="+dt.format(DateTimeFormatter.ISO_LOCAL_DATE)+" title="+entry.getTitle() );
					}*/
				});
				entry.getAbstract().ifPresent(abs -> node.setProperty(Prop.ABSTRACT, abs));
				entry.getTitle().ifPresent(title -> node.setProperty(Prop.TITLE, title));
				entry.getJournal().ifPresent(journal -> node.setProperty(Prop.JOURNAL, journal));
				node.removeLabel(Labels.DOI_STUB);
				node.removeLabel(Labels.PMID_STUB);
				node.removeLabel(Labels.PMCENTRAL_STUB);
				
				Arrays.asList(additional).forEach(l -> node.addLabel(l));
					
				entry.getKeyWords().forEach(kw -> {
					Node target = doMerge(Labels.KEYWORD, Prop.TERM, kw, graph.get());
					node.createRelationshipTo(target, Rel.HAS_KEYWORD);
				});
				boolean first = true;
				for (Author au:entry.getAuthors()) {
					Optional<Node> targetNode = mapAuthorToNode(au,graph, tx);
					if (targetNode.isPresent()) {
						Relationship r = node.createRelationshipTo(targetNode.get(), Rel.HAS_AUTHOR);
						if (first) {
							r.setProperty(Prop.FIRST_AUTHOR, true);
							
						} else {
							r.setProperty(Prop.FIRST_AUTHOR, false);
						}
					}
					first = false;
				}
				entry.getMeshHeadings().forEach(mh -> {
					Optional<Node> targetNode = mapMeshCodeToNode(mh.getDescriptor(),graph, tx);
					targetNode.ifPresent(target -> node.createRelationshipTo(target, Rel.HAS_MESH));
				});
				out.add(node);
			});
			tx.success();
		}


		return out;

	}

	public static Optional<Node> mapAuthorToNode(uk.co.terminological.bibliography.record.Author author, GraphDatabaseApi graph, Transaction tx) {
		Node out = null;

			Node node = graph.get().createNode(Labels.AUTHOR);// doMerge(AUTHOR, "identifier", identifier, graph.get());
			node.setProperty(Prop.AUTHOR_LABEL, author.getLabel());
			node.setProperty(Prop.LAST_NAME, author.getLastName());
			author.getFirstName().ifPresent(fn -> node.setProperty(Prop.FIRST_NAME, fn));
			author.getInitials().ifPresent(fn -> node.setProperty(Prop.INITIALS, fn));
			author.getORCID().ifPresent(fn -> node.setProperty(Prop.ORCID, fn));
			author.getAffiliations().forEach(af -> {
				Node node2 = graph.get().createNode(Labels.AFFILIATION);
				node2.setProperty(Prop.ORGANISATION_NAME, af);
				node.createRelationshipTo(node2, Rel.HAS_AFFILIATION);
			});
			out = node;

		return Optional.ofNullable(out);

	}
	
	

	public static Optional<Node> mapMeshCodeToNode(EntrezMeshCode meshCode, GraphDatabaseApi graph, Transaction tx) {
		Node out = null;

			Node tmp = doMerge(Labels.MESH_CODE, Prop.CODE, meshCode.getCode(), graph.get());
			tmp.setProperty(Prop.CODE, meshCode.getCode());
			tmp.setProperty(Prop.TERM, meshCode.getTerm());
			out = tmp;

		return Optional.ofNullable(out);
	}
	
	public static List<Relationship> mapCrossRefReferences(String citingDoi, List<CrossRefReference> citedDois, GraphDatabaseApi graph) {
		return mapHasReferences(Prop.DOI,citingDoi,Labels.DOI_STUB,Prop.DOI,citedDois,Labels.DOI_STUB, Rel.HAS_REFERENCE,graph);
	}
	
	public static List<Relationship> mapHasReferences(String citingType, String citingDoi, Label citingStubLabel, String citedType, List<CrossRefReference> citedDois, Label citedStubLabel, RelationshipType relType, GraphDatabaseApi graph) {
		List<Relationship> out = new ArrayList<>();
			
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			//Node start = 
					doMerge(Labels.ARTICLE, citingType, citingDoi.toLowerCase(), graph.get(), citingStubLabel);
			citedDois.forEach(cite -> {
				cite.getIdentifier().ifPresent(citedDoi -> {
					
					Node end = doMerge(Labels.ARTICLE, citedType, citedDoi.toLowerCase(),graph.get(), citedStubLabel);
					//TODO: there is more metadata available here but it is not all written in
					cite.getFirstAuthorName().ifPresent(auth -> end.setProperty(Prop.AUTHOR_LABEL, auth));
					cite.getJournal().ifPresent(o -> end.setProperty(Prop.JOURNAL, o));
					cite.getTitle().ifPresent(o -> end.setProperty(Prop.TITLE, o));
					
					Relationship tmp = doMerge(Labels.ARTICLE, citingType, citingDoi.toLowerCase() ,relType, Labels.ARTICLE, citedType, citedDoi.toLowerCase(),graph.get() );
					tmp.setProperty(Prop.CROSSREF, true);
					if (end.hasLabel(citedStubLabel)) {
						cite.getTitle().ifPresent(t -> end.setProperty(Prop.TITLE, t));
					}
					out.add(tmp);
				});
			});
			tx.success();
		}
		logger.debug("Adding {}:{} <-{}- {}:{}}",citedDois.size(),citedStubLabel,relType,citingDoi,citingType);
		return out;
	}
	
	public static Set<String> mapCermineReferences(String citingDoi, Set<CrossRefWork> citedDois, GraphDatabaseApi graph) {
		return citedDois.stream().flatMap(work -> {
			return mapCermineReference(Prop.DOI,citingDoi,Labels.DOI_STUB,Prop.DOI,work,Labels.DOI_STUB, Rel.HAS_REFERENCE,graph).stream();
		}).collect(Collectors.toSet());
	}
	
	public static Optional<String> mapCermineReference(String citingType, String citingDoi, Label citingStubLabel, String citedType, CrossRefWork cite, Label citedStubLabel, RelationshipType relType, GraphDatabaseApi graph) {
		updateCrossRefMetadata(cite, graph);
		Optional<String> out = Optional.empty();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			//Node start = 
			doMerge(Labels.ARTICLE, citingType, citingDoi.toLowerCase(), graph.get(), citingStubLabel);
			out = cite.getIdentifier().map(citedDoi -> {
					Relationship tmp = doMerge(Labels.ARTICLE, citingType, citingDoi.toLowerCase() ,relType, Labels.ARTICLE, citedType, citedDoi.toLowerCase(),graph.get() );
					tmp.setProperty(Prop.HAS_PDF, true);
					return citedDoi;
			});
			tx.success();
		}
		return out;
		
	}

	public static List<Relationship> mapPubmedRelated(List<EntrezLink> links, GraphDatabaseApi graph) {
		return mapEntrez(links, Prop.PMID, Labels.PMID_STUB, Prop.PMID, Labels.PMID_STUB, Rel.HAS_RELATED, graph, false);
	}
	
	public static List<Relationship> mapPubMedCentralReferences(List<EntrezLink> links, GraphDatabaseApi graph) {
		return mapEntrez(links, Prop.PMID, Labels.PMID_STUB, Prop.PMID, Labels.PMID_STUB, Rel.HAS_REFERENCE, graph, false);
	}
	
	public static List<Relationship> mapPubMedCentralCitedBy(List<EntrezLink> links, GraphDatabaseApi graph) {
		return mapEntrez(links, Prop.PMID, Labels.PMID_STUB, Prop.PMID, Labels.PMID_STUB, Rel.HAS_REFERENCE, graph, true);
	}
	
	public static List<Relationship> mapEntrez(List<EntrezLink> links, String inIdType, Label inLabel, String outIdType, Label outLabel, RelationshipType relType, GraphDatabaseApi graph, boolean invert) {
		logger.debug("Adding {} entries {}:{} <-{}- {}:{}",links.size(), outIdType, outLabel, relType, inIdType, inLabel);
		List<Relationship> out = new ArrayList<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());

			links.forEach(link -> { 
				link.toId.ifPresent(toId -> {
					//Node start = 
							doMerge(Labels.ARTICLE, inIdType, link.fromId, graph.get(), inLabel);
					//Node end = 
							doMerge(Labels.ARTICLE, outIdType, toId, graph.get(), outLabel);
					Relationship tmp = null;
					if (invert) {
						tmp = doMerge(Labels.ARTICLE, outIdType, toId, relType, Labels.ARTICLE, inIdType, link.fromId, graph.get());
					} else {
						tmp = doMerge(Labels.ARTICLE, inIdType, link.fromId, relType, Labels.ARTICLE, outIdType, toId, graph.get());
					}
					if (link.score.isPresent()) tmp.setProperty(Prop.RELATEDNESS, link.score.get());
					tmp.setProperty(Prop.ENTREZ, true);
				});
			});
			tx.success();
		}

		return out;
	}
	
	
	
	public static Optional<String> updateCrossRefMetadata(CrossRefWork work, GraphDatabaseApi graph) {
		if (work.getIdentifier().isPresent()) {
			
			try (Transaction tx = graph.get().beginTx()) {
				tx.acquireWriteLock(getLockNode());

				Node node = doMerge(Labels.ARTICLE, Prop.DOI, work.getIdentifier().get().toLowerCase(), graph.get());
				work.getTitle().ifPresent(title -> node.setProperty(Prop.TITLE, title));
				boolean first = true;
				for (Author au:work.getAuthors()) {
					Optional<Node> targetNode = mapAuthorToNode(au,graph, tx);
					if (targetNode.isPresent()) {
						Relationship r = node.createRelationshipTo(targetNode.get(), Rel.HAS_AUTHOR);
						if (first) {
							r.setProperty(Prop.FIRST_AUTHOR, true);
							
						} else {
							r.setProperty(Prop.FIRST_AUTHOR, false);
						}
					}
					first = false;
				}
				work.getAbstract().ifPresent(abs -> node.setProperty(Prop.ABSTRACT, abs));
				work.getDate().ifPresent(date -> node.setProperty(Prop.DATE,date));
				work.getJournal().ifPresent(journal -> node.setProperty(Prop.JOURNAL,journal));
				work.getCitedByCount().ifPresent(cited -> node.setProperty(Prop.CITED_BY, cited));
				work.getReferencesCount().ifPresent(cites -> node.setProperty(Prop.REFERENCE_COUNT, cites));
				node.removeLabel(Labels.DOI_STUB);
				node.removeLabel(Labels.PMID_STUB);
				node.removeLabel(Labels.PMCENTRAL_STUB);
				tx.success();
				
			}
			return Optional.of(work.getIdentifier().get().toLowerCase());
		}
		return Optional.empty();
	}

	public static Optional<String> updateUnpaywallMetadata(UnpaywallResult work, GraphDatabaseApi graph) {
		if (work.getIdentifier().isPresent()) {
			try (Transaction tx = graph.get().beginTx()) {
				tx.acquireWriteLock(getLockNode());
				Node node = doMerge(Labels.ARTICLE, Prop.DOI, work.getIdentifier().get().toLowerCase(), graph.get());
				work.getPdfUri().ifPresent(url -> node.setProperty(Prop.PDF_URL, url.toString()));
				work.getDate().ifPresent(date -> node.setProperty(Prop.DATE, date));
				work.getTitle().ifPresent(title -> node.setProperty(Prop.TITLE, title));
				work.getJournal().ifPresent(journal -> node.setProperty(Prop.JOURNAL, journal));
				node.removeLabel(Labels.DOI_STUB);
				node.removeLabel(Labels.PMID_STUB);
				node.removeLabel(Labels.PMCENTRAL_STUB);
				tx.success();
			}
			return Optional.of(work.getIdentifier().get().toLowerCase());
		}
		return Optional.empty();
	}
	
	public static Optional<String> updatePdfLink(UnpaywallResult work, GraphDatabaseApi graph) {
		if (work.getIdentifier().isPresent()) {
			try (Transaction tx = graph.get().beginTx()) {
				tx.acquireWriteLock(getLockNode());
				Node node = doMerge(Labels.ARTICLE, Prop.DOI, work.getIdentifier().get().toLowerCase(), graph.get());
				work.getPdfUri().ifPresent(url -> node.setProperty(Prop.PDF_URL, url.toString()));
				tx.success();
			}
			return Optional.of(work.getIdentifier().get().toLowerCase());
		}
		return Optional.empty();
	}
	
	public static Set<String> lookupDoisForUnreferenced(GraphDatabaseApi graph) {
		Set<String> out = new HashSet<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			ResourceIterator<String> resultIterator = graph.get().execute("MATCH (source:Article) WHERE NOT (source)-[:HAS_REFERENCE]->() AND source.doi IS NOT NULL RETURN source.doi AS out").columnAs("out"); 
			resultIterator.forEachRemaining(doi -> out.add(doi));
			tx.success();
		}
		return out;
	}; 
	
	public static Set<String> lookupPMIDSForUnreferenced(GraphDatabaseApi graph) {
		Set<String> out = new HashSet<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			ResourceIterator<String> resultIterator = graph.get().execute("MATCH (source:Article) WHERE NOT (source)-[:HAS_REFERENCE]->() AND source.pmid IS NOT NULL RETURN source.pmid AS out").columnAs("out"); 
			resultIterator.forEachRemaining(doi -> out.add(doi));
			tx.success();
		}
		return out;
	};
	
	public static Set<String> lookupDoisForUnknownCitedBy(GraphDatabaseApi graph) {
		Set<String> out = new HashSet<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			ResourceIterator<String> resultIterator = graph.get().execute("MATCH (source:Article) WHERE source.doi IS NOT NULL AND source.citedByCount IS NULL RETURN source.doi AS out").columnAs("out"); 
			resultIterator.forEachRemaining(doi -> out.add(doi));
			tx.success();
		}
		return out;
	};

	public static Set<String> lookupPmidStubs(GraphDatabaseApi graph) {
		Set<String> out = new HashSet<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			ResourceIterator<String> resultIterator = graph.get().execute("MATCH (source:PMIDStub) RETURN source.pmid AS out").columnAs("out"); 
			resultIterator.forEachRemaining(doi -> out.add(doi));
			tx.success();
		}
		return out;
	};
	
	public static Set<String> lookupDoiStubs(GraphDatabaseApi graph) {
		Set<String> out = new HashSet<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			ResourceIterator<String> resultIterator = graph.get().execute("MATCH (source:DOIStub) RETURN source.doi AS out").columnAs("out"); 
			resultIterator.forEachRemaining(doi -> out.add(doi));
			tx.success();
		}
		return out;
	};
	
	
	public static Set<String> lookupBroadSearchDois(GraphDatabaseApi graph) {
		Set<String> out = new HashSet<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			ResourceIterator<String> resultIterator = graph.get().execute("MATCH (source:Expand) WHERE source.doi IS NOT NULL RETURN source.doi AS out").columnAs("out"); 
			resultIterator.forEachRemaining(doi -> out.add(doi));
			tx.success();
		}
		return out;
	};
	
	public static Set<String> lookupDoisMissingPMID(GraphDatabaseApi graph) {
		Set<String> out = new HashSet<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			ResourceIterator<String> resultIterator = graph.get().execute("MATCH (source:Article) WHERE source.doi IS NOT NULL AND source.pmid IS NULL RETURN source.doi AS out").columnAs("out"); 
			resultIterator.forEachRemaining(doi -> out.add(doi));
			tx.success();
		}
		return out;
	};
	
	public static Set<String> lookupPMIDSMissingDoi(GraphDatabaseApi graph) {
		Set<String> out = new HashSet<>();
		try (Transaction tx = graph.get().beginTx()) {
			tx.acquireWriteLock(getLockNode());
			ResourceIterator<String> resultIterator = graph.get().execute("MATCH (source:Article) WHERE source.doi IS NULL AND source.pmid IS NOT NULL RETURN source.pmid AS out").columnAs("out"); 
			resultIterator.forEachRemaining(doi -> out.add(doi));
			tx.success();
		}
		return out;
	}
	public static Node getLockNode() {
		return lockNode;
	}
	public static void setLockNode(Node lockNode) {
		PubMedGraphUtils.lockNode = lockNode;
	};
	
}
