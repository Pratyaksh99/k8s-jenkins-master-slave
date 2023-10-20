// NOT USED
pipeline {
  agent any
  environment {
      AWS_ACCOUNT_ID="417060808647"
      AWS_DEFAULT_REGION="us-east-1"
      IMAGE_REPO_NAME="test-k8s-jenkins"
      IMAGE_TAG="v1"
      REPOSITORY_URI = "417060808647.dkr.ecr.us-east-1.amazonaws.com/test-k8s-jenkins"
  }
  
  stages {

    stage('Install Dependecies') {
      steps {
        sh'curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"'
        sh'sudo apt-get install unzip'
        sh'unzip awscliv2.zipr'
        sh'./aws/install'
      }
    }
      
    stage('Logging into AWS ECR') {
      steps {
        script {
          sh """aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"""
        }    
      }
    }

    // Building Docker images
    stage('Building image') {
      steps{
        script {
          dockerImage = docker.build "${IMAGE_REPO_NAME}:${IMAGE_TAG}"
        }
      }
    }
  
    // Uploading Docker images into AWS ECR
    stage('Pushing to ECR') {
      steps{  
        script {
          sh """docker tag ${IMAGE_REPO_NAME}:${IMAGE_TAG} ${REPOSITORY_URI}:$IMAGE_TAG"""
          sh """docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${IMAGE_REPO_NAME}:${IMAGE_TAG}"""
        }
      }
    }

  }

}


pipeline {
  agent {
    kubernetes {
      //cloud 'kubernetes'
        yaml """
          kind: Pod
          metadata:
            name: img
          spec:
            containers:
            - name: img
              image: jessfraz/img
              imagePullPolicy: Always
              command:
              - cat
              tty: true
              volumeMounts:
                - name: docker-config
                  mountPath: /home/user/.docker
            volumes:
              - name: docker-config
                configMap:
                  name: docker-config
        """
    }
  }
  stages {
    stage('Build with Img') {
      environment {
        PATH = "/home/user/bin:$PATH"
      }
      steps {
        container(name: 'img') {
            sh 'wget https://amazon-ecr-credential-helper-releases.s3.us-east-2.amazonaws.com/0.3.1/linux-amd64/docker-credential-ecr-login'
            sh 'chmod +x docker-credential-ecr-login'
            sh 'mkdir ~/bin'
            sh 'mv docker-credential-ecr-login ~/bin/docker-credential-ecr-login'
            sh '''
            img build . -t 417060808647.dkr.ecr.us-east-1.amazonaws.com/test-k8s-jenkins:latest -t 417060808647.dkr.ecr.us-east-1.amazonaws.com/test-k8s-jenkins:vImg$BUILD_NUMBER
            '''
            sh ' img push 417060808647.dkr.ecr.us-east-1.amazonaws.com/test-k8s-jenkins:latest'
            sh ' img push 417060808647.dkr.ecr.us-east-1.amazonaws.com/test-k8s-jenkins:vImg$BUILD_NUMBER'
        }
      }
    }
  }
}