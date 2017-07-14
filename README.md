# Big Data 2017 - Author and Paper Citation Knowledge Graphs on DBLP	

Input: grafo delle citazioni dal dataset DBLP: sia con nodi che rappresentano autori che con nodi che rappresentano articoli

Output: costruzione dei Knowledge Graphs relativi agli articoli e agli autori secondo l'ontologia di dominio di DBLP	

Uso: 
- Creare i grafi di citazioni tra articoli e tra autori in formato .graphml tramite il tool [dblp](https://github.com/fcibecchini/dblp)
- Importare i grafi generati su Neo4J tramite il tool (neo4j-graphml)[https://github.com/jexp/neo4j-shell-tools]
- Creare l'ontologia aggiornata di SwetoDBLP tramite [swetoDBLP Parser](https://github.com/fcibecchini/swetodblp_parser)
- Importare l'ontologia in formato XML/RDF su [BlazeGraph](https://www.blazegraph.com/product/)
- Lanciare lo [script](./spark-job.sh) specificand i seguenti parametri: 
  - BOLT url di Neo4j
  - Username di Neo4j
  - Password di Neo4j
  - SPARQL Endpoint di BlazeGraph
  - Numero di nodi da aggiornare per ogni batch
  - Indice del primo nodo (in Neo4J)
  - Indice dell'ultimo nodo (in Neo4J)
