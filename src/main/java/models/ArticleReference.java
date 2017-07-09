package models;

public class ArticleReference {
	private Article source;
	private Article destination;
	
	public ArticleReference(Article source, Article destination){
		this.source = source;
		this.destination = destination;
	}

	public Article getSource() {
		return source;
	}

	public void setSource(Article source) {
		this.source = source;
	}

	public Article getDestination() {
		return destination;
	}

	public void setDestination(Article destination) {
		this.destination = destination;
	}
	
}
