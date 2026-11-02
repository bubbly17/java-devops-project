
pipeline {
    agent any

    tools {
        maven 'maven'
    }

    environment {
        PATH = "C:\\Program Files\\Amazon\\AWSCLIV2;${env.PATH}"

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
                bat 'mvn clean package'
            }
        }

        stage('Verify JAR') {
            steps {
                bat 'dir target\\*.jar'
            }
        }

        stage('Check AWS CLI') {
            steps {
                bat 'where aws'
                bat 'aws --version'
            }
        }

        stage('Upload to S3') {
            steps {
                script {
                    def version = new Date().format('yyyyMMdd-HHmmss')
                    def jarFile = "target\\${APP_NAME}-1.0.0.jar"

                    bat """
                        aws s3 cp "${jarFile}" s3://${S3_BUCKET}/${APP_NAME}/${APP_NAME}-${version}.jar --region ${AWS_REGION}
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
