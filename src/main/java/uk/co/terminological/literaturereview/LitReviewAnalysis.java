//package uk.co.terminological.literaturereview;
//
//import static uk.co.terminological.simplechart.Chart.Dimension.ID;
//import static uk.co.terminological.simplechart.Chart.Dimension.STRENGTH;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.OutputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.Properties;
//import java.util.Set;
//import java.util.SortedSet;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.apache.log4j.BasicConfigurator;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.neo4j.driver.v1.AuthTokens;
//import org.neo4j.driver.v1.Config;
//import org.neo4j.driver.v1.Driver;
//import org.neo4j.driver.v1.GraphDatabase;
//import org.neo4j.driver.v1.Record;
//import org.neo4j.driver.v1.Session;
//import org.neo4j.driver.v1.StatementResult;
//import org.neo4j.driver.v1.Transaction;
//import org.neo4j.driver.v1.Value;
//import org.neo4j.driver.v1.Values;
//import org.neo4j.driver.v1.types.Node;
//import org.yaml.snakeyaml.Yaml;
//
//import com.google.common.collect.Streams;
//
//import uk.co.terminological.bibliography.BibliographicApiException;
//import uk.co.terminological.bibliography.BibliographicApis;
//import uk.co.terminological.bibliography.CiteProcProvider;
//import uk.co.terminological.bibliography.CiteProcProvider.Format;
//import uk.co.terminological.bibliography.entrez.EntrezEntries;
//import uk.co.terminological.bibliography.entrez.EntrezSearch;
//import uk.co.terminological.bibliography.record.PrintRecord;
//import uk.co.terminological.datatypes.EavMap;
//import uk.co.terminological.datatypes.StreamExceptions;
//import uk.co.terminological.datatypes.Triple;
//import uk.co.terminological.literaturegraph.Shim;
//import uk.co.terminological.nlptools.ColourScheme;
//import uk.co.terminological.nlptools.Corpus;
//import uk.co.terminological.nlptools.Document;
//import uk.co.terminological.nlptools.Filter;
//import uk.co.terminological.nlptools.TopicModelResult;
//import uk.co.terminological.nlptools.TopicModelBuilder;
//import uk.co.terminological.nlptools.Weighted;
//import uk.co.terminological.nlptools.WordCloudBuilder;
//import uk.co.terminological.nlptools.Counted;
//import uk.co.terminological.simplechart.ChartType;
//
//import uk.co.terminological.simplechart.Figure;
//
///*
// * TODO:
// * What do I need to do to get this over the line.
// * 
// * Methods
// * 
// * Sample selection
// * Stats about the collection process
// * 
// * 
// * Results
// * 
// * Temporal composition; aticle network analysis islands / sizes - horizontal staked bar graph.
// * Top N articles by page rank 
// * Top N journals by aggregated page rank
// * 
// * Co-citation authors network
// * 
// * 
// * Communities
// * 
// * Top Y topics based on title / abstract
// * 
// * 
// */
//
//public class LitReviewAnalysis {
//
//	/*public static void main(String[] args) throws Exception {
//		LitReviewAnalysis tmp = new LitReviewAnalysis();
//		tmp.setUpBeforeClass();
//		tmp.articlesCSL();
//	}*/
//
//	Driver driver;
//	Map<String, Object> obj;
//	Figure fig;
//	List<String> affiliationStopwords;
//	List<String> textStopwords;
//	Map<String,String> queries;
//	// List<Integer> communityIndex = new ArrayList<>();
//	Path outDir;
//	Path cacheDir;
//	Path secretsFile;
//	Properties prop;
//	
//
//	/*private String getCommunityName(int community) {
//		int i = getCommunityIndex(community);
//		return "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(i % 26, i % 26 + 1);
//	}
//
//	private int getCommunityIndex(int community) {
//		Integer i = communityIndex.indexOf(community);
//		if (i == -1) {
//			i = communityIndex.size();
//			communityIndex.add(community);
//		}
//		return i;
//	}*/
//
//	/*void plot(Figure fig, String name, List<String> list, Integer community, List<String> stopwords) {
//		try {
//			Integer i = getCommunity(community);
//			fig.withNewChart(name+" "+i, ChartType.WORDCLOUD)
//			.withSeries(list)
//			.bind(TEXT, t -> t)
//			.withColourScheme(ColourScheme.sequential(i))
//			.done()	
//			.withSeries(stopwords).bind(TEXT, t -> t).done()
//			.render();
//		} catch (Exception e) {throw new RuntimeException(e);}
//	};*/
//	@BeforeClass
//	public static void setUpBeforeClass() {
//		BasicConfigurator.configure();
//	}
//
//	@SuppressWarnings("unchecked")
//	@Before
//	public void setUp() throws Exception {
//		
//		Config config = Config.builder()
//				.withMaxConnectionLifetime( 30, TimeUnit.MINUTES )
//				.withMaxConnectionPoolSize( 50 )
//				.withConnectionAcquisitionTimeout( 2, TimeUnit.MINUTES )
//				.build();
//
//		driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "neo4j" ), config );
//
//		Yaml yaml = new Yaml();
//		InputStream inputStream = CitationGraphAnalyser.class.getClassLoader().getResourceAsStream("cypherQuery.yaml");
//		obj = yaml.load(inputStream);
//		
//		String propFilename = "~/Dropbox/litReview/project.prop";
//		Path propPath= Paths.get(propFilename.replace("~", System.getProperty("user.home")));
//		prop = System.getProperties();
//		prop.load(Files.newInputStream(propPath));
//
//		prop.forEach((k,v) -> prop.put(k, v.toString().replace("~", System.getProperty("user.home")))); 
//		outDir = fromProperty(prop,"output-directory");
//		cacheDir = fromProperty(prop,"working-directory");
//		secretsFile = fromProperty(prop,"bibliography-secrets");
//		
//		fig = Figure.outputTo(outDir.toFile());
//
//		affiliationStopwords = Arrays.asList(((Map<String,String>) obj.get("config")).get("stopwordsForAffiliation").split("\n"));
//		textStopwords = Arrays.asList(((Map<String,String>) obj.get("config")).get("stopwordsForText").split("\n"));
//		queries = (Map<String,String> ) obj.get("analyse");
//	}
//	
//	static Path fromProperty(Properties prop, String name) {
//		return Paths.get(prop.getProperty(name).replace("~", System.getProperty("user.home")));
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		driver.close();
//	}
//
//	/*@Test
//	public void articlesCSL() {
//		try ( Session session = driver.session() ) {
//
//			//Plot by age
//			session.readTransaction( tx -> {
//
//				String qry = queries.get("getArticlesByPagerank");
//				List<Record> res = tx.run( qry ).list();
//				CiteProcProvider out = new CiteProcProvider();
//
//				res.forEach(r -> {
//					uk.co.terminological.bibliography.record.Record rec = 
//							Shim.recordFacade(r.get("node").asNode());
//					out.add(rec);
//
//				});
//
//				try {
//					System.out.println(out.orderedCitations("ieee", Output.text).makeString());
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//
//				return true;
//			});
//		}
//	}*/
//
//	public static String REFERENCE_FORMAT = "ieee";
//	
//	@Test
//	/*
//	 * Writes every query defined in cypherQuery.yaml to a csv file. 
//	 */
//	public void writeToCsv() {
//		try ( Session session = driver.session() ) {
//			queries.forEach((name,qry) -> {
//				if (name.startsWith("get")) {
//					System.out.println("Executing query: "+name);
//					session.readTransaction( tx -> {
//
//						StatementResult qryR = tx.run( qry );
//						List<Record> res = qryR.list();
//
//						CiteProcProvider prov = CiteProcProvider.create(REFERENCE_FORMAT, Format.text);
//						res.forEach(r -> {
//							r.values().forEach(f -> {
//								try {
//									Node n = f.asNode();
//									PrintRecord tmp = Shim.recordFacade(n);
//									prov.add(tmp);
//								} catch (Exception e) {}
//							});
//						});
//
//
//						Path path = outDir.resolve(name+".tsv");
//						try {
//							List<String> cits = new ArrayList<>();
//							if (!prov.isEmpty()) {
//								cits = Arrays.asList(
//										prov.orderedCitations().getEntries());
//							}
//
//							OutputStream writer = Files.newOutputStream(path);
//							writer.write(qryR.keys().stream().collect(Collectors.joining("\t")).getBytes());
//							int i = 0;
//							for (Record r:res) {
//								StringBuilder line = new StringBuilder();
//								for (Value v :r.values()) {
//									if (line.length() != 0) line.append("\t");
//									try {
//										v.asNode();
//										String s = cits.get(i).trim();
//										line.append(s);
//									} catch (Exception e) {
//										line.append(v.toString().replace("\n", " "));
//									}
//								}
//								writer.write(("\n"+line.toString()).getBytes());
//								i++;
//							}
//							writer.write('\n');
//							writer.close();
//
//						} catch (Exception e) {
//							e.printStackTrace(System.out);
//						}
//
//						return true;
//					});
//				}
//			});
//		}
//
//	}
//
//	/*@Test
//	@Deprecated
//	// Probably better to export and do in R
//	public void plotAgeOfArticles() {
//		try ( Session session = driver.session() ) {
//
//			//Plot by age
//			session.readTransaction( tx -> {
//
//				String qry = queries.get("getArticlesByAge");
//				List<Record> res = tx.run( qry ).list();
//
//				try {
//					fig.withNewChart("Articles by age", ChartType.XYSCATTER)
//					.withSeries(res)
//					.bind(X, t -> t.get("qtr").asDouble()/4)
//					.bind(Y, t -> t.get("articles").asInt())
//					.done()
//					.config()
//					.withXLabel("years elapsed")
//					.withYLabel("articles")
//					.done().render();
//				} catch (Exception e) {throw new RuntimeException(e);}
//
//				return true;
//			});
//		}
//	}*/
//
//	/*@Test 
//	@Deprecated
//	public void plotArticlesByJournal() {
//		try ( Session session = driver.session() ) {
//
//			//Plot by age
//			session.readTransaction( tx -> {
//
//				String qry = queries.get("getArticlesByJournal");
//				List<Record> res = tx.run( qry ).list();
//
//				try {
//					fig.withNewChart("Articles by journal", ChartType.STACKEDYBAR)
//					.withSeries(res)
//					.bind(LABEL, t -> t.get("journal").asString())
//					.bind(Y, t -> t.get("articles").asNumber().intValue())
//					.done()
//					.config()
//					.withYLabel("articles")
//					.done().render();
//				} catch (Exception e) {throw new RuntimeException(e);}
//
//				return true;
//			});
//		}
//	}*/
//
//	/*@Test
//	@Deprecated
//	//Export and do in R
//	public void plotCommunityStats() {
//		try ( Session session = driver.session() ) {
//
//			//Plot by age
//			session.readTransaction( tx -> {
//
//				String qry = queries.get("getAuthorCommunityStats");
//				List<Record> res = tx.run( qry ).list();
//
//				EavMap<String,String,Double> tmp = new EavMap<>();
//
//				for (String key:Arrays.asList("authors","articles","avgPagerank")) {
//
//					Double[] sum = {0D};
//
//					res.stream().forEach(r -> {
//						tmp.add(
//								r.get("community").asNumber().toString(), key, r.get(key).asNumber().doubleValue());
//						sum[0] += r.get(key).asNumber().doubleValue();
//					});
//
//					//Convert to percentage
//					tmp.getEntitySet().forEach(e -> {
//						tmp.put(e, key, tmp.get(e, key)/sum[0]*100);
//					});
//
//				}
//
//				try {
//					fig.withNewChart("Community stats", ChartType.MULTISTACKEDYBAR)
//					.withSeries(tmp.stream())
//					.bind(LABEL, t -> t.getFirst())
//					.bind(X, t -> t.getSecond())
//					.bind(Y, t -> t.getThird())
//					.done()
//					.config()
//					.withYLabel("articles")
//					.withXLabel("facet")
//					.done().render();
//				} catch (Exception e) {throw new RuntimeException(e);}
//
//				return true;
//			});
//		}
//	}*/
//
//	@Test
//	public void plotCooccurrenceOfMeshCodes() {
//		try ( Session session = driver.session() ) {
//			session.readTransaction( tx -> {
//
//				String qry = queries.get("getMeshCodeCooccurMutualInformation");
//				List<Record> res = tx.run( qry ).list();
//				//List<Triple<String,Double,String>> links = new ArrayList<>();
//				Set<String> nodes = new HashSet<>();
//				List<Triple<String,Double,String>> links = new ArrayList<>();
//				List<Triple<String,Integer,String>> links2 = new ArrayList<>();
//
//				res.forEach(r -> {
//					String sourceTerm = r.get("sourceTerm").asString();
//					String targetTerm = r.get("targetTerm").asString();
//					Integer cooccurrenceCount = r.get("cooccurrences").asInt();
//					Double npmi = r.get("npmi").asDouble();
//
//					if (nodes.size() < 50) {
//						nodes.add(sourceTerm);
//						nodes.add(targetTerm);
//					}
//
//					if (nodes.contains(sourceTerm) && nodes.contains(targetTerm)) {
//						links.add(Triple.create(sourceTerm, npmi, targetTerm));
//						links2.add(Triple.create(sourceTerm, cooccurrenceCount, targetTerm));
//					}	
//
//				});
//
//				try {
//					fig.withNewChart("Mesh codes by npmi", ChartType.CHORD)
//					.withSeries(links)
//					.bind(ID, t -> t.getFirst(), "source")
//					.bind(STRENGTH, t -> t.getSecond())
//					.bind(ID, t -> t.getThird(), "target")
//					.withColourScheme(ColourScheme.Accent)
//					.done()
//					.render();
//
//					fig.withNewChart("Mesh codes by cooccurrence", ChartType.CHORD)
//					.withSeries(links2)
//					.bind(ID, t -> t.getFirst(), "source")
//					.bind(STRENGTH, t -> t.getSecond())
//					.bind(ID, t -> t.getThird(), "target")
//					.withColourScheme(ColourScheme.Set1)
//					.done()
//					.render();
//				} catch (Exception e) {throw new RuntimeException(e);}
//
//				return true;
//			});
//		}
//	}
//
//	@Test
//	public void plotCooccurrenceOfKeywords() {
//		try ( Session session = driver.session() ) {
//			session.readTransaction( tx -> {
//
//				String qry = queries.get("getKeywordCooccurMutualInformation");
//				List<Record> res = tx.run( qry ).list();
//				//List<Triple<String,Double,String>> links = new ArrayList<>();
//				Set<String> nodes = new HashSet<>();
//				List<Triple<String,Double,String>> links = new ArrayList<>();
//				List<Triple<String,Integer,String>> links2 = new ArrayList<>();
//
//				res.forEach(r -> {
//					String sourceTerm = r.get("sourceTerm").asString();
//					String targetTerm = r.get("targetTerm").asString();
//					Integer cooccurrenceCount = r.get("cooccurrences").asInt();
//					Double npmi = r.get("npmi").asDouble();
//
//					if (nodes.size() < 50) {
//						nodes.add(sourceTerm);
//						nodes.add(targetTerm);
//					}
//
//					if (nodes.contains(sourceTerm) && nodes.contains(targetTerm)) {
//						links.add(Triple.create(sourceTerm, npmi, targetTerm));
//						links2.add(Triple.create(sourceTerm, cooccurrenceCount, targetTerm));
//					}	
//				});
//
//
//				try {
//					fig.withNewChart("Keywords by npmi", ChartType.CHORD)
//					//.withSeries(new ArrayList<>(nodes))
//					//.bind(ID, t -> t)
//					//.bind(LABEL, t -> t)
//					//.done()
//					.withSeries(links)
//					.bind(ID, t -> t.getFirst(), "source")
//					.bind(STRENGTH, t -> t.getSecond())
//					.bind(ID, t -> t.getThird(), "target")
//					.withColourScheme(ColourScheme.Set2)
//					.done()
//					.render();
//
//					fig.withNewChart("Keywords by cooccurrence", ChartType.CHORD)
//					.withSeries(links2)
//					.bind(ID, t -> t.getFirst(), "source")
//					.bind(STRENGTH, t -> t.getSecond())
//					.bind(ID, t -> t.getThird(), "target")
//					.withColourScheme(ColourScheme.Set3)
//					.done()
//					.render();
//				} catch (Exception e) {throw new RuntimeException(e);}
//
//				return true;
//			});
//
//		}
//	}
//
//	
//
//	/*private void plotCommunityWordcloud(Corpus texts, int community, String type) {
//		WordCloudBuilder.from(texts, 200, 600, 600)
//		.withSelector(c -> c.getTermsByTotalEntropy().map(wt -> wt.scale(100)))
//		.withColourScheme(ColourScheme.sequential(community))
//		.execute(outDir.resolve("Community"+type+community+".png"));
//	}*/
//
//	/*@Test
//	public void plotCommunityAffiliations() {
//		try ( Session session = driver.session() ) {
//
//			session.readTransaction( tx -> {
//
//				String qry = queries.get("getAuthorCommunityAffiliations");
//				List<Record> res = tx.run( qry ).list();
//				Corpus texts = affiliationCorpus();
//
//				Integer community = null;
//				for( Record r : res) {
//					Integer next = r.get("community").asInt();
//					if (community == null) community = next;
//					if (community != next) {
//						plotCommunityWordcloud(texts,community,"Affiliations");
//						texts = affiliationCorpus();
//					}
//					r.get("affiliations").asList(Values.ofString()).forEach(texts::addDocument);
//					community = next;
//				}
//				plotCommunityWordcloud(texts,community,"Affiliations");
//				return true;
//			});
//		}
//	}*/
//
//	static int MAX = 10;
//
//	List<Integer> topCommunities = null;
//
//	private List<Integer> topCommunitiesByArticles(Transaction tx, int size) {
//		if (topCommunities == null) {
//
//			String qry = queries.get("getAuthorCommunityStats");
//			List<Record> res = tx.run( qry ).list();
//			SortedSet<Counted<Integer>> tmp = Counted.descending();
//
//			res.stream().forEach(r -> {
//				tmp.add(Counted.create(
//						r.get("community").asNumber().intValue(),
//						r.get("articles").asNumber().intValue()));
//			});
//
//			topCommunities = tmp.stream().limit(size).map(cc -> cc.getTarget()).collect(Collectors.toList());
//
//		}
//		return topCommunities;
//	}
//
//	
//
//	List<Integer> topArticleCommunities = null;
//
//	private List<Integer> topNArticleCommunities(Transaction tx,int size) {
//		if (topArticleCommunities == null) {
//			String qry = queries.get("getArticleCommunityTitlesAbstracts");
//			List<Record> res = tx.run( qry ).list();
//
//			Map<Integer,Integer> communityCount = new HashMap<>();
//			for( Record r : res) {
//				Integer next = r.get("articleCommunity").asInt();
//				communityCount.merge(next, 1, (v1,v2)->v1+v2);
//			}
//
//			topArticleCommunities = communityCount.entrySet().stream()
//					.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
//					.limit(MAX).map((kv) -> kv.getKey()).collect(Collectors.toList());
//		}
//
//		return topArticleCommunities;
//	}
//
//	@Test
//	public void plotArticleCommunityContent() {
//		try ( Session session = driver.session() ) {
//
//			session.readTransaction( tx -> {
//				String qry = queries.get("getArticleCommunityTitlesAbstracts");
//				List<Record> res = tx.run( qry ).list();
//				Corpus texts = textCorpus();
//
//				for( Record r : res) {
//					Integer next = r.get("articleCommunity").asInt();
//					//Integer size = r.get("size").asInt();
//					String nodeId = r.get("nodeId").asNumber().toString();
//					String title = r.get("title").asString();
//					String abstrct = r.get("abstract").asString();
//					Document doc = texts.addDocument(nodeId, title+(abstrct != null ? "\n"+abstrct : ""));
//					doc.addMetadata("articleCommunity",next);
//				}
//
//				List<Integer> top10articleCommunity = topNArticleCommunities(tx, MAX);
//				for (Integer community: top10articleCommunity) {
//					int id = top10articleCommunity.indexOf(community);
//					WordCloudBuilder.from(texts, 200, 600, 600).circular()
//					.withColourScheme(ColourScheme.sequential3(id).darker(0.25F))
//					.withSelector(c -> c.getTermsByMutualInformation(d -> community.equals(d.getMetadata("articleCommunity").orElse(null)))
//							.map(wt -> wt.scale(10000)))
//					.execute(outDir.resolve("ArticleCommunityContent"+roman(id)+".png"));
//				}
//
//				return true;
//			});
//		}
//	}
//
//	private String letter(int id) {
//		return "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(id % 26, id % 26+1);
//	}
//
//	private String roman(int id) {
//		return (new String[] {"I","II","III","IV","V","VI","VII","VIII","IX","X","XI"})[id];
//	}
//
//	@Test 
//	public void generateCommunityLabels() {
//		try ( Session session = driver.session() ) {
//			session.readTransaction( tx -> {
//
//				List<Integer> article = topNArticleCommunities(tx, MAX);
//				try {
//					OutputStream out2 = Files.newOutputStream(outDir.resolve("getArticleGroupLabels.tsv"));
//					out2.write("articleGroup\tlabel\n".getBytes());
//					article.stream().forEach(t -> 
//					StreamExceptions.tryIgnore(
//							(""+t+"\t"+roman(article.indexOf(t))+"\n").getBytes(),
//							out2::write)
//							);
//				} catch (Exception e) {throw new RuntimeException(e);}
//
//				List<Integer> author = topCommunitiesByArticles(tx, MAX);
//				try {
//					OutputStream out2 = Files.newOutputStream(outDir.resolve("getAuthorCommunityLabels.tsv"));
//					out2.write("authorCommunity\tlabel\n".getBytes());
//					author.stream().forEach(t -> 
//					StreamExceptions.tryIgnore(
//							(""+t+"\t"+letter(author.indexOf(t))+"\n").getBytes(),
//							out2::write)
//							);
//				} catch (Exception e) {throw new RuntimeException(e);}
//
//				return true;
//			});
//		}
//	}
//
//	static String TOPIC_MODEL = "topicModel.ser";
//

