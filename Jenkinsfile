pipeline {
    agent any

    tools {
        // Uncomment and adjust if your Jenkins has these configured
        // maven 'Maven-3.9'
        // nodejs 'NodeJS-18'
    }

    environment {
        // Keep builds from polluting the workspace unnecessarily
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
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
    }

    post {
        success {
            archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
            archiveArtifacts artifacts: 'frontend/dist/**', fingerprint: true, allowEmptyArchive: true
        }
        always {       }
    }
}
