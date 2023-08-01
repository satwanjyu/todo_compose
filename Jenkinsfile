pipeline {
    agent {
        any {
            image 'cimg/android'
        }
    }
    stages {
        stage('Build APK') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew assembleRelease'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/*.apk'
                }
            }
        }
    }
}
