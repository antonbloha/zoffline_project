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
                        def dateTag = sh(script: 'date +%Y%m%d', returnStdout: true).trim()
                        def imageName = "${DOCKERHUB_USER}/zwfit-offline:${dateTag}"
                        sh """
                        echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        docker tag zwfit-offline ${imageName}
                        docker push ${imageName}
                        docker logout
                        """
                    }
                }
            }
        }
        stage('Cleanup Docker Images') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_PASS')]) {
                        def dateTag = sh(script: 'date +%Y%m%d', returnStdout: true).trim()
                        def imageName = "${DOCKERHUB_USER}/zwfit-offline:${dateTag}"
                        def newImageId = sh(script: "docker images -q ${imageName}", returnStdout: true).trim()
                        
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
        }
    }
}
