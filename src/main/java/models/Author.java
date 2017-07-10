package models;

import java.util.LinkedList;
import java.util.List;

public class Author implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	
	private String name;
	private List<String> interests;
	private List<Article> articles = new LinkedList<>();

	public Author(String name, String interests){
		this.name = name;
		for(String interest : interests.split(";"))
			this.interests.add(interest);
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
