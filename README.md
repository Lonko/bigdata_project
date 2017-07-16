# Big Data 2017 - Author and Paper Citation Knowledge Graphs on DBLP	

Input: grafo delle citazioni dal dataset DBLP: sia con nodi che rappresentano autori che con nodi che rappresentano articoli

Output: costruzione dei Knowledge Graphs relativi agli articoli e agli autori secondo l'ontologia di dominio di DBLP	

Uso: 
- Creare i grafi di citazioni tra articoli e tra autori in formato .graphml tramite il tool [dblp](https://github.com/fcibecchini/dblp)
- Importare i grafi generati su [Neo4J](https://neo4j.com/) tramite il tool [neo4j-graphml](https://github.com/jexp/neo4j-shell-tools)
- Creare l'ontologia aggiornata di SwetoDBLP tramite [swetoDBLP Parser](https://github.com/fcibecchini/swetodblp_parser)
- Importare l'ontologia in formato XML/RDF su [BlazeGraph](https://www.blazegraph.com/product/)
- Lanciare spark-submit specificando i seguenti parametri: 
  - BOLT url di Neo4j
  - Username di Neo4j
  - Password di Neo4j
  - SPARQL Endpoint di BlazeGraph
  - Numero di nodi da aggiornare per ogni batch
  - Indice del primo nodo (in Neo4J)
  - Indice dell'ultimo nodo (in Neo4J)
  ````
  spark-submit \
	--class "main.Main" \
	--master yarn \
	--driver-memory 12g \
	--conf spark.neo4j.bolt.url=<url> \
	--conf spark.neo4j.bolt.user=<user> \
	--conf spark.neo4j.bolt.password=<password> \
	--packages neo4j-contrib:neo4j-spark-connector:2.0.0-M2,graphframes:graphframes:0.2.0-spark2.0-s_2.11,com.blazegraph:bigdata-core:2.0.0 \
	./bdproject-0.0.1-SNAPSHOT.jar \
	<blazegraph.url> kb <batch.size> <index.first> <index.last>
  ````

