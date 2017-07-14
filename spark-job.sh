#!/bin/bash

spark-submit \
	--class "main.Main" \
	--master yarn \
	--driver-memory 12g \
	--conf spark.neo4j.bolt.url=$1 \
	--conf spark.neo4j.bolt.user=$2 \
	--conf spark.neo4j.bolt.password=$3 \
	--packages neo4j-contrib:neo4j-spark-connector:2.0.0-M2,graphframes:graphframes:0.2.0-spark2.0-s_2.11,com.blazegraph:bigdata-core:2.0.0 \
	./bdproject-0.0.1-SNAPSHOT.jar \
	$4 kb $5 $6 $7