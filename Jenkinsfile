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
    }

    stages {

        // ─────────────────────────────────────────────────────────
        // Ensure Infrastructure — makes the pipeline self-healing
        // Starts MySQL + SonarQube if stopped, waits until ready.
        // This is what makes a push succeed even if you forgot
        // to `docker compose up` or start SonarQube manually.
        // ─────────────────────────────────────────────────────────
        stage('Ensure Infrastructure') {
            steps {
                sh '''
                    set -e

                    echo "==> Ensuring MySQL is up"
                    docker compose up -d mysql

                    echo "==> Ensuring SonarQube is up"
                    if ! docker ps --format '{{.Names}}' | grep -qx 'sonarqube'; then
                        docker compose -f docker-compose.tools.yml up -d sonarqube
                    fi

                    echo "==> Waiting for MySQL to accept connections"
                    for i in $(seq 1 60); do
                        if docker exec "$(docker compose ps -q mysql)" \
                             mysqladmin ping -h localhost -uroot -proot --silent 2>/dev/null; then
                            echo "MySQL ready"
                            break
                        fi
                        sleep 2
                        if [ "$i" -eq 60 ]; then echo "MySQL not ready"; exit 1; fi
                    done

                    echo "==> Waiting for SonarQube to be UP"
                    for i in $(seq 1 60); do
                        if curl -sf "$SONAR_HOST_URL/api/system/status" | grep -q '"status":"UP"'; then
                            echo "SonarQube ready"
                            break
                        fi
                        sleep 3
                        if [ "$i" -eq 60 ]; then echo "SonarQube not ready"; exit 1; fi
                    done

                    echo "==> Infrastructure OK"
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
                    dir('backend') {
                        sh '''
                            mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
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
            // Never delete :latest — only the build-specific tag
            sh "docker rmi -f ${IMAGE_NAME}:${IMAGE_TAG} || true"
        }
    }
}
