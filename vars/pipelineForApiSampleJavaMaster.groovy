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
                agent { 
                    docker { 
                        label 'docker'
                        image 'bitnami/kubectl' 
                        args '--entrypoint=""'
                    }
                }
                steps {
                    script {
                        def newDeployment = readYaml file: 'manifests/deployment.yaml'
                        def oldDeployment

                        withCredentials([file(credentialsId: 'kubeconfig', variable: 'kubeconfig')]) {
                            def deployments = sh( 
                                script: "kubectl --kubeconfig ${kubeconfig} get deployments --no-headers -l app=api-sample-java | wc -l",
                                returnStdout: true
                            )

                            if (deployments.toInteger() == 1) {
                                // Delete previous deployment and deploy new version...
                                println "TODO: redeployment"

                            }else if (deployments.toInteger() == 0) {
                                // Deploy new service
                                sh(
                                    script: "kubectl --kubeconfig ${kubeconfig} create -f manifests/deployment.yaml"
                                )
                                sh(
                                    script: "kubectl --kubeconfig ${kubeconfig} create -f manifests/service.yaml"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}