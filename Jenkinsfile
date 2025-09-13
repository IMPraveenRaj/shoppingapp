pipeline {
  agent any
  options { timestamps() }

  environment {
    // --- Docker Hub config ---
    REGISTRY        = 'docker.io'
    HUB_USER        = 'impraveenraj'            // your Docker Hub username
    IMAGE           = 'shoppingapp-backend'     // Docker Hub repo name
    DOCKER_BUILDKIT = '1'
    VERSION_TAG     = '0.0.1'                   // optional semantic version tag
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Test (Maven in Docker)') {
      agent {
        docker {
          image 'maven:3.9-eclipse-temurin-21'
          // mount a writable .m2 repo in the workspace
          args "-v ${WORKSPACE}/.m2:/opt/mvnrepo"
        }
      }
      steps {
        sh '''
          mkdir -p "${WORKSPACE}/.m2"
          mvn -B -Dmaven.repo.local=/opt/mvnrepo clean package
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

    stage('Smoke run (8086) in JRE container') {
      agent {
        docker {
          image 'eclipse-temurin:21-jre'
          args "-v ${WORKSPACE}:/ws -w /ws"
        }
      }
      steps {
        sh '''
          nohup java -jar target/*SNAPSHOT.jar >/tmp/app.log 2>&1 &
          APP_PID=$!
          for i in {1..30}; do
            curl -sf http://localhost:8086/api/products && OK=1 && break || sleep 1
          done
          kill $APP_PID || true
          test "$OK" = "1"
        '''
      }
    }

    stage('Docker Build & Push (main only)') {
      when { branch 'main' } // change if your default branch is "master"
      agent { label 'docker' } // requires a node with Docker installed
      steps {
        withCredentials([usernamePassword(credentialsId: 'DOCKERHUB_CREDS',
            usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
          sh """
            echo "$REG_PASS" | docker login $REGISTRY -u "$REG_USER" --password-stdin

            docker build -t $REGISTRY/$HUB_USER/$IMAGE:${GIT_COMMIT} .

            docker tag $REGISTRY/$HUB_USER/$IMAGE:${GIT_COMMIT} $REGISTRY/$HUB_USER/$IMAGE:latest

            if [ -n "$VERSION_TAG" ]; then
              docker tag $REGISTRY/$HUB_USER/$IMAGE:${GIT_COMMIT} $REGISTRY/$HUB_USER/$IMAGE:${VERSION_TAG}
            fi

            docker push $REGISTRY/$HUB_USER/$IMAGE:${GIT_COMMIT}
            docker push $REGISTRY/$HUB_USER/$IMAGE:latest
            if [ -n "$VERSION_TAG" ]; then
              docker push $REGISTRY/$HUB_USER/$IMAGE:${VERSION_TAG}
            fi
          """
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'pom.xml, Dockerfile, **/application.yml', allowEmptyArchive: true
    }
  }
}
