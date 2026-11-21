
pipeline {

    agent {
        label 'temporary-agent'
    }

    tools {
        maven 'maven'
    }

    parameters {
        string(
            name: 'TAG_NAME',
            defaultValue: 'v1.0',
            description: 'Git release tag to build, e.g. v1.0'
        )

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
        RELEASE_JAR = ''
        RELEASE_JAR_PATH = ''
    }

    stages {

        stage('Validate Git Tag') {
            steps {
                script {
                    def tagName = params.TAG_NAME?.trim()

                    if (!tagName) {
                        error 'TAG_NAME parameter is empty.'
                    }

                    if (!(tagName ==~ /^v[0-9]+\.[0-9]+(\.[0-9]+)?$/)) {
                        error "Invalid tag: ${tagName}. Use v1.0 or v1.0.0."
                    }

                    echo "Release Version: ${params.TAG_NAME}"
                }
            }
        }

        stage('Checkout Release Tag') {
            steps {
                checkout scm

                sh """
                    git rev-parse --verify "refs/tags/${params.TAG_NAME}^{commit}" >/dev/null

                    git checkout --force "refs/tags/${params.TAG_NAME}"

                    echo "Checked out release tag: ${params.TAG_NAME}"

                    git log -1 --oneline
                """
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
                        "${params.APP_NAME}-${params.TAG_NAME}.jar"

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
                    aws s3 cp "${env.RELEASE_JAR_PATH}" \
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
                        find /home/ubuntu/docker-build -mindepth 1 -maxdepth 1 -exec rm -rf {} +"

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
                    def imageTag = params.TAG_NAME
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
            Release Version: ${params.TAG_NAME}
            JAR: ${env.RELEASE_JAR}
            S3: ${params.S3_BUCKET}/${params.APP_NAME}/${env.RELEASE_JAR}
            Docker: ${params.ECR_REPO}:${params.TAG_NAME}
            """
        }

        failure {
            echo 'Pipeline failed. Check Console Output.'
        }
    }
}
