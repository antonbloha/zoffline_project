pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'qvotan/zoffline:v1'
        CONTAINER_NAME = 'zwift-offline'
        HOST_STORAGE_PATH = '/home/ubuntu/zwift_storage/storage' 
        TIMEZONE = 'America/Los_Angeles'
    }

    stages {
        stage('Pull Docker Image') {
            steps {
                script {
                    echo "Pulling Docker image: ${DOCKER_IMAGE}"
                    sh "docker pull ${DOCKER_IMAGE}"
                }
            }
        }
        stage('Create Docker Container') {
            steps {
                script {
                    echo "Creating Docker container: ${CONTAINER_NAME}"
                    sh """
                        docker create --name ${CONTAINER_NAME} \
                        -p 443:443 \
                        -p 80:80 \
                        -p 3024:3024/udp \
                        -p 3025:3025 \
                        -p 5353:53/udp \
                        -v ${HOST_STORAGE_PATH}:/usr/src/app/zwift-offline/storage \
                        -e TZ=${TIMEZONE} \
                        ${DOCKER_IMAGE}
                    """
                }
            }
        }
        stage('Start Docker Container') {
            steps {
                script {
                    echo "Starting Docker container: ${CONTAINER_NAME}"
                    sh "docker start ${CONTAINER_NAME}"
                }
            }
        }
    }

    post {
        success {
            echo "Docker container ${CONTAINER_NAME} created and started successfully."
        }
        failure {
            echo "Failed to create or start Docker container."
        }
    }
}
