package uk.co.terminological.literaturegraph;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;

import uk.co.terminological.bibliography.record.Author;
import uk.co.terminological.bibliography.record.IdType;
import uk.co.terminological.bibliography.record.PrintRecord;
import uk.co.terminological.bibliography.record.RecordReference;
import uk.co.terminological.literaturegraph.PubMedGraphSchema.Prop;
import uk.co.terminological.literaturegraph.PubMedGraphSchema.Rel;

public class Shim {

	public static PrintRecord recordFacade(org.neo4j.driver.v1.types.Node node) {
		return new PrintRecord() {
			@Override
			public Optional<String> getIdentifier() {
				return Optional.ofNullable(node.get(Prop.DOI).asString(null));
			}

			@Override
			public IdType getIdentifierType() {
				return IdType.DOI;
			}

			@Override
			public List<RecordReference> getOtherIdentifiers() {
				return Collections.emptyList();
			}

			@Override
			public List<? extends Author> getAuthors() {
				return Collections.emptyList();
			}

			@Override
			public Stream<String> getLicenses() {
				return Stream.empty();
			}

			@Override
			public Optional<String> getAbstract() {
				return Optional.ofNullable(node.get(Prop.ABSTRACT).asString(null));
			}

			@Override
			public Optional<String> getTitle() {
				return Optional.ofNullable(node.get(Prop.TITLE).asString(null));
			}

			@Override
			public Optional<String> getJournal() {
				return Optional.ofNullable(node.get(Prop.JOURNAL).asString(null));
			}

			@Override
			public Optional<LocalDate> getDate() {
				return Optional.ofNullable(node.get(Prop.DATE).asLocalDate(null));
			}

			@Override
			public Optional<URI> getPdfUri() {
				return Optional.ofNullable(node.get(Prop.PDF_URL).asString(null)).map(o -> URI.create(o.toString()));
			}
			
			public Optional<String> getFirstAuthorLastName() {
				return Optional.ofNullable(node.get(Prop.FIRST_NAME).asString(null));
			}
			
			public Optional<String> getFirstAuthorFirstName() {
				return Optional.ofNullable(node.get(Prop.LAST_NAME).asString(null));
			}

			@Override
			public Optional<String> getFirstAuthorName() {
				return Optional.ofNullable(node.get(Prop.FULL_NAME).asString(null));
			}

			@Override
			public Optional<String> getVolume() {
				return Optional.ofNullable(node.get(Prop.VOLUME).asString(null));
			}

			@Override
			public Optional<String> getIssue() {
				return Optional.ofNullable(node.get(Prop.ISSUE).asString(null));
			}

			@Override
			public Optional<Long> getYear() {
				return node.get(Prop.YEAR).isNull() ? Optional.empty() : Optional.of(node.get(Prop.YEAR).asLong());
			}

			@Override
			public Optional<String> getPage() {
				return Optional.ofNullable(node.get(Prop.PAGE).asString(null));
			}
			
		};
		
	}
	
	
	public static PrintRecord recordFacade(Node node) {
		return new PrintRecord() {
			@Override
			public Optional<String> getIdentifier() {
				return Optional.ofNullable(node.getProperty(Prop.DOI,null).toString());
			}

			@Override
			public IdType getIdentifierType() {
				return IdType.DOI;
			}

			@Override
			public List<RecordReference> getOtherIdentifiers() {
				// TODO Could look up the others
				return Collections.emptyList();
			}

			@Override
			public List<? extends Author> getAuthors() {
				List<Author> authors = new ArrayList<>();
				node.getRelationships(Rel.HAS_AUTHOR, Direction.OUTGOING).forEach(r -> {
					Node author = r.getEndNode();
					authors.add(new Author() {

						@Override
						public Optional<String> getORCID() {
							return Optional.ofNullable(author.getProperty(Prop.ORCID,null).toString());
						}

						@Override
						public Optional<String> getFirstName() {
							return Optional.ofNullable(author.getProperty(Prop.FIRST_NAME,null).toString());
						}

						@Override
						public String getLastName() {
							return author.getProperty(Prop.LAST_NAME).toString();
						}

						@Override
						public Optional<String> getInitials() {
							return Optional.ofNullable(author.getProperty(Prop.INITIALS,null).toString());
						}

						@Override
						public Stream<String> getAffiliations() {
							return Stream.empty();
						}
						
					});
				});
				return authors;
			}

			@Override
			public Stream<String> getLicenses() {
				return Stream.empty();
			}

			@Override
			public Optional<String> getAbstract() {
				return Optional.ofNullable(node.getProperty(Prop.ABSTRACT,null).toString());
			}

			@Override
			public Optional<String> getTitle() {
				return Optional.ofNullable(node.getProperty(Prop.TITLE,null).toString());
			}

			@Override
			public Optional<String> getJournal() {
				return Optional.ofNullable(node.getProperty(Prop.JOURNAL,null).toString());
			}

			@Override
			public Optional<LocalDate> getDate() {
				return Optional.ofNullable(node.getProperty(Prop.DATE,null)).map(o -> (LocalDate) o);
			}

			@Override
			public Optional<URI> getPdfUri() {
				return Optional.ofNullable(node.getProperty(Prop.PDF_URL,null)).map(o -> URI.create(o.toString()));
			}

			@Override
			public Optional<String> getFirstAuthorName() {
				return Optional.ofNullable(node.getProperty(Prop.FULL_NAME,null).toString());
			}

			@Override
			public Optional<String> getVolume() {
				return Optional.ofNullable(node.getProperty(Prop.VOLUME,null).toString());
			}

			@Override
			public Optional<String> getIssue() {
				return Optional.ofNullable(node.getProperty(Prop.ISSUE,null).toString());
			}

			@Override
			public Optional<Long> getYear() {
				return Optional.ofNullable(node.getProperty(Prop.YEAR,null)).map(o -> (Long) o);
			}

			@Override
			public Optional<String> getPage() {
				return Optional.ofNullable(node.getProperty(Prop.PAGE,null).toString());
			}
			
		};
		
	}
}
