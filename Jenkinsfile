pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        githubPush()
    }

    environment {
        DOCKER_HUB_USER    = 'hasdryball'
        IMAGE_NAME         = "${DOCKER_HUB_USER}/nasri_3cacc1_appgestiondesprojets"
        IMAGE_TAG          = "${env.BUILD_NUMBER}"
        DOCKER_CREDS_ID    = 'docker-hub-creds'
        SONAR_CREDS_ID     = 'sonar-token'
        SONAR_HOST_URL     = 'http://localhost:9000'
        SONAR_PROJECT_KEY  = 'DevOps-AppGestionDesProjets'
        SONAR_PROJECT_NAME = 'DevOps-AppGestionDesProjets'
        MYSQL_CONTAINER    = 'devops-appgestiondesprojets-mysql-1'
        SONAR_CONTAINER    = 'sonarqube'
    }

    stages {

        // Create the container if it doesn't exist, otherwise just start it.
        // Only MySQL is awaited here; SonarQube boots in the background
        // while Maven and npm run, and is awaited right before the analysis.
        stage('Ensure Infrastructure') {
            steps {
                sh '''
                    set -e

                    echo "==> MySQL"
                    if docker inspect "$MYSQL_CONTAINER" >/dev/null 2>&1; then
                        docker start "$MYSQL_CONTAINER"
                    else
                        docker compose up -d mysql
                    fi

                    echo "==> SonarQube (no wait here)"
                    if docker inspect "$SONAR_CONTAINER" >/dev/null 2>&1; then
                        docker start "$SONAR_CONTAINER"
                    else
                        docker compose -f docker-compose.tools.yml up -d sonarqube
                    fi

                    echo "==> Waiting for MySQL to be healthy"
                    for i in $(seq 1 60); do
                        status=$(docker inspect -f '{{.State.Health.Status}}' "$MYSQL_CONTAINER")
                        if [ "$status" = "healthy" ]; then
                            echo "MySQL ready"
                            break
                        fi
                        if [ "$i" -eq 60 ]; then echo "MySQL not ready"; exit 1; fi
                        sleep 2
                    done
                '''
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git rev-parse --short HEAD'
            }
        }

        stage('Backend - Build & Test') {
            steps {
                dir('backend') {
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
                withCredentials([string(credentialsId: env.SONAR_CREDS_ID, variable: 'SONAR_TOKEN')]) {
                    // Wait for SonarQube just before it is needed (up to 6 min on cold start)
                    sh '''
                        echo "==> Waiting for SonarQube to be UP"
                        for i in $(seq 1 120); do
                            if curl -sf "$SONAR_HOST_URL/api/system/status" | grep -q '"status":"UP"'; then
                                echo "SonarQube ready"
                                break
                            fi
                            if [ "$i" -eq 120 ]; then echo "SonarQube not ready"; exit 1; fi
                            sleep 3
                        done
                    '''
                    dir('backend') {
                        // No clean/verify: reuse the build from the previous stage
                        sh '''
                            mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.token=${SONAR_TOKEN}
                        '''
                    }
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
                    sh "docker push --all-tags ${IMAGE_NAME}"
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
            // Never delete :latest, only the build-specific tag
            sh "docker rmi -f ${IMAGE_NAME}:${IMAGE_TAG} || true"
        }
    }
}
