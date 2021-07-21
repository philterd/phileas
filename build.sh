#!/bin/bash
export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain mtnfog --domain-owner 341239660749 --query authorizationToken --output text --profile codeartifact`
echo $CODEARTIFACT_AUTH_TOKEN
mvn clean install
