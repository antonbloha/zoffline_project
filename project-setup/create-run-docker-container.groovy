pipeline {
    agent any
    options {
        ansiColor('xterm')
    }
    stages {
        stage('Clean up Existing Containers') {
            steps {
                script {
                    // Check for running containers and stop them
                    sh '''
                    running_containers=$(docker ps -q)
                    if [ ! -z "$running_containers" ]; then
                        echo "Stopping running containers..."
                        docker stop $running_containers
                    else
                        echo "No running containers to stop."
                    fi
                    '''
                    
                    // Remove all stopped containers
                    sh '''
                    stopped_containers=$(docker ps -a -q)
                    if [ ! -z "$stopped_containers" ]; then
                        echo "Removing stopped containers..."
                        docker rm $stopped_containers
                    else
                        echo "No stopped containers to remove."
                    fi
                    '''
                }
            }
        }
        stage('Clone Git repo') {
            steps {
                git(branch: 'main', url: 'https://github.com/antonbloha/zoffline_project.git', credentialsId: 'jenkins_github_access')
            }
        }
        stage('Read Docker Image Tag') {
            steps {
                script {
                    def imageName = readFile('./zwift-offline-zoffline_1.0.132734/image_tag.txt').trim()
                    env.IMAGE_NAME = imageName
                }
            }
        }
        stage('Update Docker Compose with Image') {
            steps {
                script {
                    sh """
                    sed -i.bak 's|image: .*|image: ${env.IMAGE_NAME}|' ./zwift-offline-zoffline_1.0.132734/docker-compose.yml
                    """
                }
            }
        }
        stage('Create and Start Docker Container') {
            steps {
                script {
                    sh """
                    cd ./zwift-offline-zoffline_1.0.132734
                    docker-compose down || true
                    docker-compose up -d
                    """
                }
            }
        }
    }
}
