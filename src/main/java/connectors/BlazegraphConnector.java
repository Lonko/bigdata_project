package connectors;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.rio.RDFFormat;

import com.bigdata.rdf.sail.webapp.SD;
import com.bigdata.rdf.sail.webapp.client.ConnectOptions;
import com.bigdata.rdf.sail.webapp.client.JettyResponseListener;
import com.bigdata.rdf.sail.webapp.client.RemoteRepository;
import com.bigdata.rdf.sail.webapp.client.RemoteRepositoryManager;

public class BlazegraphConnector {

	protected static final Logger log = Logger
			.getLogger(BlazegraphConnector.class);
	private static final String serviceURL = //"http://localhost:9999/bigdata";
					"http://ec2-34-212-137-94.us-west-2.compute.amazonaws.com:9999/blazegraph";

	public static void main(String[] args) throws Exception {

		final RemoteRepositoryManager repo = new RemoteRepositoryManager(
				serviceURL, false /* useLBS */);

		try {

			JettyResponseListener response = getStatus(repo);
			System.out.println(response.getResponseBody());

			// create a new namespace if not exists
			final String namespace = "kb";
			final Properties properties = new Properties();
			properties.setProperty("com.bigdata.rdf.sail.namespace", namespace);
			if (!namespaceExists(repo, namespace)) {
//				log.info(String.format("Namespace %s doesn't exists", namespace));
			} else {
//				log.info(String.format("Namespace %s already exists", namespace));
				System.out.println("C'è kb");
			}
			
			
			//get properties for namespace
//			log.info(String.format("Property list for namespace %s", namespace));
			response = getNamespaceProperties(repo, namespace);
//			log.info(response.getResponseBody());
			System.out.println("Property list for namespace: " + namespace +"\n"+
							response.getResponseBody());

			/*
			 * Load data from file located in the resource folder
			 * src/main/resources/data.n3
			 */
//			final String resource = "/data.n3";
//			loadDataFromResource(repo, namespace, resource);

			// execute query
			final TupleQueryResult result = repo.getRepositoryForNamespace(namespace)
					//.prepareTupleQuery("SELECT * {?s ?p ?o} LIMIT 100")
					.prepareTupleQuery("prefix foaf: <http://xmlns.com/foaf/0.1/>\n" + 
							"prefix dc: <http://purl.org/dc/elements/1.1/>\n" + 
							"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
							"PREFIX opus: <http://lsdis.cs.uga.edu/projects/semdis/opus#>\n" + 
							"\n" + 
							" SELECT  ?article ?x ?author\n" + 
							" WHERE{\n" + 
							"	?article opus:author ?seq.\n" + 
							"	?seq ?x ?author.\n" + 
							"   filter (?x != rdf:type).\n" + 
							" }")
					.evaluate();
			
			//result processing
			try {
				while (result.hasNext()) {
					final BindingSet bs = result.next();
//					log.info(bs);
					System.out.println(bs/*.getValue("author")*/);
				}
			} finally {
				result.close();
			}

		} finally {
			repo.close();
			System.out.println("CHIUSO");
		}

	}

	/*
	 * Status request.
	 */
	private static JettyResponseListener getStatus(final RemoteRepositoryManager repo)
			throws Exception {

		final ConnectOptions opts = new ConnectOptions(serviceURL + "/status");
		opts.method = "GET";
		return repo.doConnect(opts);

	}

	/*
	 * Check namespace already exists.
	 */
	private static boolean namespaceExists(final RemoteRepositoryManager repo,
			final String namespace) throws Exception {
		
		final GraphQueryResult res = repo.getRepositoryDescriptions();
		try {
			while (res.hasNext()) {
				final Statement stmt = res.next();
				if (stmt.getPredicate()
						.toString()
						.equals(SD.KB_NAMESPACE.stringValue())) {
					if (namespace.equals(stmt.getObject().stringValue())) {
						return true;
					}
				}
			}
		} finally {
			res.close();
		}
		return false;
	}

	/*
	 * Get namespace properties.
	 */
	private static JettyResponseListener getNamespaceProperties(
			final RemoteRepositoryManager repo, final String namespace) throws Exception {

		final ConnectOptions opts = new ConnectOptions(serviceURL + "/namespace/"
				+ namespace + "/properties");
		opts.method = "GET";
		return repo.doConnect(opts);

	}

	/*
	 * Load data into a namespace.
	 */
	private static void loadDataFromResource(final RemoteRepositoryManager repo,
			final String namespace, final String resource) throws Exception {
		final InputStream is = BlazegraphConnector.class
				.getResourceAsStream(resource);
		if (is == null) {
			throw new IOException("Could not locate resource: " + resource);
		}
		try {
			repo.getRepositoryForNamespace(namespace).add(
					new RemoteRepository.AddOp(is, RDFFormat.N3));
		} finally {
			is.close();
		}
	}
}
