package main;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import models.Article;
import models.Author;
import rdf.RDFController;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Row;
import org.neo4j.spark.Neo4JavaSparkContext;

//import static org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK_SER;


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
				+ "LIMIT 2000 " // TEST
				+ "OPTIONAL MATCH (p)-[r]->(p2:Paper) "
				+ "RETURN p.title, p.year, p.venue, p.abstract, collect(p2.title+\"\t\") as references";
		
		JavaRDD<Row> articleRows = context.queryRow(query, new HashMap<String,Object>());
		JavaRDD<Article> articles = articleRows.map(this::makeArticle);
		articles.foreachPartition(p -> updateArticlesPartition(p));
	}
	
	private Article makeArticle(Row r) {
		Article a = new Article(r.getString(0), r.getString(1),r.getString(2),r.getString(3));
		String[] refs = r.get(4).toString().replaceAll("\\[|\\]", "").split("\t,");
		for (String ref : refs) {
			a.addReference(ref.trim());
		}
		return a;
	}
	
	private void updateArticlesPartition(Iterator<Article> p) {
		RDFController controller = new RDFController(RDF_SERVICE,RDF_NAMESPACE);
		
		Map<String,String> title2abstract = new HashMap<>();
		
		/* SPARQL Select couple of cited articles and save them on a Map*/
		
		StringBuilder buildQ = new StringBuilder("SELECT ?article ?citArticle WHERE {");
		while (p.hasNext()) {
			Article a = p.next();
			title2abstract.put(a.getTitle(), a.getArticleAbstract());
			
			for (String title : a.getReferences()) {
				buildQ.append("{ ?article rdfs:label \"" + a.getTitle() + "\"" 
							 + "?citArticle rdfs:label \"" + title + "\"} \nUNION");
			}
		}
		String select = buildQ.toString().substring(0, buildQ.length() - 5)+"}";
		Map<String,List<String>> citationsMap = controller.getURICitationsMap(select);
		
		addArticlesCitations(controller, title2abstract, citationsMap);
		addAuthorsCitations(controller, citationsMap);
		
		controller.closeConnection();
	}
	
	private void addArticlesCitations(RDFController controller, Map<String,String> title2abstract, 
			Map<String,List<String>> citationsMap) {
		
		StringBuilder build = new StringBuilder("DELETE {");
		for (Map.Entry<String, List<String>> citationList : citationsMap.entrySet()) {
			build.append(citationList.getKey()+" opus:abstract \""+title2abstract.get(citationList.getKey())+"\" ");
			for (String citation : citationList.getValue())
				build.append(
					citationList.getKey() + " opus:cites " +citation+ " " 
					+citation+" <http://purl.org/ontology/bibo/citedBy> "+citationList.getKey()+" ");
		}
		build.append("} INSERT {");
		for (Map.Entry<String, List<String>> citationList : citationsMap.entrySet()) {
			build.append(citationList.getKey()+" opus:abstract \"" +title2abstract.get(citationList.getKey())+"\" ");
			for (String citation : citationList.getValue())
				build.append(
						citationList.getKey()+" opus:cites " +citation+"  " 
						+citation+" <http://purl.org/ontology/bibo/citedBy> "+citationList.getKey()+" ");
		}
		build.append("}");
		
		String update = build.toString();
	}
	
	private void addAuthorsCitations(RDFController controller, Map<String,List<String>> citationsMap) {
		StringBuilder build = new StringBuilder(
				"DELETE {" + "?auth1 foaf:publications ?article " + "?auth1 foaf:knows ?auth2} ");
		build.append("INSERT {" + "?auth1 foaf:publications ?article " + "?auth1 foaf:knows ?auth2} ");
		build.append("WHERE {");

		for (Map.Entry<String, List<String>> citationList : citationsMap.entrySet()) {
			for (String citation : citationList.getValue()) {
				build.append("{" + citationList.getKey() + " opus:cites " + citation + " " + citationList.getKey()
						+ " opus:author ?Seq1" + " " + citation + " opus:author ?Seq2" + " ?Seq1 ?x ?auth1"
						+ " ?Seq2 ?x ?auth2" + " BIND(" + citationList.getKey() + " AS ?article)} UNION");
			}
		}
		
		String update = build.toString().substring(0, build.length() - 5)+"}";
	}
	
	private void parseAuthors(Neo4JavaSparkContext context) {
		String query = "MATCH (p:Paper)"
				+ "WHERE p.year IN [\"2000\",\"2001\",\"2002\",\"2003\",\"2004\",\"2005\"] "
				+ "WITH p.title as t, SPLIT(p.author_ids, \",\") as ids "
				+ "MATCH (a:Author) "
				+ "WHERE a.name in ids "
				+ "RETURN a.author_name, a.interests, collect(t+\"\t\") as titles";
		
		JavaRDD<Row> authorRows = context.queryRow(query, new HashMap<String,Object>());
		JavaRDD<Author> authors = authorRows.map(this::makeAuthor);
		
		authors
		.foreachPartition(p -> {
			RDFController controller = new RDFController(RDF_SERVICE,RDF_NAMESPACE);
			p.forEachRemaining(a -> updateAuthor(a,controller));			
			controller.closeConnection();
		});
	}
	
	private Author makeAuthor(Row r) {
		Author a = new Author(r.getString(0), r.getString(1));
		String[] titles = r.get(2).toString().replaceAll("\\[|\\]", "").split("\t,");
		for (String t : titles) {
			a.addArticle(t);
		}
		return a;
	}
	
	private void updateAuthor(Author author, RDFController controller) {
		Collection<URI> sameAuthors = controller.lookUpAuthor(author);
		for (URI aut : sameAuthors) {
			for (String interest : author.getInterests()) {
				// ADD aut foaf:interest ?topic
				// WHERE ?topic rdf:type foaf:Document ;
				// 				rdfs:label interest
			}
		}
	}
	
	/*
	private void updateArticle(Article article, RDFController controller) {
		URI articleURI = controller.lookUpArticle(article);

		if (articleURI!=null) {
//			ADD N opus:abstract article.getAbstract()
//			for each RDFNode A : N opus:author A
//				ADD A foaf:publications N
//			For each reference : article.getReferences(): 
//			  RDFNode N1 = LOOKUP reference.getDest().getName()
//			  if (N and N1 are not related)
//			  	ADD N opus:cites N1
//			  	ADD N1 ontology:citedBy N
//			  	for each RDFNode A : N opus:author A
//			  		for each RDFNode A1 : N1 opus:author A1
//			  			if A != A1 and (A and A1 are not related)
//			  				ADD A foaf:knows A1
//			  				ADD A1 isKnownBy?? A
		}
	}
	*/
}
