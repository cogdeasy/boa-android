// Northbridge Mobile Engineering - retail Android pipeline.
// Runs on the shared mobile agents (JDK 8 + Android SDK 30 pre-installed).
pipeline {
    agent { label 'android-jdk8' }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 45, unit: 'MINUTES')
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-8-openjdk-amd64'
        ANDROID_SDK_ROOT = '/opt/android-sdk'
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh './gradlew clean assembleMockDebug assembleLiveDebug --console=plain'
            }
        }
        stage('Unit tests') {
            steps {
                sh './gradlew testMockDebugUnitTest jacocoTestReport --console=plain'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/build/test-results/**/*.xml'
                    publishHTML(target: [reportDir: 'app/build/reports/jacoco/jacocoTestReport/html',
                                         reportFiles: 'index.html', reportName: 'JaCoCo'])
                }
            }
        }
        stage('Lint') {
            steps {
                // Lint findings are tracked in MOB-2291; do not fail the build until the backlog is cleared.
                sh './gradlew lintMockDebug --console=plain || true'
            }
        }
        stage('UAT build') {
            when { branch 'main' }
            steps {
                sh './gradlew assembleLiveUat --console=plain'
                archiveArtifacts artifacts: 'app/build/outputs/apk/**/*.apk', fingerprint: true
            }
        }
    }
}
