package models;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

public class Article {
	private String title;
	private String year;
	private String journal;
	private String articleAbstract;
	private URI articleUri;
	private List<ArticleReference> references;
	
	public Article(String title, String year, String journal, String abs){
		this.title = title;
		this.year = year;
		this.journal = journal;
		this.articleAbstract = abs;
		this.references = new LinkedList<>();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getJournal() {
		return journal;
	}

	public void setJournal(String journal) {
		this.journal = journal;
	}

	public String getArticleAbstract() {
		return articleAbstract;
	}

	public void setArticleAbstract(String articleAbstract) {
		this.articleAbstract = articleAbstract;
	}

	public URI getArticleUri() {
		return articleUri;
	}

	public void setArticleUri(URI articleUri) {
		this.articleUri = articleUri;
	}

	public List<ArticleReference> getReferences() {
		return references;
	}
	
	public void setReferences(List<ArticleReference> references) {
		this.references = references;
	}
	
	public void addReference(ArticleReference ref){
		this.references.add(ref);
	}	
	
}
