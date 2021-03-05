def call(env){
    pipeline {
        agent none
        
        tools {
            // Install the Maven version configured as "MAVEN_HOME" and add it to the path.
            maven "MAVEN_HOME"
        }

        stages {
            agent none
            stage('Checkout') {
                steps {
                    script {
                        // Get code from GitHub repository
                        git 'https://github.com/jprietor/apiSampleJava.git'
                    }
                }
            }
            stage('Compile') {
                agent none
                steps {
                    script {
                        // Run Maven skipping tests
                        sh "mvn clean package -DskipTests"
                    }

                    post {
                        success {
                            archiveArtifacts 'target/*.jar'
                        }
                    }
                }
            }
            stage('Test') {
                agent none
                steps {
                    script {
                        sh "mvn test"
                    }
                }
            }
            stage('Build') {
                agent none
                steps {
                    script {
                        println "Docker build..."
                    }
                }
            }
            stage('Deliver') {
                agent none
                steps {
                    script {
                        println "Docker push..."
                    }
                }
            }
            stage('Deploy') {
                agent none
                steps {
                    script {
                        println "Kubernetes deployment..."
                    }
                }
            }
        }
    }
}