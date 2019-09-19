pipeline {
    agent any
    tools {
        maven 'maven-3.6.0'
        jdk 'jdk8u192'
    }
    triggers {
        pollSCM 'H/10 * * * *'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }
    parameters {
        booleanParam(defaultValue: true, description: 'Source Analysis', name: 'isAnalysis')
    }
    environment {
        //Use Pipeline Utility Steps plugin to read information from pom.xml into env variables
        IMAGE = readMavenPom().getArtifactId()
        VERSION = readMavenPom().getVersion()
        PHILTER_INDEX_DIR = "${WORKSPACE}"
    }
    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
            }
        }
        stage ('Build') {
            steps {
                sh "mvn -version"
                sh "./get-data.sh"
                sh "mvn license:aggregate-add-third-party license:aggregate-download-licenses install deploy"
            }
            post {
                always {
                    jiraSendBuildInfo site: 'mtnfog.atlassian.net'
                }
            }
        }
        stage ('Analysis') {
            when {
                expression {
                    if (env.ISANALYSIS == "true") {
                        return true
                    }
                    return false
                }
            }
            steps {
                sh "./code-analysis.sh"
            }
        }
    }
    post {
        success {
            sh "docker system prune -f"
            slackSend (color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
        failure {
            slackSend (color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
            sh "docker system prune -f"
            mail to: 'jeff.zemerick@mtnfog.com',
                 subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
                 body: "Something is wrong with ${env.BUILD_URL}"
        }
    }
}
