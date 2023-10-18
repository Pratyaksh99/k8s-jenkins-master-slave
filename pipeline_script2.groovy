pipeline {
  agent any
  stages {
    // Building Docker Image
    stage('Building Docker Image') {
      steps {
        script {
          container('docker') {
            dockerImage = docker.build('test-k8s-jenkins')
          }
        }
      }
    }
    // Pushing to ECR
    stage('Pushing to ECR') {
      steps{
        script {
          container('docker') {
            docker.withRegistry('https://417060808647.dkr.ecr.us-east-1.amazonaws.com', 'ecr-creds') {
              dockerImage.push("$BUILD_NUMBER")
              dockerImage.push('latest')
            }
          }
        }
      }
    }
  }
}

