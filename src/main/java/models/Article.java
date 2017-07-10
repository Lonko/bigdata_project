package models;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Article {
	private String title;
	private String year;
	private String journal;
	private String articleAbstract;
	private URI articleUri;
	private List<Article> references;
	
	public Article(String title, String year, String journal) {
		this.title = title;
		this.year = year;
		this.journal = journal;
		this.references = new LinkedList<>();
	}
	
	public Article(String title, String year, String journal, String abs){
		this(title,year,journal);
		this.articleAbstract = abs;
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

	public List<Article> getReferences() {
		return references;
	}
	
	public void setReferences(List<Article> references) {
		this.references = references;
	}
	
	public void addReference(Article ref){
		this.references.add(ref);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(title, year, journal);
	}
	
	@Override
	public boolean equals(Object other) {
		Article o = (Article) other;
		return Objects.equals(title, o.title) &&
				Objects.equals(year, o.year) &&
				Objects.equals(journal, o.journal);
	}
	
}
