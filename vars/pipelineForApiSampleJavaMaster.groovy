def call(env){
    pipeline {
        agent none
        
        tools {
            // Install the Maven version configured as "MAVEN_HOME" and add it to the path.
            maven "MAVEN_HOME"
        }

        stages {
            stage('Checkout') {
                agent { label 'main' }
                steps {
                    script {
                        // Get code from GitHub repository
                        git 'https://github.com/jprietor/apiSampleJava.git'
                    }
                }
            }
            stage('Compile') {
                agent { label 'main' }
                steps {
                    script {
                        // Run Maven skipping tests
                        sh "mvn clean package -DskipTests"
                    }
                }

                post {
                    success {
                        archiveArtifacts 'target/*.jar'
                    }
                }
            }
            stage('Test') {
                agent { label 'main' }
                steps {
                    script {
                        sh "mvn test"
                    }
                }
            }
            stage('Build') {
                agent { label 'docker' }
                steps {
                    script {
                        sh "docker build ${env.DOCKERFILE_LOCATION} -t ${env.DOCKER_IMAGE}:${env.VERSION}"
                    }
                }
            }
            stage('Deliver') {
                agent { label 'docker' }
                steps {
                    script {
                        sh "docker push ${env.DOCKER_IMAGE}:${env.VERSION}"
                    }
                }
            }
            stage('Deploy') {
                agent { label 'main' }
                steps {
                    script {
                        println "Kubernetes deployment..."
                    }
                }
            }
        }
    }
}