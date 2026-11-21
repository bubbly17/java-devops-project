
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
            description: 'Private IP of the EC2 Docker build server'
        )

        string(
            name: 'ECR_REPO',
            defaultValue: '324464329126.dkr.ecr.ap-south-1.amazonaws.com/java-devops-project',
            description: 'Complete ECR repository URI'
        )
    }

    environment {
        RELEASE_VERSION = ''
        RELEASE_JAR = ''
        RELEASE_JAR_PATH = ''
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate Git Tag') {
            steps {
                script {
                    if (!env.TAG_NAME) {
                        error 'No Git tag detected. Trigger this pipeline using a Git tag.'
                    }

                    if (!(env.TAG_NAME ==~ /v[0-9]+\.[0-9]+(\.[0-9]+)?/)) {
                        error "Invalid tag: ${env.TAG_NAME}. Use v1.0 or v1.0.0."
                    }

                    env.RELEASE_VERSION = env.TAG_NAME

                    echo "Release Version: ${env.RELEASE_VERSION}"
                }
            }
        }

        stage('Maven Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Prepare Versioned JAR') {
            steps {
                script {
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

                    env.RELEASE_JAR =
                        "${params.APP_NAME}-${env.RELEASE_VERSION}.jar"

                    env.RELEASE_JAR_PATH =
                        "target/${env.RELEASE_JAR}"

                    sh """
                        cp "${jarFile}" "${env.RELEASE_JAR_PATH}"
                    """

                    echo "Created JAR: ${env.RELEASE_JAR}"
                }
            }
        }

        stage('Verify JAR') {
            steps {
                sh 'ls -lh "${RELEASE_JAR_PATH}"'
            }
        }

        stage('Check AWS CLI') {
            steps {
                sh 'which aws'
                sh 'aws --version'
            }
        }

        stage('Upload JAR to S3') {
            steps {
                sh """
                    aws s3 cp "${RELEASE_JAR_PATH}" \
                    "s3://${params.S3_BUCKET}/${params.APP_NAME}/${env.RELEASE_JAR}" \
                    --region "${params.AWS_REGION}"
                """

                echo "S3 upload completed for ${env.RELEASE_JAR}"
            }
        }

        stage('Prepare Docker Build') {
            steps {
                sshagent(credentials: ['app-ec2-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no \
                        ubuntu@${params.APP_SERVER} \
                        "mkdir -p /home/ubuntu/docker-build && \
                        rm -rf /home/ubuntu/docker-build/*"

                        scp -o StrictHostKeyChecking=no \
                        Dockerfile pom.xml \
                        ubuntu@${params.APP_SERVER}:/home/ubuntu/docker-build/

                        scp -r -o StrictHostKeyChecking=no \
                        src \
                        ubuntu@${params.APP_SERVER}:/home/ubuntu/docker-build/
                    """
                }
            }
        }

        stage('Docker Build and Push to ECR') {
            steps {
                script {
                    def imageTag = env.RELEASE_VERSION
                    def registry = params.ECR_REPO.split('/')[0]

                    sshagent(credentials: ['app-ec2-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${params.APP_SERVER} \
                            "aws ecr get-login-password \
                            --region ${params.AWS_REGION} | \
                            sudo docker login \
                            --username AWS \
                            --password-stdin ${registry}"

                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${params.APP_SERVER} \
                            "cd /home/ubuntu/docker-build && \
                            sudo docker build \
                            -t ${params.ECR_REPO}:${imageTag} ."

                            ssh -o StrictHostKeyChecking=no \
                            ubuntu@${params.APP_SERVER} \
                            "sudo docker push \
                            ${params.ECR_REPO}:${imageTag}"
                        """
                    }

                    echo "Docker image pushed: ${params.ECR_REPO}:${imageTag}"
                }
            }
        }
    }

    post {
        success {
            echo """
            SUCCESS!
            Release Version: ${env.RELEASE_VERSION}
            JAR: ${env.RELEASE_JAR}
            S3: ${params.S3_BUCKET}/${params.APP_NAME}/${env.RELEASE_JAR}
            Docker: ${params.ECR_REPO}:${env.RELEASE_VERSION}
            """
        }

        failure {
            echo 'Pipeline failed. Check Console Output.'
        }
    }
}
