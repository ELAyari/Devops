pipeline {
    agent any

    environment {
        DOCKER_HUB_USER = 'hasdryball'
        IMAGE_NAME      = "${DOCKER_HUB_USER}/devops-app"
        IMAGE_TAG       = "${env.BUILD_NUMBER}"
        DOCKER_CREDS_ID = 'docker-hub-creds'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git rev-parse --short HEAD'
            }
        }

        stage('Backend - Clean & Package') {
            steps {
                dir('backend') {
                    sh 'mvn -B clean package -DskipTests'
                }
            }
        }

        stage('Frontend - Clean & Package') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest -f Dockerfile ."
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.DOCKER_CREDS_ID,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    '''
                    sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                    sh "docker push ${IMAGE_NAME}:latest"
                    sh 'docker logout || true'
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
            archiveArtifacts artifacts: 'frontend/dist/**', fingerprint: true, allowEmptyArchive: true
        }
        always {
            sh "docker rmi -f \$(docker images -q ${IMAGE_NAME}) || true"
        }
    }
}
