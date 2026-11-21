
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
            description: 'Git release tag, for example v1.0'
        )

        string(
            name: 'AWS_REGION',
            defaultValue: 'ap-south-1',
            description: 'AWS region'
        )

        string(
            name: 'S3_BUCKET',
            defaultValue: 'vaishnavi-java-jar-artifacts-2026',
            description: 'S3 bucket name'
        )

        string(
            name: 'APP_NAME',
            defaultValue: 'java-devops-project',
            description: 'Application name'
        )

        string(
            name: 'APP_SERVER',
            defaultValue: '172.31.3.96',
            description: 'Docker build EC2 private IP'
        )

        string(
            name: 'ECR_REPO',
            defaultValue: '324464329126.dkr.ecr.ap-south-1.amazonaws.com/java-devops-project',
            description: 'ECR repository URL'
        )
    }

    stages {

        stage('Validate Git Tag') {
            steps {
                script {
                    def tagName = params.TAG_NAME?.trim()

                    if (!tagName) {
                        error 'TAG_NAME cannot be empty'
                    }

                    if (!(tagName ==~ /^v[0-9]+\.[0-9]+(\.[0-9]+)?$/)) {
                        error "Invalid tag format: ${tagName}. Use v1.0 or v1.0.0"
                    }

                    echo "Release Version: ${tagName}"
                }
            }
        }

        stage('Checkout Release Tag') {
            steps {
                checkout scm

                sh """
                    git fetch --tags --force

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

                    def releaseJar = "${params.APP_NAME}-${params.TAG_NAME}.jar"
                    def releaseJarPath = "target/${releaseJar}"

                    sh "cp '${jarFile}' '${releaseJarPath}'"

                    echo "Created JAR: ${releaseJarPath}"
                }
            }
        }

        stage('Verify JAR') {
            steps {
                sh """
                    ls -lh "target/${params.APP_NAME}-${params.TAG_NAME}.jar"
                """
            }
        }

        stage('Check AWS CLI') {
            steps {
                sh """
                    which aws
                    aws --version
                """
            }
        }

        stage('Upload JAR to S3') {
            steps {
                sh """
                    aws s3 cp \
                    "target/${params.APP_NAME}-${params.TAG_NAME}.jar" \
                    "s3://${params.S3_BUCKET}/${params.APP_NAME}/${params.APP_NAME}-${params.TAG_NAME}.jar" \
                    --region "${params.AWS_REGION}"
                """
            }
        }

        stage('Prepare Docker Build') {
            steps {
                sshagent(credentials: ['app-ec2-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.APP_SERVER} \
                        'mkdir -p /home/ubuntu/docker-build && rm -rf /home/ubuntu/docker-build/*'

                        scp -o StrictHostKeyChecking=no Dockerfile \
                            pom.xml \
                            ubuntu@${params.APP_SERVER}:/home/ubuntu/docker-build/

                        scp -r -o StrictHostKeyChecking=no src \
                            ubuntu@${params.APP_SERVER}:/home/ubuntu/docker-build/
                    """
                }
            }
        }

        stage('Docker Build and Push to ECR') {
            steps {
                sshagent(credentials: ['app-ec2-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.APP_SERVER} '
                            cd /home/ubuntu/docker-build

                            aws ecr get-login-password --region ${params.AWS_REGION} \
                            | sudo docker login --username AWS --password-stdin ${params.ECR_REPO.split('/')[0]}

                            sudo docker build \
                            -t ${params.ECR_REPO}:${params.TAG_NAME} .

                            sudo docker push ${params.ECR_REPO}:${params.TAG_NAME}
                        '
                    """
                }
            }
        }
    }

    post {
        success {
            echo """
            ==========================================
                     PIPELINE SUCCESS
            ==========================================

            Application : ${params.APP_NAME}
            Release Tag : ${params.TAG_NAME}

            S3 JAR:
            s3://${params.S3_BUCKET}/${params.APP_NAME}/${params.APP_NAME}-${params.TAG_NAME}.jar

            ECR Image:
            ${params.ECR_REPO}:${params.TAG_NAME}

            ==========================================
            """
        }

        failure {
            echo """
            ==========================================
                     PIPELINE FAILED
            ==========================================

            Application : ${params.APP_NAME}
            Release Tag : ${params.TAG_NAME}

            Check the Jenkins Console Output for errors.

            ==========================================
            """
        }
    }
}
