stage('Build & Test (Maven in Docker)') {
  agent {
    docker {
      image 'maven:3.9-eclipse-temurin-21'
      // run as root so we can write to the mounted cache, and add :rw,z for SELinux
      args "--user 0:0 -v ${WORKSPACE}/.m2:/opt/mvnrepo:rw,z"
    }
  }
  environment {
    // tell Maven to use the mounted repo
    MVN_REPO = "/opt/mvnrepo"
  }
  steps {
    sh '''
      mkdir -p "${MVN_REPO}"
      chmod -R 777 "${MVN_REPO}" || true
      mvn -B -Dmaven.repo.local="${MVN_REPO}" clean package
    '''
  }
  post {
    always {
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
      junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/*.xml'
      archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, onlyIfSuccessful: false
    }
  }
}
