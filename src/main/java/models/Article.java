package models;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Article implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	private String title;
	private String year;
	private String journal;
	private String articleAbstract;
	private List<String> references;
	
	public Article(String title, String year, String journal) {
		this.title = title;
		this.year = year;
		this.journal = journal;
		this.references = new LinkedList<>();
	}
	
	public Article(String title, String year) {
		this.title = title;
		this.year = year;
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

	public List<String> getReferences() {
		return references;
	}
	
	public void setReferences(List<String> references) {
		this.references = references;
	}
	
	public void addReference(String ref){
		this.references.add(ref);
	}
	
	@Override
	public String toString() {
		return getTitle()+", "+getYear()+", "+getJournal()+", "+getArticleAbstract();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(title,year);
	}
	
	@Override
	public boolean equals(Object other) {
		Article o = (Article) other;
		return Objects.equals(title, o.title) 
				&& Objects.equals(year, o.year);
	}
	
}
