package models;

import java.util.LinkedList;
import java.util.List;

public class Author {
	
	private String name;
	private List<String> interests;
	private List<Article> articles;

	/*constructor for author without valid interests*/
	public Author(String name, List<Article> articles){
		this.name = name;
		this.articles = articles;
		this.interests = new LinkedList<>();
	}
	
	/*constructor for author with valid interests*/
	public Author(String name, List<Article> articles, List<String> interests){
		this.name = name;
		this.articles = articles;
		this.interests = interests;
	}	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getInterests() {
		return interests;
	}

	public void setInterests(List<String> interests) {
		this.interests = interests;
	}

	public List<Article> getArticles() {
		return articles;
	}

	public void setArticles(List<Article> articles) {
		this.articles = articles;
	}
	
}
