pipeline {
  agent any
  options { timestamps(); ansiColor('xterm') }

  environment {
    // --- Docker Hub config ---
    REGISTRY        = 'docker.io'
    HUB_USER        = 'impraveenraj'             // your Docker Hub username
    IMAGE           = 'shoppingapp-backend'      // repository name on Docker Hub
    DOCKER_BUILDKIT = '1'                         // faster docker builds

    // Optional: set a version tag for releases (e.g., from a parameter or env)
    VERSION_TAG     = '0.0.1'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Test (Maven)') {
      agent { docker { image 'maven:3.9-eclipse-temurin-21' } }
      steps {
        sh 'mvn -B clean package'   // spring-boot:repackage is already bound in your POM
      }
      post {
        always {
          junit '**/target/surefire-reports/*.xml'
          junit '**/target/failsafe-reports/*.xml'
          archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
      }
    }

    stage('Smoke run (8086)') {
      steps {
        sh '''
          nohup java -jar target/*SNAPSHOT.jar >/tmp/app.log 2>&1 &
          APP_PID=$!
          # wait up to 30s for the app to answer on 8086
          for i in {1..30}; do
            curl -sf http://localhost:8086/api/products && OK=1 && break || sleep 1
          done
          kill $APP_PID || true
          test "$OK" = "1"
        '''
      }
    }

    stage('Docker Build & Push (main only)') {
      when { branch 'main' }                 // change to 'master' if needed
      agent { label 'docker' }               // node with Docker CLI/daemon
      steps {
        withCredentials([usernamePassword(credentialsId: 'DOCKERHUB_CREDS',
            usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
          sh """
            echo "$REG_PASS" | docker login $REGISTRY -u "$REG_USER" --password-stdin

            docker build -t $REGISTRY/$HUB_USER/$IMAGE:${GIT_COMMIT} .

            # Also tag as :latest
            docker tag $REGISTRY/$HUB_USER/$IMAGE:${GIT_COMMIT} $REGISTRY/$HUB_USER/$IMAGE:latest

            # Optional semantic version tag
            if [ -n "$VERSION_TAG" ]; then
              docker tag $REGISTRY/$HUB_USER/$IMAGE:${GIT_COMMIT} $REGISTRY/$HUB_USER/$IMAGE:${VERSION_TAG}
            fi

            # Push all tags
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
