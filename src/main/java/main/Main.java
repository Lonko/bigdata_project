package main;

import java.util.HashMap;
import rdf.RDFController;
import scala.Tuple2;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.Row;
import org.neo4j.spark.Neo4JavaSparkContext;

//import static org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK_SER;


public class Main implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String rdfService;
	private final String rdfNamespace;
	
	public Main(String service, String namespace) {
		rdfService = service;
		rdfNamespace = namespace;
	}

	public static void main(String[] args) {
		if (args.length<2) {
			System.out.println("USAGE:"
					+ "<blazegraph server URL>"
					+ "<blazegraph namespace>");
			return;
		}
		
		SparkConf conf = new SparkConf().setAppName("Authors-Articles KG");
		SparkContext sc = new SparkContext(conf);
		Neo4JavaSparkContext context = Neo4JavaSparkContext.neo4jContext(sc);
		
		Main main = new Main(args[0], args[1]);
		main.parseArticles(context);
		//main.parseAuthors(context);
	}
	
	private void parseArticles(Neo4JavaSparkContext context) {
		String query = "MATCH (p:Paper) "
				//+ "WHERE p.year IN [\"2000\",\"2001\",\"2002\",\"2003\",\"2004\",\"2005\"] "
				+ "WHERE p.title = \"RoadRunner: automatic data extraction from data-intensive web sites\" "
				+ "WITH p "
				+ "OPTIONAL MATCH (p)-[r]->(p2:Paper) "
				+ "RETURN p.title, p.year, p.venue, p.abstract, collect(p2.title+\"\t\") as references";
		
		int numberOfUpdates = 
		context
			.queryRow(query, new HashMap<String,Object>())
			.mapToPair(this::makeArticleStatements)
			.reduceByKey(String::concat)
			.map(this::performUpdate)
			.reduce(Integer::sum)
			.intValue();
		
		System.out.println(numberOfUpdates);
	}
	
	private Tuple2<Integer,String> makeArticleStatements(Row r) {
		String title = r.getString(0);
		if (!title.endsWith(".")) title += ".";
		String abs = r.getString(3);
		String[] refs = r.get(4).toString().replaceAll("\\[|\\]", "").split("\t,");
		
		StringBuilder build = new StringBuilder();
		if (abs!=null)
			build.append("insert {?article opus:abstract \""+abs+"\"}"
					+ "where {"
					+ "?article rdfs:label \""+title+"\" . "
					+ "?article opus:author ?seq . "
					+ "?seq ?x ?author . "
					+ "?author rdf:type foaf:Person};\n");
		
		for (String rr : refs) {
			String ref = rr.trim();
			if (!ref.endsWith(".")) ref += ".";
			build.append("insert {"
					+ "?article opus:cites ?ref . "
					+ "?ref <http://purl.org/ontology/bibo/citedBy> ?article"
					+ "} "
					+ "where {"
					+ "?article rdfs:label \""+title+"\" . "
					+ "?ref rdfs:label \""+ref+"\""
					+ "};\n"
					+ "insert {?auth1 foaf:knows ?auth2}"
					+ "where {"
					+ "?article rdfs:label \""+title+"\" . "
					+ "?article opus:author ?seq1 . "
					+ "?seq1 ?x1 ?auth1 . "
					+ "?auth1 rdf:type foaf:Person . "
					+ "{"
					+ "select ?auth2 "
					+ "where {"
					+ "?article2 rdfs:label \""+ref+"\" . "
					+ "?article2 opus:author ?seq2 . "
					+ "?seq2 ?x2 ?auth2 . "
					+ "?auth2 rdf:type foaf:Person . "
					+ "}}filter(?auth1 != ?auth2)};\n");
		}
		
		int insertStatements = 3*refs.length;
		if (abs!=null) insertStatements++;
		return new Tuple2<>(insertStatements, build.toString());
	}
	
	private int performUpdate(Tuple2<Integer,String> tuple) {
		RDFController controller = new RDFController(rdfService,rdfNamespace);
		controller.updateQuery(tuple._2);
		controller.closeConnection();
		return tuple._1;
	}
	
	private void parseAuthors(Neo4JavaSparkContext context) {
		String query = "MATCH (p:Paper)"
				+ "WHERE p.year IN [\"2000\",\"2001\",\"2002\",\"2003\",\"2004\",\"2005\"] "
				+ "WITH p.title as t, SPLIT(p.author_ids, \",\") as ids "
				+ "MATCH (a:Author) "
				+ "WHERE a.name in ids "
				+ "RETURN a.author_name, a.interests, collect(t+\"\t\") as titles";
		
		int numberOfUpdates =
		context
		.queryRow(query, new HashMap<String,Object>())
		.mapToPair(this::makeAuthorStatements)
		.reduceByKey(String::concat)
		.map(this::performUpdate)
		.reduce(Integer::sum)
		.intValue();
	
		System.out.println(numberOfUpdates);
	}
	
	private Tuple2<Integer,String> makeAuthorStatements(Row r) {
		String name = r.getString(0);
		String interests = r.getString(1);
		String[] papers = r.get(2).toString().replaceAll("\\[|\\]", "").split("\t,");
		StringBuilder build = new StringBuilder();
		int statementsCount=0;
		if (papers.length>0 && interests!=null) {
			
			build.append("insert { ?author foaf:interest ?topic} where { "
					+ "?author foaf:name \""+name+"\" . "
					+ "?article opus:author ?s . ?s ?x ?author . ");
			
			for (int i=0; i<papers.length; i++) {
				String p = papers[i].trim();
				if (!p.endsWith(".")) p += ".";
				build.append("{?article rdfs:label \""+p+"\"}");
				if (i<papers.length-1) build.append(" union ");
			}
			
			String[] interestsArray = interests.split(";");
			build.append("{ select ?topic where {");
			for (int i=0; i<interestsArray.length; i++) {
				String in = interestsArray[i];
				build.append("{?topic rdfs:label \""+in+"\"}");
				if (i<interestsArray.length-1) build.append(" union ");
			}
			build.append("}}};\n");
			
			statementsCount = interestsArray.length; 
		}
		return new Tuple2<>(statementsCount, build.toString());
	}

}
