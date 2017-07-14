# Big Data 2017 - Author and Paper Citation Knowledge Graphs on DBLP	

Input: grafo delle citazioni dal dataset DBLP: sia con nodi che rappresentano autori che con nodi che rappresentano articoli

Output: costruzione dei Knowledge Graphs relativi agli articoli e agli autori secondo l'ontologia di dominio di DBLP	

Uso: 

- Lanciare lo [script](./spark-job.sh) specificand i seguenti parametri: 
  - BOLT url di Neo4j
  - Username di Neo4j
  - Password di Neo4j
  - SPARQL Endpoint di BlazeGraph
  - Numero di nodi da aggiornare per ogni batch
  - Indice del primo nodo (in Neo4J)
  - Indice dell'ultimo nodo (in Neo4J)