// TODO: integrate following code into graph build.
// Write topic model into graph....

//	//
//	private TopicModelResult getTopicModel() {
//		// Kryo kryo = new Kryo();
//	    // kryo.register(TopicModelResult.class);
//		Path topicPath = cacheDir.resolve(TOPIC_MODEL);
//		try {
//			if (Files.exists(topicPath)) {
//				//Input oid = new Input(Files.newInputStream(topicPath));
//				//return kryo.readObject(oid,TopicModelResult.class);
//				ObjectInputStream oid = new ObjectInputStream(Files.newInputStream(topicPath));
//				return (TopicModelResult) oid.readObject();
//			}
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		TopicModelResult result = null;
//		try {
//			Session session = driver.session();
//			Transaction tx = session.beginTransaction();
//
//			String qry = queries.get("getAuthorCommunityTitlesAbstracts");
//			List<Record> res = tx.run( qry ).list();
//			Corpus texts = textCorpus();
//
//			for( Record r : res) {
//				Integer next = r.get("community").asInt();
//				Integer artComm = r.get("articleCommunity").asInt();
//				String nodeId = r.get("nodeId").asNumber().toString();
//				String title = r.get("title").asString();
//				String abstrct = r.get("abstract").asString();
//				Boolean isOriginalSearch = r.get("isOriginalSearch", false);
//				Document doc = texts.addDocument(nodeId, title+(abstrct != null ? "\n"+abstrct : ""));
//				doc.addMetadata("community",next);
//				doc.addMetadata("articleCommunity",artComm);
//				doc.addMetadata("isOriginalSearch",isOriginalSearch);
//				Optional<String> doi = Optional.ofNullable(r.get("doi").asString());
//				Optional<String> pmid = Optional.ofNullable(r.get("pmid").asString());
//				doi.ifPresent(d -> doc.addMetadata("doi",d));
//				pmid.ifPresent(p -> doc.addMetadata("pmid",p));
//
//			}
//
//			result = TopicModelBuilder.create(texts).withTopics(10).execute(0.1,0.1);
//			tx.close();
//			session.close();
//
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		try {
//			//Output oos = new Output(Files.newOutputStream(topicPath));
//			//kryo.writeObject(oos,result);
//			ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(topicPath));
//			oos.writeObject(result);
//			oos.close();
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		result.getTopicsForDocuments().findFirst().get().streamDocuments()
//		return result;
//	}
//
//	private Corpus affiliationCorpus() {return 
//			Corpus.create()
//			.withStopwordFilter(affiliationStopwords)
//			.withTokenFilter(Filter.number())
//			.withTokenFilter(Filter.shorterThan(4));
//	}
//
//	private Corpus textCorpus() {return 
//			Corpus.create()
//			.withStopwordFilter(textStopwords)
//			.withTokenFilter(Filter.number())
//			.withTokenFilter(Filter.shorterThan(4));
//	}
//	
//	List<Integer> topCommunities = null;
//	
//	private String letter(int id) {
//		return "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(id % 26, id % 26+1);
//	}
//
//	private String roman(int id) {
//		return (new String[] {"I","II","III","IV","V","VI","VII","VIII","IX","X","XI"})[id];
//	}
//	
//	private List<Integer> topCommunitiesByArticles(Transaction tx, int size) {
//		if (topCommunities == null) {
//
//			String qry = queries.get("getAuthorCommunityStats");
//			List<Record> res = tx.run( qry ).list();
//			SortedSet<Counted<Integer>> tmp = Counted.descending();
//
//			res.stream().forEach(r -> {
//				tmp.add(Counted.create(
//						r.get("community").asNumber().intValue(),
//						r.get("articles").asNumber().intValue()));
//			});
//
//			topCommunities = tmp.stream().limit(size).map(cc -> cc.getTarget()).collect(Collectors.toList());
//
//		}
//		return topCommunities;
//	}
//	
//	List<Integer> topArticleCommunities = null;
//
//	private List<Integer> topNArticleCommunities(Transaction tx, int max) {
//		if (topArticleCommunities == null) {
//			String qry = queries.get("getArticleCommunityTitlesAbstracts");
//			List<Record> res = tx.run( qry ).list();
//
//			Map<Integer,Integer> communityCount = new HashMap<>();
//			for( Record r : res) {
//				Integer next = r.get("articleCommunity").asInt();
//				communityCount.merge(next, 1, (v1,v2)->v1+v2);
//			}
//
//			topArticleCommunities = communityCount.entrySet().stream()
//					.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
//					.limit(max).map((kv) -> kv.getKey()).collect(Collectors.toList());
//		}
//
//		return topArticleCommunities;
//	}
//	
//	public void plotCommunityAffiliations(int max) {
//		try ( Session session = driver.session() ) {
//
//			session.readTransaction( tx -> {
//
//				String qry = queries.get("getAuthorCommunityAffiliations");
//				List<Record> res = tx.run( qry ).list();
//				Corpus texts = affiliationCorpus();
//
//				for( Record r : res) {
//					Integer next = r.get("community").asInt();
//					// Integer size = r.get("size").asInt();
//					r.get("affiliations").asList(Values.ofString()).forEach(aff -> { 
//						Document doc = texts.addDocument(aff);
//						doc.addMetadata("community", next);
//					});
//				}
//
//				int id = 0;
//				for (Integer community: topCommunitiesByArticles(tx,max)) {
//					WordCloudBuilder.from(texts, 200, 600, 600).circular()
//					.withColourScheme(ColourScheme.sequential(id).darker(0.25F))
//					//.withSelector(c -> c.getTermsByMutualInformation(d -> community.equals(d.getMetadata("community").orElse(null)))
//					.withSelector(c -> c.getTermsByTfidf(d -> community.equals(d.getMetadata("community").orElse(null)))
//							.map(wt -> wt.scale(100000)))
//					.execute(outDir.resolve("CommunityAffiliations"+letter(id)+".png"));
//					id++;
//				}
//				//plotCommunityWordcloud(texts,community,"Affiliations");
//				return true;
//			});
//		}
//	}
//
//	public void plotAuthorCommunityContent(int max) {
//		try ( Session session = driver.session() ) {
//
//			session.readTransaction( tx -> {
//				String qry = queries.get("getAuthorCommunityTitlesAbstracts");
//				List<Record> res = tx.run( qry ).list();
//				Corpus texts = textCorpus();
//
//				for( Record r : res) {
//					Integer next = r.get("community").asInt();
//					//Integer size = r.get("size").asInt();
//					String nodeId = r.get("nodeId").asNumber().toString();
//					String title = r.get("title").asString();
//					String abstrct = r.get("abstract").asString();
//					Document doc = texts.addDocument(nodeId, title+(abstrct != null ? "\n"+abstrct : ""));
//					doc.addMetadata("community",next);
//				}
//
//				int id=0;
//				for (Integer community: topCommunitiesByArticles(tx,max)) {
//					WordCloudBuilder.from(texts, 200, 600, 600).circular()
//					.withColourScheme(ColourScheme.sequential(id).darker(0.25F))
//					.withSelector(c -> c.getTermsByMutualInformation(d -> community.equals(d.getMetadata("community").orElse(null)))
//							.map(wt -> wt.scale(10000)))
//					.execute(outDir.resolve("CommunityContent"+letter(id)+".png"));
//					id++;
//				}
//
//				return true;
//			});
//		}
//	}
//	
//	public void plotArticleCommunityContent(int max) {
//	
//	// -Xss68m
//	// stack size needs to be big enough to handle serialisation
//	@Test
//	public void plotTopicContent() {
//		try ( Session session = driver.session() ) {
//			session.readTransaction( tx -> {
//
//				List<Integer> top10community = topCommunitiesByArticles(tx,MAX);
//				List<Integer> top10articleCommunity = topNArticleCommunities(tx, MAX);
//				TopicModelResult result = getTopicModel();
//				result.printTopics(10);
//				EavMap<Integer,Integer,Double> authorArticleCorrelation = new EavMap<>();
//				Corpus texts = result.getCorpus();
//
//				for( Document r : texts.getDocuments()) {
//					Integer next = r.getMetadata("community").map(o -> (int) o).orElse(-1);
//					Integer artComm = r.getMetadata("articleCommunity").map(o -> (int) o).orElse(-1);
//					if (top10community.contains(next) && top10articleCommunity.contains(artComm)) {
//						Double score = authorArticleCorrelation.get(next, artComm);
//						if (score == null) { score = 0D; }
//						score += 1.0D;
//						authorArticleCorrelation.put(next, artComm, score);
//					}
//
//				}
//
//				EavMap<Integer,Integer,Double> articleCommunityCorrelation = new EavMap<>();
//				EavMap<Integer,Integer,Double> topicCommunityCorrelation = new EavMap<>();
//				try {
//
//					OutputStream out = Files.newOutputStream(outDir.resolve("getTopicDocuments.tsv"));
//					out.write("topic\tnodeId\tweight\n".getBytes());
//
//					result.getTopicsForDocuments().forEach(top -> {
//
//						int id = top.getTopicId();
//
//						WordCloudBuilder.from(texts, 200, 600, 600).circular()
//						.withColourScheme(ColourScheme.sequential2(id).darker(0.25F))
//						.withSelector(c -> 
//						c.streamTopics()
//						.filter(t -> t.getTopicId() == id)
//						.flatMap(t -> t.streamTerms())
//						.map(wt -> wt.scale(10000)))
//						.execute(outDir.resolve("TopicContent"+id+".png"));
//
//						top.streamDocuments().forEach(wd -> {
//
//							StreamExceptions.tryLogWarn(
//									(id+"\t"+wd.getTarget().getIdentifier()+"\t"+wd.getWeight()+"\n").getBytes(),
//									out::write);
//
//							Optional<Integer> commId = wd.getTarget().getMetadata("community").map(o -> (int) o);
//							commId.ifPresent( cid -> {
//								if (top10community.contains(cid)) {
//									Double score = topicCommunityCorrelation.get(id, cid);
//									if (score == null) { score = 0D; }
//									score += wd.getWeight();
//									topicCommunityCorrelation.put(id, cid, score);
//								}
//							});
//							Optional<Integer> artCommId = wd.getTarget().getMetadata("articleCommunity").map(o -> (int) o);
//							artCommId.ifPresent( cid -> {
//								if (top10articleCommunity.contains(cid)) {
//									Double score = articleCommunityCorrelation.get(id, cid);
//									if (score == null) { score = 0D; }
//									score += wd.getWeight();
//									articleCommunityCorrelation.put(id, cid, score);
//								}
//							});
//						});
//					});
//
//					doChordDiagram(topicCommunityCorrelation,
//							i -> "Topic "+i,
//							j -> "Community "+letter(top10community.indexOf(j)),
//							"Topic author community relationships");
//
//					doChordDiagram(articleCommunityCorrelation,
//							i -> "Topic "+i,
//							j -> "Article group "+roman(top10articleCommunity.indexOf(j)),
//							"Topic article group relationships");
//
//					doChordDiagram(authorArticleCorrelation,
//							i -> "Community "+letter(top10community.indexOf(i)),
//							j -> "Article group "+roman(top10articleCommunity.indexOf(j)),
//							"Author community article group relationships");
//
//					out.close();
//
//					OutputStream out2 = Files.newOutputStream(outDir.resolve("getTopicCommunity.tsv"));
//					out2.write("topic\tcommunity\ttotalScore\n".getBytes());
//					topicCommunityCorrelation.stream().forEach(t -> 
//					StreamExceptions.tryIgnore(
//							(""+t.getFirst()+"\t"+t.getSecond()+"\t"+t.getThird()+"\n").getBytes(),
//							out2::write
//							));
//
//					OutputStream out3 = Files.newOutputStream(outDir.resolve("getTopicArticleCommunity.tsv"));
//					out3.write("topic\tarticleCommunity\ttotalScore\n".getBytes());
//					articleCommunityCorrelation.stream().forEach(t -> 
//					StreamExceptions.tryIgnore(
//							(""+t.getFirst()+"\t"+t.getSecond()+"\t"+t.getThird()+"\n").getBytes(),
//							out3::write
//							));
//
//					OutputStream out4 = Files.newOutputStream(outDir.resolve("getAuthorCommunityArticleGroup.tsv"));
//					out4.write("authorCommunity\tarticleCommunity\ttotalScore\n".getBytes());
//					authorArticleCorrelation.stream().forEach(t -> 
//					StreamExceptions.tryIgnore(
//							(""+t.getFirst()+"\t"+t.getSecond()+"\t"+t.getThird()+"\n").getBytes(),
//							out4::write
//							));
//
//					out2.close();
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
//				return true;
//			});
//
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	private void doChordDiagram(
//			EavMap<Integer,Integer,Double> correlation,
//			Function<Integer,String> nameSource,
//			Function<Integer,String> nameTarget,
//			String diagramName
//			) {
//		Stream<Triple<String,String,Double>> display = Streams.concat(
//				correlation.stream().map(t -> 
//				Triple.create(
//						nameSource.apply(t.getFirst()),
//						nameTarget.apply(t.getSecond()),
//						t.getThird())),
//				correlation.stream().map(t -> 
//				Triple.create(
//						nameTarget.apply(t.getSecond()),
//						nameSource.apply(t.getFirst()),
//						t.getThird()))
//				);
//
//		try {
//			fig.withNewChart(diagramName, ChartType.CHORD)
//			.withSeries(display)
//			.bind(ID, t -> t.getFirst(), "source")
//			.bind(STRENGTH, t -> t.getThird())
//			.bind(ID, t -> t.getSecond(), "target")
//			.withColourScheme(ColourScheme.Set2)
//			.done()
//			.render();
//		} catch (Exception e) {throw new RuntimeException(e);}
//	}
//
//	@Test
//	public void testModels() throws IOException, BibliographicApiException {
//		
//		String broaderSearch = prop.getProperty("broader-search");
//		BibliographicApis biblioApi = BibliographicApis.create(secretsFile);
//		TopicModelResult topic = getTopicModel();
//		//TODO: Fully recreate the database limited to 2017 and before.
//		
//		OutputStream topicCsv = Files.newOutputStream(outDir.resolve("getTestSetTopics.tsv"));
//		topicCsv.write("doi\ttopic\tscore\n".getBytes());
//		
//		OutputStream refsCsv = Files.newOutputStream(outDir.resolve("getTestSetReferences.tsv"));
//		refsCsv.write("doi\treferenceDoi\n".getBytes());
//		
//		Optional<EntrezSearch> result = biblioApi.getEntrez().buildSearchQuery(broaderSearch)
//			.betweenDates(LocalDate.of(2017, 1, 1), LocalDate.of(2018, 12, 31))
//			.execute();
//		
//		CiteProcProvider cppNew = CiteProcProvider.create(REFERENCE_FORMAT, Format.text);
//		CiteProcProvider cppRefs = CiteProcProvider.create(REFERENCE_FORMAT, Format.text);
//		
//		result.ifPresent(r -> {
//			Optional<EntrezEntries> entries = r.getStoredResult(biblioApi.getEntrez());
//			
//			entries.ifPresent(es -> {
//				es.stream().filter(e -> e.getDoi().isPresent())
//					.forEach(e -> {
//						String doi = e.getDoi().get();
//						//String pmid = e.getPMID().get();
//						String abstrct = e.getAbstract().orElse("");
//						String title = e.getTitle().orElse("");
//						SortedSet<Weighted<Integer>> topicScores = topic.predict((title+"\n"+abstrct).trim());
//						topicScores.forEach(wt -> {
//							try {
//								topicCsv.write((doi+"\t"+wt.getTarget()+"\t"+wt.getWeight()+"\n").getBytes());
//							} catch (IOException e1) {
//								throw new RuntimeException(e1);
//							}
//						});
//						biblioApi.getCrossref().getByDoi(doi).stream()
//								.map(sr -> sr.getWork())
//								.flatMap(w -> {
//									cppNew.add(w,doi);
//									return w.getReferences();
//								})
//								.flatMap(ref -> {
//									ref.getIdentifier().ifPresent(d -> cppRefs.add(ref,d));
//									return ref.getIdentifier().stream();
//								})
//								.forEach(refDoi -> {
//									try {
//										refsCsv.write((doi+"\t"+refDoi+"\n").getBytes());
//									} catch (IOException e1) {
//										throw new RuntimeException(e1);
//									}
//								});						
//						//TODO: Check whether references are in corpus (found / not found)
//						//TODO: Check whether topic scores agree???
//						//TODO: get author community for each reference (as a SortedSet<Counted<Integer>>)
//						//TODO: get article group for each reference (as a SortedSet<Counted<Integer>>)
//						//TODO: write into a csv
//					});
//			});
//			
//		});
//		
//		
//		cppNew.writeToFile(outDir.resolve("getTestSetDetails.tsv"));
//		cppRefs.writeToFile(outDir.resolve("getTestSetReferencesDetails.tsv"));
//		
//		topicCsv.close();
//		refsCsv.close();
//
//	}
////	
////	try ( Session session = driver.session() ) {
////
////		session.readTransaction( tx -> {
////			String qry = queries.get("getArticleCommunityTitlesAbstracts");
////			List<Record> res = tx.run( qry ).list();
////			Corpus texts = textCorpus();
////
////			for( Record r : res) {
////				Integer next = r.get("articleCommunity").asInt();
////				//Integer size = r.get("size").asInt();
////				String nodeId = r.get("nodeId").asNumber().toString();
////				String title = r.get("title").asString();
////				String abstrct = r.get("abstract").asString();
////				Document doc = texts.addDocument(nodeId, title+(abstrct != null ? "\n"+abstrct : ""));
////				doc.addMetadata("articleCommunity",next);
////			}
////
////			List<Integer> top10articleCommunity = topNArticleCommunities(tx, max);
////			for (Integer community: top10articleCommunity) {
////				int id = top10articleCommunity.indexOf(community);
////				WordCloudBuilder.from(texts, 200, 600, 600).circular()
////				.withColourScheme(ColourScheme.sequential3(id).darker(0.25F))
////				.withSelector(c -> c.getTermsByMutualInformation(d -> community.equals(d.getMetadata("articleCommunity").orElse(null)))
////						.map(wt -> wt.scale(10000)))
////				.execute(outDir.resolve("ArticleCommunityContent"+roman(id)+".png"));
////			}
////
////			return true;
////		});
////	}
//}
//}

//		@SuppressWarnings("unchecked")
//		Map<String,String> queries = (Map<String,String> ) obj.get("analyse");
//		
//		try ( Session session = driver.session() ) {
//	        
//			//Plot by age
//			session.readTransaction( tx -> {
//	        	
//	        	String qry = queries.get("getArticlesByAge");
//	        	List<Record> res = tx.run( qry ).list();
//	        	
//	        	try {
//	        	fig.withNewChart("Articles by age", ChartType.XYSCATTER)
//					.withSeries(res)
//						.bind(X, t -> t.get("qtr").asDouble()/4)
//						.bind(Y, t -> t.get("articles").asInt())
//					.done()
//					.config()
//						.withXLabel("years elapsed")
//						.withYLabel("articles")
//					.done().render();
//	        	} catch (Exception e) {throw new RuntimeException(e);}
//	        	
//	            return true;
//	        });
//	        
//	        session.readTransaction( tx -> {
//	        	
//	        	String qry = queries.get("getMeshCodeCooccurMutualInformation");
//	        	List<Record> res = tx.run( qry ).list();
//	        	//List<Triple<String,Double,String>> links = new ArrayList<>();
//	        	Set<String> nodes = new HashSet<>();
//	        	List<Triple<String,Double,String>> links = new ArrayList<>();
//	        	List<Triple<String,Integer,String>> links2 = new ArrayList<>();
//	        	
//	        	res.forEach(r -> {
//	        		String sourceTerm = r.get("sourceTerm").asString();
//	        		String targetTerm = r.get("targetTerm").asString();
//	        		Integer cooccurrenceCount = r.get("cooccurrences").asInt();
//	        		Double npmi = r.get("npmi").asDouble();
//	        		
//	        		if (nodes.size() < 50) {
//	        			nodes.add(sourceTerm);
//	        			nodes.add(targetTerm);
//	        		}
//	        		
//	        		if (nodes.contains(sourceTerm) && nodes.contains(targetTerm)) {
//	        			links.add(Triple.create(sourceTerm, npmi, targetTerm));
//	        			links2.add(Triple.create(sourceTerm, cooccurrenceCount, targetTerm));
//	        		}	
//		        	
//	        		
//	        			        		
//	        	});
//	        	
//	        	try {
//	        	fig.withNewChart("Mesh codes by npmi", ChartType.CHORD)
//					.withSeries(links)
//						.bind(ID, t -> t.getFirst(), "source")
//						.bind(STRENGTH, t -> t.getSecond())
//						.bind(ID, t -> t.getThird(), "target")
//						.withColourScheme(ColourScheme.Accent)
//					.done()
//					.render();
//	        	
//	        	fig.withNewChart("Mesh codes by cooccurrence", ChartType.CHORD)
//				.withSeries(links2)
//					.bind(ID, t -> t.getFirst(), "source")
//					.bind(STRENGTH, t -> t.getSecond())
//					.bind(ID, t -> t.getThird(), "target")
//					.withColourScheme(ColourScheme.Set1)
//				.done()
//				.render();
//	        	} catch (Exception e) {throw new RuntimeException(e);}
//	        	
//	            return true;
//	        });
//	        
//	        session.readTransaction( tx -> {
//	        	
//	        	String qry = queries.get("getKeywordCooccurMutualInformation");
//	        	List<Record> res = tx.run( qry ).list();
//	        	//List<Triple<String,Double,String>> links = new ArrayList<>();
//	        	Set<String> nodes = new HashSet<>();
//	        	List<Triple<String,Double,String>> links = new ArrayList<>();
//	        	List<Triple<String,Integer,String>> links2 = new ArrayList<>();
//	        	
//	        	res.forEach(r -> {
//	        		String sourceTerm = r.get("sourceTerm").asString();
//	        		String targetTerm = r.get("targetTerm").asString();
//	        		Integer cooccurrenceCount = r.get("cooccurrences").asInt();
//	        		Double npmi = r.get("npmi").asDouble();
//	        		
//	        		if (nodes.size() < 50) {
//	        			nodes.add(sourceTerm);
//	        			nodes.add(targetTerm);
//	        		}
//	        		
//	        		if (nodes.contains(sourceTerm) && nodes.contains(targetTerm)) {
//	        			links.add(Triple.create(sourceTerm, npmi, targetTerm));
//	        			links2.add(Triple.create(sourceTerm, cooccurrenceCount, targetTerm));
//	        		}	
//		        	
//	        		
//	        			        		
//	        	});
//	        	
//	        	try {
//	        	fig.withNewChart("Keywords by npmi", ChartType.NETWORK)
//	        		.withSeries(new ArrayList<>(nodes))
//	        			.bind(ID, t -> t)
//	        			.bind(LABEL, t -> t)
//	        		.done()
//	        		.withSeries(links)
//						.bind(ID, t -> t.getFirst(), "source")
//						.bind(STRENGTH, t -> t.getSecond())
//						.bind(ID, t -> t.getThird(), "target")
//						.withColourScheme(ColourScheme.Set2)
//					.done()
//					.render();
//	        	
//	        	fig.withNewChart("Keywords by cooccurrence", ChartType.CHORD)
//				.withSeries(links2)
//					.bind(ID, t -> t.getFirst(), "source")
//					.bind(STRENGTH, t -> t.getSecond())
//					.bind(ID, t -> t.getThird(), "target")
//					.withColourScheme(ColourScheme.Set3)
//				.done()
//				.render();
//	        	} catch (Exception e) {throw new RuntimeException(e);}
//	        	
//	            return true;
//	        });
//	        
//	       
//	        
//	        
//	        session.readTransaction( tx -> {
//	        	
//	        	String qry = queries.get("getAuthorCommunityAffiliations");
//	        	List<Record> res = tx.run( qry ).list();
//	        	List<String> texts = new ArrayList<>();
//	        	Integer community = null;
//	        	for( Record r : res) {
//	        		Integer next = r.get("community").asInt();
//	        		if (community == null) community = next;
//	        		if (community != next) {
//	        			plot(fig, "Community affiliations", texts, community, affiliationStopwords);
//	        			texts = new ArrayList<>();
//	        		}
//	        		texts.addAll(r.get("affiliations").asList(Values.ofString()));
//	        		community = next;
//	        	}
//	        	plot(fig, "Community affiliations", texts, community, affiliationStopwords);
//	        	return true;
//	        });
//	        
//	        // 
//	        
//	        session.readTransaction( tx -> {
//	        	
//	        	String qry = queries.get("getAuthorCommunityKeywords");
//	        	List<Record> res = tx.run( qry ).list();
//	        	List<String> texts = new ArrayList<>();
//	        	Integer community = null;
//	        	for( Record r : res) {
//	        		Integer next = r.get("community").asInt();
//	        		if (community == null) community = next;
//	        		if (community != next) {
//	        			plot(fig, "Community keywords", texts, community, textStopwords);
//	        			texts = new ArrayList<>();
//	        		}
//	        		texts.addAll(r.get("terms").asList(Values.ofString()));
//	        		community = next;
//	        	}
//	        	plot(fig, "Community keywords", texts, community, textStopwords);
//	        	return true;
//	        });
//	        
//	        session.readTransaction( tx -> {
//	        	
//	        	String qry = queries.get("getAuthorCommunityTitlesAbstracts");
//	        	List<Record> res = tx.run( qry ).list();
//	        	List<String> texts = new ArrayList<>();
//	        	Integer community = null;
//	        	for( Record r : res) {
//	        		Integer next = r.get("community").asInt();
//	        		if (community == null) community = next;
//	        		if (community != next) {
//	        			plot(fig, "Community content", texts, community, textStopwords);
//	        			texts = new ArrayList<>();
//	        		}
//	        		texts.addAll(r.get("abstracts").asList(Values.ofString()));
//	        		texts.addAll(r.get("titles").asList(Values.ofString()));
//	        		community = next;
//	        	}
//	        	//TODO: use mallet to identify topics
//	        	
//	        	
//	        	
//	        	plot(fig, "Community content", texts, community, textStopwords);
//	        	return true;
//	        });
//	        
//	    } catch ( ServiceUnavailableException ex ) {
//	        log.error(ex.getMessage());
//	    }
//		
//		driver.close();
//		
//	}
//	
//	static List<Integer> communityIndex = new ArrayList<>();
//	
//	static void plot(Figure fig, String name, List<String> list, Integer community, List<String> stopwords) {
//		try {
//    	
//		Integer i = communityIndex.indexOf(community);
//		if (i == -1) {
//			i = communityIndex.size();
//			communityIndex.add(community);
//		}
//		
//		fig.withNewChart(name+" "+i, ChartType.WORDCLOUD)
//		.withSeries(list)
//			.bind(TEXT, t -> t)
//			.withColourScheme(ColourScheme.sequential(i))
//		.done()	
//		.withSeries(stopwords).bind(TEXT, t -> t).done()
//		.render();
//	} catch (Exception e) {throw new RuntimeException(e);}
//	};
