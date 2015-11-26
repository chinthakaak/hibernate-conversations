#!/usr/bin/env bash

mvn resources:resources
cp target/classes/liquibase.schema.properties target/classes/liquibase.properties
mvn liquibase:clearCheckSums liquibase:update