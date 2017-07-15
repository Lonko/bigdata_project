package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class KGWriter {

	public static void main(String[] args) {
		write();
	}
	
	public static void write() {
		try (BufferedReader br = new BufferedReader(new FileReader("export.csv"))) {
			Writer out = new OutputStreamWriter(new FileOutputStream(
					new File("export.json"), true),
					StandardCharsets.UTF_8);
			
			StringBuilder buildTriples = new StringBuilder();
			StringBuilder buildLabels = new StringBuilder();

			String line;
			br.readLine();
			while ((line = br.readLine()) != null) {
				String[] rec = line.split(",");
				
				buildTriples.append("{"
							+"\"subject\" : \""+rec[0]+"\",\n"
							+"\"predicate\" : \""+rec[1]+"\",\n"
							+"\"object\" : \""+rec[2]+"\",\n"
							+"\"vw\" : 0\n"
							+ "},\n");
				
				if (rec[0].startsWith("<http") && 
						(!rec[1].equals("foaf:workplaceHomepage") && 
						 !rec[1].equals("foaf:homepage"))) {
					String sub = rec[0].substring(rec[0].lastIndexOf("/")+1, rec[0].length()-1)
							.replaceAll(".html", "");
					buildLabels.append("\""+rec[0]+"\" : \""+sub+"\",\n");
				}
				if (rec[1].startsWith("<http")) {
					String pre = rec[1].substring(rec[1].lastIndexOf("/")+1, rec[1].length()-1);
					buildLabels.append("\""+rec[1]+"\" : \""+pre+"\",\n");
				}
				if (rec[2].startsWith("<http") && 
						(!rec[1].equals("foaf:workplaceHomepage") && 
						 !rec[1].equals("foaf:homepage"))) {
					String obj = rec[2].substring(rec[2].lastIndexOf("/")+1, rec[2].length()-1)
							.replaceAll(".html", "");
					buildLabels.append("\""+rec[2]+"\" : \""+obj+"\",\n");
				}
			}
			String triples = buildTriples.toString().substring(0, buildTriples.length()-2);
			String labels = buildLabels.toString().substring(0, buildLabels.length()-2);

			String toWrite = "{\"Triples\": ["+triples+"], \"Labels\": {"+labels+"}}";
			
			out.write(toWrite);
			out.flush();
			out.close();
		} catch (IOException e) {
			System.err.println("Exception e = " + e);
		}
	}

}
