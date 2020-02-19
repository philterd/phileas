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
        PHILEAS_INDER_DIR = "${WORKSPACE}"
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
                          extensions: [],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'git@bitbucket.org:mountainfog/phileas.git']]
                        ])
            }
        }
        stage ('Build') {
            steps {
                sh "mvn -version"
                sh "./get-data.sh"
                sh "mvn -U clean license:aggregate-add-third-party license:aggregate-download-licenses install deploy -Pit"
                sh 'mvn sonar:sonar -Dsonar.host.url=https://build.mtnfog.com/sonarqube -Dsonar.login=a8938795a48d1da708a9a139030077c98308390e'
            }
            post {
                always {
                    jiraSendBuildInfo site: 'mtnfog.atlassian.net'
                }
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
