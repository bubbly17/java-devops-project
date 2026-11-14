
pipeline {

    agent {
        label 'temporary-agent'
    }

    tools {
        maven 'maven'
    }

    environment {
        AWS_REGION = 'ap-south-1'
        AWS_ACCOUNT_ID = '324464329126'

        S3_BUCKET = 'vaishnavi-java-jar-artifacts-2026'
        APP_NAME = 'java-devops-project'

        APP_SERVER = '172.31.3.96'

        ECR_REPO = '324464329126.dkr.ecr.ap-south-1.amazonaws.com/java-devops-project'
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
                        ubuntu@${APP_SERVER}:/home/ubuntu/java-app/app.jar

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${APP_SERVER} \
                        "sudo -n /usr/bin/systemctl restart java-app"

                        sleep 3

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${APP_SERVER} \
                        "curl -fsS http://localhost:8080/api/hello"
                    '''
                }
            }
        }

        stage('Prepare Docker Build') {
            steps {
                sshagent(credentials: ['app-ec2-ssh-key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${APP_SERVER} \
                        "mkdir -p /home/ubuntu/java-app/target"

                        scp -o StrictHostKeyChecking=no \
                        Dockerfile \
                        ubuntu@${APP_SERVER}:/home/ubuntu/java-app/Dockerfile

                        scp -o StrictHostKeyChecking=no \
                        target/java-devops-project-1.0.0.jar \
                        ubuntu@${APP_SERVER}:/home/ubuntu/java-app/target/java-devops-project-1.0.0.jar
                    '''
                }
            }
        }

        stage('Docker Build and Push to ECR') {
            steps {
                script {
                    def imageTag = "build-${BUILD_NUMBER}"

                    sshagent(credentials: ['app-ec2-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${APP_SERVER} \
                            "aws ecr get-login-password --region ${AWS_REGION} | sudo docker login --username AWS --password-stdin ${ECR_REPO.split('/')[0]}"

                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${APP_SERVER} \
                            "cd /home/ubuntu/java-app && sudo docker build -t ${ECR_REPO}:${imageTag} ."

                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${APP_SERVER} \
                            "sudo docker push ${ECR_REPO}:${imageTag}"
                        """
                    }

                    env.IMAGE_TAG = imageTag
                }
            }
        }

        stage('Deploy from ECR') {
            steps {
                sshagent(credentials: ['app-ec2-ssh-key']) {
                    sh '''
                        echo "Pulling image from ECR"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${APP_SERVER} \
                        "sudo docker pull ${ECR_REPO}:${IMAGE_TAG}"

                        echo "Removing old container"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${APP_SERVER} \
                        "sudo docker rm -f java-devops-container 2>/dev/null || true"

                        echo "Starting new container"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${APP_SERVER} \
                        "sudo docker run -d --name java-devops-container --restart unless-stopped -p 8081:8080 ${ECR_REPO}:${IMAGE_TAG}"

                        sleep 5

                        echo "Testing deployed application"

                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${APP_SERVER} \
                        "curl -fsS http://localhost:8081/api/hello"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Maven build, S3 upload, Java deployment, ECR push and Docker deployment completed!'
        }

        failure {
            echo 'Pipeline failed. Check Console Output.'
        }
    }
}
