package models;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Author {
	
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
	
	public double distance(Author other) {
		Set<Article> articles = new HashSet<>(getArticles());
		Set<Article> otherArticles = new HashSet<>(other.getArticles());

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
}
