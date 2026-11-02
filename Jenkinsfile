pipeline {
    agent any

    environment {
        AWS_REGION = 'ap-south-1'
        S3_BUCKET  = 'vaishnavi-java-jar-artifacts-2026'
        APP_NAME   = 'java-devops-project'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Verify JAR') {
            steps {
                sh 'ls -lh target/*.jar'
            }
        }

        stage('Upload to S3') {
            steps {
                script {
                    def jarFile = sh(
                        script: "ls target/${APP_NAME}-*.jar | head -n 1",
                        returnStdout: true
                    ).trim()

                    def version = sh(
                        script: "date +%Y%m%d-%H%M%S",
                        returnStdout: true
                    ).trim()

                    sh """
                        aws s3 cp '${jarFile}' \
                        's3://${S3_BUCKET}/${APP_NAME}/${APP_NAME}-${version}.jar' \
                        --region '${AWS_REGION}'
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Build successful. JAR uploaded to S3.'
        }

        failure {
            echo 'Build failed. Check the Console Output for the error.'
        }
    }
}