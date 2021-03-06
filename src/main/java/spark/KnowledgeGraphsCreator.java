package spark;

import java.util.HashMap;

import org.apache.spark.sql.Row;
import org.neo4j.spark.Neo4JavaSparkContext;

import rdf.RDFController;
import scala.Tuple2;

//import static org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK_SER;

/**
 * Performs parsing of DBLP Papers and Authors from linked Neo4j Citation Graphs.<br>
 * Citation relations, papers abstracts and authors interests will be added to a given
 * BlazeGraph RDF storage source, enriching SwetoDBLP ontology data. 
 *
 */
public class KnowledgeGraphsCreator implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String rdfService;
	private final String rdfNamespace;
	
	public KnowledgeGraphsCreator(String service, String namespace) {
		rdfService = service;
		rdfNamespace = namespace;
	}
	
	/**
	 * Parse Articles from Neo4J citation graph
	 * @param context Neo4JavaSparkContext context
	 * @param start first Paper id
	 * @param end last Paper id
	 * @return number of INSERT updates performed
	 */
	public int parseArticles(Neo4JavaSparkContext context, int start, int end) { 
		String query = "MATCH (p:Paper) "
				+ "WHERE p.name >= "+start+" and p.name <= "+end+" "
				+ "WITH p "
				+ "OPTIONAL MATCH (p)-[r]->(p2:Paper) "
				+ "RETURN p.title, p.abstract, collect(p2.title+\"\t\") as references";
		
		int numberOfUpdates = 
		context
			.queryRow(query, new HashMap<String,Object>())
			.mapToPair(this::makeArticleStatements)
			.reduceByKey(String::concat)
			.map(this::updateAndGetKey)
			.reduce(Integer::sum)
			.intValue();
		
		return numberOfUpdates;
	}
	
	private Tuple2<Integer,String> makeArticleStatements(Row r) {
		String title = formatTitle(r.getString(0));
		if (title.isEmpty()) return new Tuple2<>(0,"");
		
		boolean validArticles=false;
		int inserts = 0;
		String abs = r.getString(1);
		String[] refs = r.get(2).toString().replaceAll("\\[|\\]", "").split("\t,");
		
		StringBuilder build = new StringBuilder();
		if (abs!=null) {
			String abss = abs.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase();
			build.append("insert {?a opus:abstract \""+abss+"\" } "
					+ "where {?a rdfs:label \""+title+"\"};\n");
			inserts++;
		}
		
		for (String rr : refs) {
			String ref = formatTitle(rr.trim());
			if (!ref.isEmpty()) {
				build.append("insert {?a opus:cites ?r . ?r bibo:citedBy ?a} "
					+ "where {?a rdfs:label \""+title+"\" . ?r rdfs:label \""+ref+"\"};\n"
					+ "insert {?a1 foaf:knows ?a2}"
					+ "where {"
					+ "?a rdfs:label \""+title+"\" . "
					+ "?a opus:author ?s1 . ?s1 ?x1 ?a1 . "
					+ "?a1 rdf:type foaf:Person . "
					+ "{"
					+ "select ?a2 "
					+ "where {"
					+ "?ar rdfs:label \""+ref+"\" . "
					+ "?ar opus:author ?s2 . ?s2 ?x2 ?a2 . "
					+ "?a2 rdf:type foaf:Person}} "
					+ "filter(?a1 != ?a2)};\n");
				inserts++;
				validArticles=true;
			}
		}

		return (validArticles) ? new Tuple2<>(inserts, build.toString()) : new Tuple2<>(0,"");
	}
	
	private int updateAndGetKey(Tuple2<Integer,String> tuple) {
		if (!tuple._2.isEmpty()) {
			RDFController controller = new RDFController(rdfService,rdfNamespace);
			controller.updateQuery(tuple._2);
			controller.closeConnection();
		}
		return tuple._1;
	}
	
	/**
	 * Parse Authors from Neo4J author citation graph<br>
	 * Authors are resolved starting from Papers in the specified ID range.
	 * @param context Neo4JavaSparkContext context
	 * @param start first Paper id
	 * @param end last Paper id
	 * @return number of INSERT updates performed
	 */
	public int parseAuthors(Neo4JavaSparkContext context, int start, int end) {
		String query = "MATCH (p:Paper) "
				+ "WHERE p.name >= "+start+" and p.name <= "+end+" "
				+ "WITH p.title as t, SPLIT(p.author_ids, \",\") as ids "
				+ "MATCH (a:Author) "
				+ "WHERE a.name in ids "
				+ "RETURN a.author_name, a.interests, collect(t+\"\t\") as titles";
		
		int numberOfUpdates =
		context
			.queryRow(query, new HashMap<String,Object>())
			.mapToPair(this::makeAuthorStatements)
			.reduceByKey(String::concat)
			.map(this::updateAndGetKey)
			.reduce(Integer::sum)
			.intValue();
	
		return numberOfUpdates;
	}
	
	private Tuple2<Integer,String> makeAuthorStatements(Row r) {
		String name = r.getString(0);
		String interests = r.getString(1);
		String[] papers = r.get(2).toString().replaceAll("\\[|\\]", "").split("\t,");
		StringBuilder build = new StringBuilder();
		int insertStatements=0;
		boolean validArticles=false;
		if (papers.length>0 && interests!=null) {
			
			build.append("insert { ?a foaf:interest ?t} where { "
					+ "?a foaf:name \""+name+"\" . "
					+ "?ar opus:author ?s . ?s ?x ?a . ");
			
			for (int i=0; i<papers.length; i++) {
				String p = formatTitle(papers[i].trim());
				if (!p.isEmpty()) {
					validArticles=true;
					build.append("{?ar rdfs:label \""+p+"\"}");
					if (i<papers.length-1) build.append(" union ");
				}
			}
			if (!validArticles)
				return new Tuple2<>(0,"");

			String[] interestsArray = interests.split(";");
			build.append("{ select ?t where {");
			for (int i=0; i<interestsArray.length; i++) {
				String in = interestsArray[i];
				build.append("{?t rdf:type foaf:Document . ?t rdfs:label \""+in+"\"}");
				if (i<interestsArray.length-1) build.append(" union ");
			}
			build.append("}}};\n");
			
			insertStatements = interestsArray.length; 
		}
				
		return new Tuple2<>(insertStatements, build.toString());
	}
	
	private String formatTitle(String title) {
		return title.replaceAll("[^a-zA-Z0-9\\(\\)\\,\\.\\:\\-\\+ ]", "").toLowerCase();
	}

}
