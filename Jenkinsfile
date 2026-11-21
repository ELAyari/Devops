pipeline {
    agent any

    triggers {
        githubPush()
    }

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

        stage('Backend - Build & Test') {
            steps {
                dir('backend') {
                    // Compilation et tests du backend
                    sh 'mvn -B clean package'
                }
            }
        }

        stage('Frontend - Build') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('backend') {
                    // Commande complète pour l'analyse Sonar avec Maven
                    sh '''
                        mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                        -Dsonar.projectKey=DevOps-AppGestionDesProjets \
                        -Dsonar.projectName='DevOps-AppGestionDesProjets' \
                        -Dsonar.host.url=http://192.168.100.132:9000 \
                        -Dsonar.token=sqp_c15a90815c89a724ec1e554ca59c4bf5ef6951ec
                    '''
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
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
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
            sh '''
                if [ -n "$(docker images -q ${IMAGE_NAME})" ]; then
                    docker rmi -f $(docker images -q ${IMAGE_NAME}) || true
                fi
            '''
        }
    }
}

