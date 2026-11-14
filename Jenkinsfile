
pipeline {

    agent {
        label 'temporary-agent'
    }

    tools {
        maven 'maven'
    }

    environment {
        AWS_REGION = 'ap-south-1'
        S3_BUCKET  = 'vaishnavi-java-jar-artifacts-2026'
        APP_NAME   = 'java-devops-project'
        APP_SERVER = '172.31.3.96'
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

        stage('Check AWS CLI') {
            steps {
                sh 'which aws'
                sh 'aws --version'
            }
        }

        stage('Upload to S3') {
            steps {
                script {
                    def version = new Date().format('yyyyMMdd-HHmmss')
                    def jarFile = "target/${APP_NAME}-1.0.0.jar"

                    sh """
                        aws s3 cp "${jarFile}" \
                        s3://${S3_BUCKET}/${APP_NAME}/${APP_NAME}-${version}.jar \
                        --region ${AWS_REGION}
                    """
                }
            }
        }

        stage('Deploy Java Application') {
            steps {
                sshagent(credentials: ['app-ec2-ssh-key']) {
                    sh '''
                        scp -o StrictHostKeyChecking=no \
                        target/java-devops-project-1.0.0.jar \
                        ubuntu@172.31.3.96:/home/ubuntu/java-app/app.jar

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@172.31.3.96 \
                        "sudo -n /usr/bin/systemctl restart java-app"

                        sleep 3

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@172.31.3.96 \
                        "curl -fsS http://localhost:8080/api/hello"
                    '''
                }
            }
        }

        stage('Docker Deployment') {
            steps {
                sshagent(credentials: ['app-ec2-ssh-key']) {
                    sh '''
                        echo "Copying Dockerfile and JAR to application EC2"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@172.31.3.96 \
                        "mkdir -p /home/ubuntu/java-app/target"

                        scp -o StrictHostKeyChecking=no \
                        Dockerfile \
                        ubuntu@172.31.3.96:/home/ubuntu/java-app/Dockerfile

                        scp -o StrictHostKeyChecking=no \
                        target/java-devops-project-1.0.0.jar \
                        ubuntu@172.31.3.96:/home/ubuntu/java-app/target/java-devops-project-1.0.0.jar

                        echo "Building Docker image"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@172.31.3.96 \
                        "cd /home/ubuntu/java-app && sudo docker build -t java-devops-app:latest ."

                        echo "Replacing old Docker container"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@172.31.3.96 \
                        "sudo docker rm -f java-devops-container 2>/dev/null || true"

                        echo "Starting new Docker container"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@172.31.3.96 \
                        "sudo docker run -d --name java-devops-container --restart unless-stopped -p 8081:8080 java-devops-app:latest"

                        sleep 5

                        echo "Testing Docker application"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@172.31.3.96 \
                        "curl -fsS http://localhost:8081/api/hello"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Build, S3 upload, Java deployment and Docker deployment successful!'
        }

        failure {
            echo 'Pipeline failed. Check Console Output.'
        }
    }
}
