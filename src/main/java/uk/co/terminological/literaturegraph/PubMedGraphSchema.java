package uk.co.terminological.literaturegraph;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubMedGraphSchema {
	
	public static Logger logger = LoggerFactory.getLogger(PubMedGraphSchema.class);
	
	public static class Labels {
		public static final Label ARTICLE = Label.label("Article");
		public static final Label DOI_STUB = Label.label("DOIStub");
		public static final Label PMID_STUB = Label.label("PMIDStub");
		public static final Label PMCENTRAL_STUB = Label.label("PMCentralStub");
		public static final Label ORIGINAL_SEARCH = Label.label("PubmedSearchResult");
		public static final Label EXPAND = Label.label("Expand");
		public static final Label AUTHOR = Label.label("Author");
		public static final Label AFFILIATION = Label.label("Affiliation");
		public static final Label MESH_CODE = Label.label("MeshCode");
		public static final Label TOKEN = Label.label("Token");
		public static final Label SEARCH_RESULT = Label.label("SearchResult");
		public static final Label PDF_AVAILABLE = Label.label("PdfAvailable");
		public static final Label KEYWORD = Label.label("Keyword");
	}
	
	public enum Rel implements RelationshipType {
	    HAS_AUTHOR,
	    HAS_MESH, 
	    HAS_REFERENCE, 
	    HAS_RELATED, 
	    HAS_TOKEN, 
	    TOKEN_PRECEEDED_BY,
	    HAS_KEYWORD,
	    HAS_AFFILIATION, 
	    SIMILAR_TO,
	    SAME_AS,
	    CO_AUTHOR
	}
	
	public static class Prop {
		public static final String DATE = "date";
		public static final String PDF_URL = "pdfUrl";
		public static final String TITLE = "title";
		public static final String CODE = "code";
		public static final String TERM = "term";
		public static final String CROSSREF = "crossref";
		public static final String INITIALS = "initials";
		public static final String LAST_NAME = "lastName";
		public static final String FIRST_NAME = "firstName";
		public static final String HAS_PDF = "pdf";
		public static final String RELATEDNESS = "relatedness";
		public static final String ENTREZ = "entrez";
		public static final String PMID = "pmid";
		public static final String DOI = "doi";
		public static final String PMCID = "pmcid";
		public static final String AUTHOR_LABEL = "authorLabel";
		public static final String MESH_CODE = "code";
		public static final String TOKEN_VALUE = "value";
		public static final String ABSTRACT = "abstract";
		public static final String ORGANISATION_NAME = "organisationName";
		public static final String ORCID = "orcid";
		public static final String SCORE = "score";
		public static final String JOURNAL = "journal";
		public static final String CITED_BY = "citedByCount";
		public static final String REFERENCE_COUNT = "referencesCount";
		public static final String VOLUME = "volume";
		public static final String ISSUE = "issue";
		public static final String YEAR = "year";
		public static final String PAGE = "page";
		public static final String FULL_NAME = "fullName";
		public static final String FIRST_AUTHOR = "isFirstAuthor";
	}
	
	public static void setupSchema(GraphDatabaseApi graph) {
		
		try ( Transaction tx = graph.get().beginTx() )
		{
		    Schema schema = graph.get().schema();
		    schema.indexFor( Labels.ARTICLE ).on( Prop.PMID ).create();
		    schema.indexFor( Labels.ARTICLE ).on( Prop.DOI ).create();
		    schema.indexFor( Labels.ARTICLE ).on( Prop.PMCID ).create();
		    schema.constraintFor( Labels.ARTICLE ).assertPropertyIsUnique( Prop.PMID );
		    schema.constraintFor( Labels.ARTICLE ).assertPropertyIsUnique( Prop.DOI );
		    schema.constraintFor( Labels.ARTICLE ).assertPropertyIsUnique( Prop.PMCID );
		    schema.indexFor( Labels.KEYWORD ).on( Prop.TERM ).create();
		    schema.constraintFor( Labels.KEYWORD ).assertPropertyIsUnique( Prop.TERM );
		    schema.indexFor( Labels.AUTHOR ).on( Prop.AUTHOR_LABEL ).create();
		    schema.indexFor( Labels.AUTHOR ).on( Prop.LAST_NAME ).create();
		    schema.indexFor( Labels.MESH_CODE ).on( Prop.MESH_CODE ).create();
		    schema.constraintFor( Labels.MESH_CODE ).assertPropertyIsUnique( Prop.MESH_CODE );
		    tx.success();
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
	}
	
}
