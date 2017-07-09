package connectors;

import java.util.LinkedList;
import java.util.List;

import models.Article;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

public class Neo4jConnector {
	private static SessionFactory sessionFactory = new SessionFactory("models");
	private Session session;
	
	public Neo4jConnector(){
	}
	
    /*Actually only gets 25 articles for now */
    public List<Article> getAllArticles(){
    	List<Article> articlesList = new LinkedList<>();
    	this.session = sessionFactory.openSession();
    	
    	Iterable<Article> articles = session.loadAll(Article.class);
    	for(Article article : articles)
    		articlesList.add(article);
    	
    	return articlesList;
    }    

}
