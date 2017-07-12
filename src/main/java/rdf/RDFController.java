package rdf;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.bigdata.rdf.sail.webapp.client.RemoteRepository;

import models.Article;
import models.Author;

/**
 * Controller class to perform API calls to BlazeGraph RDF storage.
 *
 */
public class RDFController implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	private RemoteRepository repo;
	
	/**
	 * Creates a RDFController with an open connection to the RDF repository.
	 * @param service
	 * @param namespace
	 */
	public RDFController(String service, String namespace) {
		try {
			repo = BlazeGraphFactory.getRemoteRepository(service, namespace);
		} catch (Exception e) {
			repo = null;
		}
	}
	
	/**
	 * Returns the RDF URI of the given Article, or null if not present
	 * @return
	 */
	public URI lookUpArticle(Article article) {
		String title = article.getTitle();
		String query = "SELECT ?article WHERE { ?article rdfs:label \""+title+"\" }";
		
		try {
			TupleQueryResult result = repo.prepareTupleQuery(prefixes()+query).evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bs = result.next();
					return URI.create(bs.getValue("article").stringValue());
				}
				return null;
			} finally {
				result.close();
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	public Map<String,List<String>> getURICitationsMap(String query) {
		Map<String,List<String>> citationsMap = new HashMap<>();
		
		try {
			TupleQueryResult result = repo.prepareTupleQuery(prefixes()+query).evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bs = result.next();
					String article = "<"+bs.getValue("article").stringValue()+">";
					String citedArticle = "<"+bs.getValue("citArticle").stringValue()+">";
					List<String> citations = citationsMap.get(article);
					if (citations==null) {
						citations = new ArrayList<>();
						citationsMap.put(article, citations);
					}
					citations.add(citedArticle);
				}
				return citationsMap;
			} finally {
				result.close();
			}
		} catch (Exception e) {
			return citationsMap;
		}
	}
	
	public void updateQuery(String update) {
		try {
			repo.prepareUpdate(prefixes()+update).evaluate();
		} catch (Exception e) {
			System.err.println("EXCEPTION IN UPDATE: "+e.getMessage());
		}
	}
	
	
	/**
	 * Returns a URI representing the same Author object as the one given in input.
	 * @param author
	 * @return
	 */
	public String lookUpAuthor(Author author) {
		double threshold = 0.2;
		String name = author.getName();
		String queryCandidates = "SELECT ?author ?title "
				+ "WHERE { "
				+ "?author foaf:name \""+name+"\" ."
				+ "?article opus:author ?seq . "
				+ "?seq ?x ?author . "
				+ "?article rdfs:label ?title . "
				+ "} ";

		try {
			Map<String, List<String>> candidates = new HashMap<>();
			TupleQueryResult result = repo.prepareTupleQuery(prefixes()+queryCandidates).evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bs = result.next();
					String auth = bs.getValue("author").stringValue();
					String title = bs.getValue("title").stringValue();
					List<String> titles = candidates.get(auth);
					if (titles == null) {
						titles = new ArrayList<>();
						candidates.put(auth, titles);
					}
					titles.add(title);
				}
				
				for (String c : candidates.keySet()) {
					List<String> articles = candidates.get(c);
					if (articlesDistance(author.getArticles(), articles)<threshold)
						return "<"+c+">";
				}
				return "";
			} finally {
				result.close();
			}
		} catch (Exception e) {
			return "";
		}
	}
	
	/**
	 * Returns the List of Articles made by the given URI author
	 * @param author
	 * @return
	 */
	public List<String> getArticlesOfAuthor(URI author) {
		List<String> articles = new ArrayList<>();

		String query = "SELECT ?article ?title "
				+ "WHERE { "
				+ "?article opus:author ?seq . "
				+ "?seq ?x <"+author.toString()+"> . "
				+ "?article rdfs:label ?title . "
				+ "} ";
		
		try {
			TupleQueryResult result = repo.prepareTupleQuery(prefixes()+query).evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bs = result.next();
					String title = bs.getValue("title").stringValue();
					articles.add(title);
				}
				return articles;
			} finally {
				result.close();
			}
		} catch (Exception e) {
			return articles;
		}
	}
	
	/**
	 * Returns a List of URIs representing the same URI Author as the one given in input,
	 * or a List containing only the author in input if there are no owl:sameAs relations.
	 * @param author
	 * @return
	 */
	public List<URI> getSameAuthors(URI author) {
		String authorString = author.toString();
		List<URI> authors = Arrays.asList(author);
		
		String query = "SELECT ?author "
				+ "WHERE{"
				+ "{ "
				+ "<"+authorString+"> owl:sameAs ?sameAuthor . "
				+ "bind (?sameAuthor as ?author) "
				+ "} "
				+ "UNION "
				+ "{ "
				+ "?sameAuthor owl:sameAs <"+authorString+"> . "
				+ "bind (?sameAuthor as ?author)"
				+ "} "
				+ "UNION "
				+ "{ "
				+ "?sameAuthor owl:sameAs <"+authorString+"> ; "
				+ "owl:sameAs ?anotherAuthor . "
				+ "bind (?anotherAuthor as ?author) "
				+ "filter (?author != <"+authorString+">)";
		
		try {
			TupleQueryResult result = repo.prepareTupleQuery(prefixes()+query).evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bs = result.next();
					authors.add(URI.create(bs.getValue("author").stringValue()));
				}
				return authors;
			} finally {
				result.close();
			}
		} catch (Exception e) {
			return authors;
		}
    }
	
	/**
	 * Returns the symmetric set difference between two sets of Articles.<br>
	 * A distance of 0 means perfect matching. A distance of 1 means the lists are disjoint
	 * @param list1 first articles list
	 * @param list2 second articles list
	 * @return the symmetric difference as a value between 0 and 1
	 */
	public double articlesDistance(List<String> list1, List<String> list2) {
		Set<String> articles = new HashSet<>(list1);
		Set<String> otherArticles = new HashSet<>(list2);

		Set<String> union = new HashSet<>();
		Set<String> diff1 = new HashSet<>();
		Set<String> diff2 = new HashSet<>();
		Set<String> unionDiff = new HashSet<>();
		
		union.addAll(articles);
		union.addAll(otherArticles);
		
		diff1.addAll(articles);
		diff1.removeAll(otherArticles);
				
		diff2.addAll(otherArticles);
		diff2.removeAll(articles);
		
		unionDiff.addAll(diff1);
		unionDiff.addAll(diff2);
		
		return (double) unionDiff.size() / (double) union.size();
	}
	
	public void closeConnection() {
		try {
			repo.getRemoteRepositoryManager().close();
		} catch (Exception e) {
			System.err.println("Could not close connection");
		}
	}
	
	private String prefixes() {
		return "prefix foaf: <http://xmlns.com/foaf/0.1/>\n" + 
				"prefix dc: <http://purl.org/dc/elements/1.1/>\n" + 
				"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
				"prefix opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#>\n\n";
	}

}
