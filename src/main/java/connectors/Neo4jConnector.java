package connectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import models.Article;
import models.ArticleReference;
import models.Author;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

public class Neo4jConnector {
	private String host;
	private String user;
	private String password;
	private Driver driver;
	
	public Neo4jConnector(String host, String user, String password){
		this.host = host;
		this.user = user;
		this.password = password;
	}
	
	public void connect(){
		this.driver = GraphDatabase.driver(this.host, AuthTokens.basic(this.user, this.password));
	}
	
    public void close(){
        driver.close();
    }

    public ArrayList<Integer> getAllIds(Session session, String label){
    	ArrayList<Integer> ids = new ArrayList<>();
    	System.out.println("INIZIO QUERY");
    	StatementResult result = session.run("MATCH (n:"+label+") RETURN ID(n) AS id");
    	while (result.hasNext())
    	{
    		Record record = result.next();
    		int id = record.get("id").asInt();
    		ids.add(id);
    	}
    	return ids;
    }
    
    /*Actually only gets 25 articles for now */
    public Map<String, Article> getAllArticles(){
    	Map<String, Article> articles = new HashMap<>();
    	Session session = this.driver.session();

    	ArrayList<Integer> ids = this.getAllIds(session, "Paper");
    	int start = 0, end = 1000, stepSize = 1000;
    	while(end < 1000000){
    		String id4query = "[";
    		for(int i = start; i < end; i++)
    			id4query += String.valueOf(ids.get(i))+",";
    		id4query = id4query.substring(0, id4query.length()-1) + "]";

    		StatementResult result = session.run("MATCH (p:Paper) "
    				+ "WHERE ID(p) in " + id4query + " "
    				+ "RETURN p.title AS title, p.year AS year, p.venue AS venue, p.abstract AS abstract ");
    		while (result.hasNext())
    		{
    			Record record = result.next();
    			String title = record.get("title").asString();
    			String year = record.get("year").asString();
    			String journal = record.get("venue").asString();
    			String abs = record.get("abstract").asString();
    			if(abs == null)
    				abs = "";
    			Article article = new Article(title, year, journal, abs);
    			articles.put(title, article);
    		}
    		start = end;
    		end += stepSize;
    		System.out.println(end);
    	}
    	session.close();
    	return articles;
    }
    
    /*Actually only gets 25 articles for now */
    public Map<String, Author> getAllAuthors(){
    	Map<String, Author> authors = new HashMap<>();
    	Session session = this.driver.session();

    	ArrayList<Integer> ids = this.getAllIds(session, "Author");
    	int start = 0, end = 1000, stepSize = 1000;
    	while(end < 1000000){
    		String id4query = "[";
    		for(int i = start; i < end; i++)
    			id4query += String.valueOf(ids.get(i))+",";
    		id4query = id4query.substring(0, id4query.length()-1) + "]";

    		StatementResult result = session.run("MATCH (a:Author) "
    				+ "WHERE ID(a) in " + id4query + " "
    				+ "RETURN p.author_name AS name, p.interests AS interests");
    		while (result.hasNext())
    		{
    			Record record = result.next();
    			String name = record.get("author_name").asString();
    			String interests = record.get("interests").asString();
    			Author author = new Author(name, interests);
    			authors.put(name, author);
    		}
    		start = end;
    		end += stepSize;
    		System.out.println(end);
    	}
    	session.close();
    	return authors;
    }
    
    /*get references for a single node */
    private List<String> getReferences(String title, Session session){
    	List<String> refs= new LinkedList<>();

    	StatementResult result = session.run(
    			"MATCH (p1:Paper{title:\"" + title + "\"})-[r:PAPER_REFERENCE]->(p2:Paper) "
    			+ "RETURN p2.title AS title");
    	while (result.hasNext())
    	{
    		Record record = result.next();
    		String titleReference = record.get("title").asString();
    		refs.add(titleReference);
    	}
    	System.out.println("OUT");
    	return refs;
    }
    
    /*get references for all nodes*/
    public void getAllReferences(Map<String, Article> articles){
    	
    	articles.entrySet().parallelStream().forEach(e -> {
        	Session session = this.driver.session();
    		List<String> references = this.getReferences(e.getKey(), session);
    		for(String ref : references){
    			Article destination = articles.get(ref);
    			ArticleReference aRef = new ArticleReference(e.getValue(), destination);
    			e.getValue().addReference(aRef);
    		}
        	session.close();
        	System.out.println(System.currentTimeMillis());
    	}); 
    	
    }
   
}
