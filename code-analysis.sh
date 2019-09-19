#!/bin/bash

SONARQUBE_TOKEN=`aws ssm get-parameter --region us-east-1 --name sonarqube_token| jq -r .Parameter.Value`
mvn test jacoco:report org.owasp:dependency-check-maven:check -Powasp
mvn sonar:sonar -Dsonar.host.url=https://build.mtnfog.com/sonarqube -Dsonar.login=$SONARQUBE_TOKEN

