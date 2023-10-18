FROM jenkins/jenkins:lts-jdk11
# Pipelines with Blue Ocean UI and Kubernetes
RUN jenkins-plugin-cli --plugins blueocean kubernetes