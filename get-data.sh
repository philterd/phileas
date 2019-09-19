#!/bin/bash

VERSION=$1
if [ -z "$VERSION" ]
then
  echo "Getting the project and version from the pom.xml."
  VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version -f ./pom.xml | egrep -v '^\[|Downloading:' | tr -d ' \n'`
fi

echo "Getting Phineas development data from S3 for version $VERSION."

mkdir -p data
aws s3 sync s3://mtnfog-development/phineas-data/$VERSION/ ./data/
