pipeline {
    agent any
    
    stages {
        stage('Clone Git Repository') {
            steps {
                git(branch: 'main', url: 'git@github.com:LocalCoding/DevOps_May_24.git', credentialsId: 'jenkins_github_access')
            }
        }
        stage('Navigate to Directory') {
            steps {
                script {
                    dir('repo') {
                        echo "Navigating to the directory where the docker-compose.yaml file is located"
                    }
                }
            }
        }
        stage('Pull Docker Image and Start Container with Docker Compose') {
            steps {
                script {
                    dir('repo') {
                        echo "Starting Docker container using docker-compose"
                        sh "docker-compose -f ${DOCKER_COMPOSE_FILE} up -d"
                    }
                }
            }
        }
        stage('Verify Docker Container') {
            steps {
                script {
                    echo "Verifying that Docker container is running."
                    sh "docker ps -a"
                }
            }
        }
    }

    post {
        success {
            echo "Docker container started successfully using Docker Compose."
        }
        failure {
            echo "Failed to start Docker container using Docker Compose."
        }
    }
}
