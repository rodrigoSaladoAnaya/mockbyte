#!/usr/bin/env bash
./mvnw clean package && mv target/mockbyte-1.1-SNAPSHOT-jar-with-dependencies.jar target/mockbyte-fat.jar
