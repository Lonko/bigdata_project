package main;

import java.net.URI;
import java.util.HashMap;

import models.Article;
import models.Author;
import rdf.RDFController;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Row;
import org.neo4j.spark.Neo4JavaSparkContext;

import static org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK_SER;


public class Main implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	private final static String RDF_SERVICE = 
			"http://ec2-34-212-137-94.us-west-2.compute.amazonaws.com:9999/blazegraph";
	private final static String RDF_NAMESPACE = "kb";	

	public static void main(String[] args) {
		SparkConf conf = new SparkConf().setAppName("Authors-Articles KG");
		SparkContext sc = new SparkContext(conf);
		Neo4JavaSparkContext context = Neo4JavaSparkContext.neo4jContext(sc);
		
		Main main = new Main();
		main.parseArticles(context);
	}
	
	private void parseArticles(Neo4JavaSparkContext context) {
		
		String query = "MATCH (p:Paper) "
				+ "WHERE p.year IN [\"2000\",\"2001\",\"2002\",\"2003\",\"2004\",\"2005\"] "
				+ "WITH p "
				+ "LIMIT 20 " // TEST
				+ "OPTIONAL MATCH (p)-[r]->(p2:Paper) "
				+ "RETURN p.title, p.year, p.venue, p.abstract, collect(p2.title+\"\t\") as references";
		
		JavaRDD<Row> articleRows = context.queryRow(query, new HashMap<String,Object>());
		
		JavaRDD<Article> articles = 
				articleRows
				.map(r -> {
					Article a = new Article(
							r.getString(0), 
							r.getString(1),
							r.getString(2),
							r.getString(3));
					String[] refs = r.get(4).toString().replaceAll("\\[|\\]", "").split("\t,");
					for (String ref : refs) {
						a.addReference(ref.trim());
					}
					return a;
				});
				
		articles
		
		.foreach(a -> update(a, RDF_SERVICE, RDF_NAMESPACE));
	}
	
	private void parseAuthors(Neo4JavaSparkContext context) {
		String query = "MATCH (p:Paper)"
				+ "WHERE p.year IN [\"2000\",\"2001\",\"2002\",\"2003\",\"2004\",\"2005\"] "
				+ "WITH p.title as t, SPLIT(p.author_ids, \",\") as ids "
				+ "MATCH (a:Author) "
				+ "WHERE a.name in ids "
				+ "RETURN a.author_name, a.interests, collect(t+\"\t\") as titles";
		
		JavaRDD<Row> authorRows = context.queryRow(query, new HashMap<String,Object>());
		
		JavaRDD<Author> authors = 
				authorRows
				.map(r -> {
					Author a = new Author(r.getString(0), r.getString(1));
					String[] titles = r.get(2).toString().replaceAll("\\[|\\]", "").split("\t,");
					for (String t : titles) {
						a.addArticle(t);
					}
					return a;
				});
	}
	
	private void update(Article article, String service, String namespace) {
		RDFController controller = new RDFController(service,namespace);
		
		URI articleURI = controller.lookUpArticle(article);
		System.out.println(articleURI);
		
		controller.closeConnection();
				
//		ADD N opus:abstract article.getAbstract()
//		for each RDFNode A : N opus:author A
//			ADD A foaf:publications N
//		For each reference : article.getReferences(): 
//		  RDFNode N1 = LOOKUP reference.getDest().getName()
//		  if (N and N1 are not related)
//		  	ADD N opus:cites N1
//		  	ADD N1 ontology:citedBy N
//		  	for each RDFNode A : N opus:author A
//		  		for each RDFNode A1 : N1 opus:author A1
//		  			if A != A1 and (A and A1 are not related)
//		  				ADD A foaf:knows A1
//		  				ADD A1 isKnownBy?? A
	}

}
