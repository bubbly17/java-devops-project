
pipeline {

    agent {
        label 'temporary-agent'
    }

    tools {
        maven 'maven'
    }

    parameters {
        string(
            name: 'AWS_REGION',
            defaultValue: 'ap-south-1',
            description: 'Enter your AWS region'
        )

        string(
            name: 'S3_BUCKET',
            defaultValue: 'vaishnavi-java-jar-artifacts-2026',
            description: 'Enter the S3 bucket name'
        )

        string(
            name: 'APP_NAME',
            defaultValue: 'java-devops-project',
            description: 'Enter your Maven application name'
        )

        string(
            name: 'APP_SERVER',
            defaultValue: '172.31.3.96',
            description: 'Enter the private IP of the EC2 Docker build server'
        )

        string(
            name: 'ECR_REPO',
            defaultValue: '324464329126.dkr.ecr.ap-south-1.amazonaws.com/java-devops-project',
            description: 'Enter the complete ECR repository URI'
        )
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

                    def jarFile = sh(
                        script: """
                            find target -maxdepth 1 -type f \
                            -name '${params.APP_NAME}-*.jar' \
                            ! -name '*-sources.jar' \
                            ! -name '*-javadoc.jar' \
                            | head -n 1
                        """,
                        returnStdout: true
                    ).trim()

                    if (!jarFile) {
                        error "No JAR found for ${params.APP_NAME}"
                    }

                    sh """
                        aws s3 cp "${jarFile}" \
                        "s3://${params.S3_BUCKET}/${params.APP_NAME}/${params.APP_NAME}-${version}.jar" \
                        --region "${params.AWS_REGION}"
                    """
                }
            }
        }

        stage('Prepare Docker Build') {
            steps {
                sshagent(credentials: ['app-ec2-ssh-key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${APP_SERVER} \
                        "mkdir -p /home/ubuntu/docker-build && rm -rf /home/ubuntu/docker-build/*"

                        scp -o StrictHostKeyChecking=no \
                        Dockerfile pom.xml \
                        ubuntu@${APP_SERVER}:/home/ubuntu/docker-build/

                        scp -r -o StrictHostKeyChecking=no \
                        src \
                        ubuntu@${APP_SERVER}:/home/ubuntu/docker-build/
                    '''
                }
            }
        }

        stage('Docker Build and Push to ECR') {
            steps {
                script {
                    def imageTag = "build-${BUILD_NUMBER}"
                    def registry = params.ECR_REPO.split('/')[0]

                    sshagent(credentials: ['app-ec2-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${params.APP_SERVER} \
                            "aws ecr get-login-password --region ${params.AWS_REGION} | sudo docker login --username AWS --password-stdin ${registry}"

                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${params.APP_SERVER} \
                            "cd /home/ubuntu/docker-build && sudo docker build -t ${params.ECR_REPO}:${imageTag} ."

                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${params.APP_SERVER} \
                            "sudo docker push ${params.ECR_REPO}:${imageTag}"
                        """
                    }

                    echo "Docker image pushed: ${params.ECR_REPO}:${imageTag}"
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Maven build, S3 upload, Docker build and ECR push completed!'
        }

        failure {
            echo 'Pipeline failed. Check Console Output.'
        }
    }
}
