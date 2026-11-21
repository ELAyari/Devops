pipeline {
    agent any

    triggers {
        githubPush() // Pour le webhook (si tu l'as configuré)
    }

    environment {
        DOCKER_HUB_USER = 'hasdryball'
        IMAGE_NAME      = "${DOCKER_HUB_USER}/devops-app"
        IMAGE_TAG       = "${env.BUILD_NUMBER}"
        DOCKER_CREDS_ID = 'docker-hub-creds'
        SONAR_SERVER    = 'SonarQube' // Nom dans Configurer le système
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
                    // On compile et on lance les tests (enlevant -DskipTests pour Sonar)
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
                // Injecte l'URL et le token depuis Jenkins
                withSonarQubeEnv("${SONAR_SERVER}") {
                    dir('backend') {
                        // L'analyse Maven demandée par le PDF
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    // Attend le résultat de SonarQube (nécessite le webhook Sonar -> Jenkins)
                    waitForQualityGate abortPipeline: true
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
            sh "docker rmi -f \$(docker images -q ${IMAGE_NAME}) || true"
        }
    }
}
