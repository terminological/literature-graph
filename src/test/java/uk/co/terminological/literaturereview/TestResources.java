package uk.co.terminological.literaturereview;
import uk.co.terminological.datatypes.FluentList;
import uk.co.terminological.literaturereview.CitationGraph;

public class TestResources {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		FluentList.create(
				"neo4j.conf",
				"plugins/apoc-3.5.0.1-all.jar",
				"plugins/graph-algorithms-algo-3.5.0.1.jar"
			).forEach(s -> {
				System.out.println(CitationGraph.class.getResource("/"+s));
			});
		

	}

}
