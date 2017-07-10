package connectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import models.Article;

public class Neo4jMain {
	public static void main(String[] args){
		BufferedReader br = null;      
		String host="", user="", pass="";
        try {
            br = new BufferedReader(new FileReader("credentials.txt"));
            String line = br.readLine();
            if(line!=null){
            	String[] credentials = line.split(";");
            	host=credentials[0];
            	user=credentials[1];
            	pass=credentials[2];
            }            	
        }catch(Exception e){
        	e.printStackTrace();
        }
		//Neo4jConnector n4j = new Neo4jConnector(host, user, pass);
		System.out.println("CONNESSIONE");
		//n4j.connect();
		System.out.println("QUERY");
		//Map<String, Article> arts = n4j.getAllArticles();
		//System.out.println(arts.size());
		//n4j.getAllReferences(arts);
//		arts.entrySet().stream().forEach(entry -> {
//			System.out.println(entry.getKey().toUpperCase());
//			entry.getValue().getReferences().forEach(r -> System.out.println(r));
//		});
		//n4j.close();
		System.out.println("CONNESSIONE CHIUSA");
	}
}
