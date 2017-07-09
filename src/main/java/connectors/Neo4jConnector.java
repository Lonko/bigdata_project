package connectors;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import models.Article;

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
	
    /*Actually only gets 25 articles for now */
    public Map<String, Article> getAllArticles(){
    	Map<String, Article> articles = new HashMap<>();
    	Session session = this.driver.session();

    	StatementResult result = session.run("MATCH (p:Paper) "
    			+ "RETURN p.title AS title, p.year AS year, p.venue AS venue, p.abstract AS abstract "
    			+ "LIMIT 25");
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

    	session.close();
    	return articles;
    }
    
    /*get references for a single node */
    private List<String> getReferences(String title, Session session){
    	List<String> refs= new LinkedList<>();
    	System.out.println("TITOLO: "+title);

    	StatementResult result = session.run(
    			"MATCH (p1:Paper{name:\"" + title + "\"})-[r:PAPER_REFERENCE]->(p2:Paper) "
    			+ "RETURN p2.title AS title");
    	while (result.hasNext())
    	{
    		Record record = result.next();
    		String titleReference = record.get("title").asString();
    		System.out.println(titleReference);
    		refs.add(titleReference);
    	}
    	
    	return refs;
    }
    
    /*get references for all nodes*/
    public void getAllReferences(Map<String, Article> articles){
    	Session session = this.driver.session();
    	
    	articles.entrySet().stream().forEach(e -> {
    		List<String> references = this.getReferences(e.getKey(), session);
    		for(String ref : references){
    			Article destination = articles.get(ref);
    			e.getValue().addReference(destination);
    		}
    	}); 
    	
    	session.close();
    }
}
