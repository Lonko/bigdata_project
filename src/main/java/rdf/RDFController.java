package rdf;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.bigdata.rdf.sail.webapp.client.RemoteRepository;

import connectors.BlazeGraphFactory;
import models.Article;
import models.Author;

/**
 * Controller class to perform API calls to BlazeGraph RDF storage.
 * @author fabio
 *
 */
public class RDFController {
	private Logger log = Logger.getLogger(RDFController.class.getName());
	
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
		//TODO: JAR with normalized function
		String query = "SELECT ?article "
				+ "WHERE { "
				+ "?article rdf:type opus:Article . "
				+ "?article rdfs:label ?title . "
				+ "FILTER (normalized(?title) = "+title+") . "
				+ "}";
		
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
			log.log(Level.WARNING, "Article not found");
			return null;
		}
	}
	
	/**
	 * Returns a Collection of URIs representing the same Author object as the one given in input.
	 * @param author
	 * @return
	 */
	public Collection<URI> lookUpAuthor(Author author) {
		double threshold = 0.2;
		String name = author.getName();
		String queryCandidates = "SELECT ?author "
				+ "WHERE { "
				+ "?author rdf:type foaf:Person . "
				+ "?author foaf:name ?name ."
				+ "BIND(cfn:distance(+"+name+", str(?name) as ?dist) . "
				+ "FILTER(?dist < +"+threshold+") "
				+ "} "
				+ "ORDER BY ?dist";

		try {
			List<URI> candidates = new ArrayList<>();
			TupleQueryResult result = repo.prepareTupleQuery(prefixes()+queryCandidates).evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bs = result.next();
					candidates.add(URI.create(bs.getValue("author").stringValue()));
				}
				
				Set<URI> visited = new HashSet<>(); // globally visited authors

				if (!candidates.isEmpty()) {
					for (URI c : candidates) {
						if (!visited.contains(c)) {
							Set<URI> rdfAuthors = new HashSet<>(getSameAuthors(c));
							visited.addAll(rdfAuthors);
							for (URI a : rdfAuthors) {
								List<Article> articles = getArticlesOfAuthor(a);
								if (articlesDistance(author.getArticles(), articles)<threshold) {
									return rdfAuthors;
								}
							}
							return new HashSet<>();
						}
					}
				}
				return new HashSet<>();
			} finally {
				result.close();
			}
		} catch (Exception e) {
			return new HashSet<>();
		}
	}
	
	/**
	 * Returns the List of Articles made by the given URI author
	 * @param author
	 * @return
	 */
	public List<Article> getArticlesOfAuthor(URI author) {
		List<Article> articles = new ArrayList<>();

		String query = "SELECT ?article ?title ?year ?journal "
				+ "WHERE { "
				+ "?article opus:author ?seq . "
				+ "?seq ?x <"+author.toString()+"> . "
				+ "?article rdfs:label ?title . "
				+ "?article opus:year ?year . "
				+ "?article opus:journal_name ?journal"
				+ "} ";
		
		try {
			TupleQueryResult result = repo.prepareTupleQuery(prefixes()+query).evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bs = result.next();
					String title = bs.getValue("title").stringValue();
					String year = bs.getValue("year").stringValue();
					String journal = bs.getValue("journal").stringValue();
					articles.add(new Article(title,year,journal));
				}
				return articles;
			} finally {
				result.close();
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Articles not found");
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
	public double articlesDistance(List<Article> list1, List<Article> list2) {
		Set<Article> articles = new HashSet<>(list1);
		Set<Article> otherArticles = new HashSet<>(list2);

		Set<Article> union = new HashSet<>();
		Set<Article> diff1 = new HashSet<>();
		Set<Article> diff2 = new HashSet<>();
		Set<Article> unionDiff = new HashSet<>();
		
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
			// TODO
		}
	}
	
	private String prefixes() {
		return "prefix foaf: <http://xmlns.com/foaf/0.1/>\n" + 
				"prefix dc: <http://purl.org/dc/elements/1.1/>\n" + 
				"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
				"prefix opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#>\n\n";
	}

}
