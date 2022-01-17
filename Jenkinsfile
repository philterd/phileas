pipeline {
    agent any
    tools {
        maven 'maven-3.6.3'
        jdk 'java-1.11.0-openjdk-amd64'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
        disableConcurrentBuilds()
    }
    triggers {
        pollSCM 'H/10 * * * *'
        parameterizedCron('''
            # Build AMI from master each morning at 1 AM.
            H 2 * * * %BRANCH_TAG=master
        ''')
    }
    parameters {
        gitParameter(defaultValue: 'origin/master', description: 'Branch/tag to build', name: 'BRANCH_TAG', type: 'PT_BRANCH_TAG')
    }
    environment {
        PHILEAS_INDEX_DIR = "${WORKSPACE}"
        PHILTER_REDIS_HOST = "localhost"
        PHILTER_REDIS_PORT = "6379"
        PHILTER_REDIS_SSL = "false"
        PHILTER_REDIS_AUTH_TOKEN = "Randompassword1!"
        PHILTER_REDIS_CLUSTERED = "false"
        CODEARTIFACT_AUTH_TOKEN = sh(returnStdout: true, script: 'aws codeartifact get-authorization-token --domain mtnfog --domain-owner 341239660749 --query authorizationToken --output text --region us-east-1')
    }
    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
                checkout([$class: 'GitSCM',
                          branches: [[name: "${params.BRANCH_TAG}"]],
                          doGenerateSubmoduleConfigurations: false,
                            extensions: [
                                [$class: 'GitLFSPull'],
                                [$class: 'CheckoutOption', timeout: 20],
                                [$class: 'CloneOption',
                                        depth: 0,
                                        noTags: false,
                                        reference: '/other/optional/local/reference/clone',
                                        shallow: false,
                                        timeout: 180]
                            ],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'git@bitbucket.org:mountainfog/phileas.git']]
                        ])
            }
        }
        stage ('Build') {
            steps {
                sh "mvn -version"
                sh "mvn -U clean license:aggregate-add-third-party license:aggregate-download-licenses install deploy -Pit"
            }
        }
        stage ('Analyze') {
            steps {
                sh "mvn dependency-check:aggregate -Powasp"
                sh "mvn sonar:sonar -Dsonar.host.url=${env.SONARQUBE_URL} -Dsonar.login=${env.SONARQUBE_TOKEN}"
            }
        }
    }
    post {
        success {
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
