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
					+ "<batch size>\n"
					+ "<total papers>");
			return;
		}
		
		KnowledgeGraphsCreator kg = new KnowledgeGraphsCreator(args[0], args[1]);
		
		SparkConf conf = new SparkConf().setAppName("DBLP Authors-Articles KG");
		SparkContext sc = new SparkContext(conf);
		Neo4JavaSparkContext context = Neo4JavaSparkContext.neo4jContext(sc);
		
		int batchSize = Integer.valueOf(args[2]);
		int totalPapers = Integer.valueOf(args[3]);
		
		int i=0;
		while (i<totalPapers) {
			int start = i+1;
			int end = i+batchSize;
			long timeMillis = System.currentTimeMillis();
			int papersStatements = kg.parseArticles(context, start, end);
			int authorsStatements = kg.parseAuthors(context, start, end);
			double elapsedTime = (double)(timeMillis - System.currentTimeMillis()) / 1000;
			i += batchSize;
			
			System.out.println("FROM "+start+" TO "+end+" \n"
							 + "Papers Statements: "+papersStatements+" \n"
							 + "Authors Statements: "+authorsStatements+" \n"
							 + "Elapsed time: "+elapsedTime+" \n");
		}
	}
	
}
