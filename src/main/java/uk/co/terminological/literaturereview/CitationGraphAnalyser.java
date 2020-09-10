package uk.co.terminological.literaturereview;



import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.inject.internal.util.Lists;

import uk.co.terminological.bibliography.CiteProcProvider;
import uk.co.terminological.bibliography.CiteProcProvider.Format;
import uk.co.terminological.bibliography.record.PrintRecord;
import uk.co.terminological.jsr223.RMethod;
import uk.co.terminological.literaturegraph.Shim;

public class CitationGraphAnalyser {

	static Logger log = LoggerFactory.getLogger(CitationGraphAnalyser.class);
	Driver driver;
	
	Map<String, Object> obj;
	List<String> affiliationStopwords;
	List<String> textStopwords;
	Map<String,String> queries;
	
	String referenceFormat;
	
	@SuppressWarnings("unchecked")
	public CitationGraphAnalyser(CitationGraph graph) throws IOException {
		
		
		//BasicConfigurator.configure();
		
		Config config = Config.builder()
	            .withMaxConnectionLifetime( 30, TimeUnit.MINUTES )
	            .withMaxConnectionPoolSize( 50 )
	            .withConnectionAcquisitionTimeout( 2, TimeUnit.MINUTES )
	            .build();
		
		driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "neo4j" ), config );
		
		Yaml yaml = new Yaml();
		InputStream inputStream = CitationGraphAnalyser.class.getClassLoader().getResourceAsStream("cypherQuery.yaml");
		obj = yaml.load(inputStream);
		
		this.referenceFormat = "ieee";
		
		affiliationStopwords = Arrays.asList(((Map<String,String>) obj.get("config")).get("stopwordsForAffiliation").split("\n"));
		textStopwords = Arrays.asList(((Map<String,String>) obj.get("config")).get("stopwordsForText").split("\n"));
		queries = (Map<String,String> ) obj.get("analyse");
	}
	
	@RMethod
	public void setReferenceFormat(String referenceFormat) {
		this.referenceFormat = referenceFormat;
	}
	
	/*
	 * Writes a named query defined in cypherQuery.yaml to a dataframe file. 
	 */
	@RMethod
	public List<Map<String,Object>> executeQuery(String qryName) {
		try ( Session session = driver.session() ) {
			String qry = queries.get(qryName);
			System.out.println("Executing query: "+qryName);
			List<Map<String,Object>> out2 = session.readTransaction( tx -> {

				StatementResult qryR = tx.run( qry );
				List<Record> res = qryR.list();

				CiteProcProvider prov = CiteProcProvider.create(this.referenceFormat, Format.text);
				List<Integer> ids = new ArrayList<>(); 
				int i = 0;
				for (Record r:res) {
					for(Value f:r.values()) {
						try {
							Node n = f.asNode();
							PrintRecord tmp = Shim.recordFacade(n);
							prov.add(tmp);
							ids.add(i);
						} catch (Exception e) {}
					}
					i++;
				}

							
				List<String> cits = new ArrayList<>();
				
				try {
				if (!prov.isEmpty()) {
					cits = Arrays.asList(
						prov.orderedCitations().getEntries());
					}
				
				} catch (Exception e) {
					e.printStackTrace(System.out);
				};

				List<Map<String,Object>> out = new ArrayList<Map<String,Object>>();
				
				
				int i2 = 0;
				for (Record r:res) {
					Map<String,Object> tmp = r.asMap();
					Integer tmp2 = ids.indexOf(i2);
					if (tmp2 != -1) {
						tmp.put("citation", cits.get(tmp2).trim());
					}
					i2++;
				}
				return out;
			});
			return out2;
		}	
	}		

	@RMethod
	public void shutdown() {
		driver.closeAsync();
	}
			
	@RMethod
	public List<String> getQueryNames() {
		List<String> out = Lists.newArrayList(queries.values());
		out.removeIf(s -> !s.contains("get"));
		return out;
	}
	

    
}
