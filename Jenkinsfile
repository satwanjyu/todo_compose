pipeline {
    agent { any { image 'cimg/android' } }
    stages {
        stage('build') {
            steps {
                sh './gradlew --version'
            }
        }
    }
}
