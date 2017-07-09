package connectors;

import java.util.LinkedList;
import java.util.List;

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
	
    public List<Article> getAllArticles(){
    	List<Article> articles = new LinkedList<>();

    	Session session = this.driver.session();

    	StatementResult result = session.run("MATCH (n:Paper) "
    			+ "RETURN n.title AS title, n.year AS year, n.venue AS venue, n.abstract AS abstract "
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
    		System.out.println(title);
    		articles.add(article);
    	}

    	return articles;
    }
}
