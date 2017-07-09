package models;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label="Paper")
public class Article{	
	@GraphId
	private Long id;
	@Property(name="title")
	private String title;
	@Property(name="year")
	private String year;
	@Property(name="venue")
	private String journal;
	@Property(name="abstract")
	private String articleAbstract;
	
	@Relationship(type="PAPER_REFERENCE", direction="OUTGOING")
	private Set<Article> references = new HashSet<>();
	
}
