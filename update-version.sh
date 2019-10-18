#!/bin/bash

# Sets the project version and copies the data in S3.

V=$1

mvn versions:set -DnewVersion=$V -DgenerateBackupPoms=false
aws s3 sync ./data/ s3://mtnfog-development/phileas-data/$V/


