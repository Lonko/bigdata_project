package models;

import java.util.LinkedList;
import java.util.List;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@NodeEntity
public class Author{
	@GraphId
	private Long id;
	@Property(name="name")
	private String name;
	@Convert(InterestsConverter.class)
	private List<String> interests;
	@Transient
	private List<Article> articles;

	public void addArticle(Article article){
		this.articles.add(article);
	}
}
