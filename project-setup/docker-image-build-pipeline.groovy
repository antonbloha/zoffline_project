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
                sh '''
                cd ./zwift-offline-zoffline_1.0.132734
                docker build -t zwfit-offline .
                '''
            }
        }
        stage('Verify the Image') {
            steps {
                script {
                    def imageExists = sh(
                        script: 'docker images -q zwfit-offline',
                        returnStatus: true
                    )
                    if (imageExists != 0) {
                        error("Docker image 'zwfit-offline' not found, stopping pipeline.")
                    }
                }
            }
        }
        stage('Docker Hub Login and Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_PASS')]) {
                        def dateTag = sh(script: 'date +%Y%m%d%H%M%S', returnStdout: true).trim()
                        def imageName = "${DOCKERHUB_USER}/zwfit-offline:${dateTag}"
                        sh """
                        echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        docker tag zwfit-offline ${imageName}
                        docker push ${imageName}
                        docker logout
                        """
                        env.IMAGE_NAME = imageName  // Store the image name for later use
                    }
                }
            }
        }
        stage('Cleanup Docker Images') {
            steps {
                script {
                    def newImageId = sh(script: "docker images -q ${env.IMAGE_NAME}", returnStdout: true).trim()
                    
                    if (newImageId) {
                        def allImageIds = sh(script: 'docker images -q', returnStdout: true).trim().split("\n")
                        def imagesToRemove = allImageIds.findAll { it != newImageId }

                        if (imagesToRemove) {
                            sh "docker rmi -f ${imagesToRemove.join(' ')}"
                        }
                    } else {
                        error("Failed to find the image ID for the newly created image.")
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
        stage('Start Container Creation Pipeline') {
            steps {
                script {
                    build job: 'Creating and Starting Docker Container', wait: false
                }
            }
        }
    }
}
