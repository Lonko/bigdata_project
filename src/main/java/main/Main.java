package main;

import spark.KnowledgeGraphsCreator;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.neo4j.spark.Neo4JavaSparkContext;

public class Main implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		if (args.length<4) {
			System.out.println("USAGE:\n"
					+ "<blazegraph server URL>\n"
					+ "<blazegraph namespace>\n"
					+ "<Start Year>\n"
					+ "<End Year>");
			return;
		}
		
		KnowledgeGraphsCreator kg = new KnowledgeGraphsCreator(args[0], args[1], args[2], args[3]);
		
		SparkConf conf = new SparkConf().setAppName("DBLP Authors-Articles KG");
		SparkContext sc = new SparkContext(conf);
		Neo4JavaSparkContext context = Neo4JavaSparkContext.neo4jContext(sc);
		
		kg.parseArticles(context);
		//main.parseAuthors(context);
	}
	
}
