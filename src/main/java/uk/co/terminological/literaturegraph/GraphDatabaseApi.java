package uk.co.terminological.literaturegraph;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.configuration.BoltConnector;
import org.neo4j.kernel.configuration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphDatabaseApi {

	private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseApi.class);
	
	private static GraphDatabaseApi singleton;
	private GraphDatabaseService graphDb;

	public static void main(String[] args) {
		Path f = Paths.get(args[0]);
		Path g = Paths.get(args[1]);
		GraphDatabaseApi.create(f,g).waitAndShutdown();
	}

	public GraphDatabaseApi(Path graphDbPath, Path graphConfPath) {

		//http://neo4j-contrib.github.io/neo4j-jdbc/
		logger.info("Opening graphdb in: "+graphDbPath);
		
		if (graphConfPath != null) {
			
			graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder( graphDbPath.toFile() )
				.loadPropertiesFromFile( graphConfPath.toString() )
				.newGraphDatabase();
		} else {
			Config config = Config.builder()
				.withServerDefaults()
				.build();
			BoltConnector bolt = config.boltConnectors().get(0);
			graphDb = new GraphDatabaseFactory()
					.newEmbeddedDatabaseBuilder( graphDbPath.toFile() )
					.setConfig( bolt.type, "BOLT" )
					.setConfig( bolt.enabled, "true" )
					.setConfig( bolt.listen_address, "localhost:7687" )
					.newGraphDatabase();
					
		}
		
		
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				waitAndShutdown();
			}
		} );
	}

	public void shutdown() {
		if (graphDb != null && graphDb.isAvailable(1)) {
			graphDb.shutdown();
			graphDb = null;
		}
		logger.info("graphDb is shutdown");
	}

	public void waitAndShutdown() {
		if (graphDb != null && graphDb.isAvailable(1)) {
			System.out.println("Press Enter key to shutdown neo4j...");
			try
			{
				System.in.read();
			}  
			catch(Exception e)
			{}  
			graphDb.shutdown();
			logger.info("graphDb is shutdown");
		}
	}

	public GraphDatabaseService get() {return graphDb;}

	public static GraphDatabaseApi create(Path graphDbPath, Path graphConfPath) {
		if (singleton == null) singleton = new GraphDatabaseApi(graphDbPath, graphConfPath);
		return singleton;
	}
	
	public static GraphDatabaseApi create(Path graphDbPath) {
		if (singleton == null) singleton = new GraphDatabaseApi(graphDbPath, null);
		return singleton;
	}

	public static GraphDatabaseService db() {
		if (singleton == null) throw new NotInitialisedException();
		return singleton.get();
	}

	public static class NotInitialisedException extends RuntimeException {}
}
