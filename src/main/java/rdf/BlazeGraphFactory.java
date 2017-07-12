package rdf;

import com.bigdata.rdf.sail.webapp.client.RemoteRepository;
import com.bigdata.rdf.sail.webapp.client.RemoteRepositoryManager;

public class BlazeGraphFactory {
		
	public static RemoteRepository getRemoteRepository(String service, String namespace) 
			throws Exception {
		final RemoteRepositoryManager repo = new RemoteRepositoryManager(service, false);
		/*if (!namespaceExists(repo, namespace)) {
			log.log(Level.SEVERE, String.format("Namespace %s doesn't exists", namespace));
			throw new RuntimeException();
		}*/
		return repo.getRepositoryForNamespace(namespace);
	}
	/*
	private static boolean namespaceExists(RemoteRepositoryManager repo, String namespace) 
		throws Exception {
		
		GraphQueryResult res = repo.getRepositoryDescriptions();
		try {
			while (res.hasNext()) {
				Statement stmt = res.next();
				if (stmt.getPredicate().toString().equals(SD.KB_NAMESPACE.stringValue()) &&
					namespace.equals(stmt.getObject().stringValue()))
					return true;
			}
		} finally {
			res.close();
		}
		return false;
	}
	 */
}
