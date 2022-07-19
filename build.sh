#!/bin/bash
export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain mtnfog --domain-owner 341239660749 --query authorizationToken --output text`
echo $CODEARTIFACT_AUTH_TOKEN
mvn clean license:aggregate-add-third-party license:aggregate-download-licenses install -Pit
