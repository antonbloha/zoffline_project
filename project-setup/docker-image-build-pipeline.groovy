pipeline {
    agent any
    options {
        ansiColor('xterm')
    }
    environment {
        DOCKERHUB_USER = ''
        DOCKERHUB_PASS = ''
    }
    stages {
        stage('Clone Git repo') {
            steps {
                git(branch: 'main', url: 'https://github.com/antonbloha/zoffline_project.git', credentialsId: 'jenkins_github_access')
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    def dateTag = sh(script: 'date +%Y%m%d%H%M%S', returnStdout: true).trim()
                    sh """
                    cd ./zwift-offline-zoffline_1.0.132734
                    docker build -t zwift-offline:${dateTag} .
                    """
                    env.IMAGE_NAME = "zwift-offline:${dateTag}"
                }
            }
        }
        stage('Verify the Image') {
            steps {
                script {
                    def imageExists = sh(
                        script: "docker images -q ${env.IMAGE_NAME}",
                        returnStatus: true
                    )
                    if (imageExists != 0) {
                        error("Docker image '${env.IMAGE_NAME}' not found, stopping pipeline.")
                    }
                }
            }
        }
        stage('Docker Hub Login and Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_PASS')]) {
                        def imageName = "${DOCKERHUB_USER}/zwift-offline:${env.IMAGE_NAME.split(':')[1]}"
                        sh """
                        echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        docker tag ${env.IMAGE_NAME} ${imageName}
                        docker push ${imageName}
                        docker logout
                        """
                        env.IMAGE_NAME = imageName  // Store the image name for later use
                    }
                }
            }
        }
        stage('Update image_tag.txt and Push to GitHub') {
            steps {
                script {
                    sshagent(['jenkins_github_access']) {
                        // Write the image name to image_tag.txt
                        sh """
                        echo "${env.IMAGE_NAME}" > ./zwift-offline-zoffline_1.0.132734/image_tag.txt
                        """

                        // Commit and push the change back to GitHub using SSH
                        sh """#!/bin/bash
                        cd ./zwift-offline-zoffline_1.0.132734
                        git add image_tag.txt
                        git commit -m 'Update image_tag.txt with ${env.IMAGE_NAME}'
                        git push git@github.com:antonbloha/zoffline_project.git main
                        """
                    }
                }
            }
        }
        stage('Cleanup Docker Images') {
            steps {
                script {
                    // Remove all unused Docker images
                    sh 'docker image prune -a -f'
                }
            }
        }
        stage('Start Container Creation Pipeline') {
            steps {
                script {
                    build job: 'Creating and Starting Docker Container', wait: false
                }
            }
        }
    }
}
