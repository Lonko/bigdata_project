package models;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Author implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	
	private String name;
	private List<String> interests;
	private List<String> articles = new LinkedList<>();

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

	public List<String> getArticles() {
		return articles;
	}

	public void addArticle(String title) {
		articles.add(title);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, articles);
	}
	
	@Override
	public boolean equals(Object other) {
		Author o = (Author) other;
		return Objects.equals(name, o.name) 
				&& Objects.equals(articles, o.articles);
	}
	
}
