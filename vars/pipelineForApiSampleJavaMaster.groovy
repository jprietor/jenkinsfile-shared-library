def deployApp(newDeployment, kubeconfig) {
    newDeployment.metadata.name = "api-sample-java-${env.VERSION}"
    newDeployment.metadata.labels.version = "${env.VERSION}"
    newDeployment.spec.selector.matchLabels.version = "${env.VERSION}"
    newDeployment.spec.template.metadata.labels.version = "${env.VERSION}"
    newDeployment.spec.template.spec.containers[0].image = "${env.DOCKER_IMAGE}:${env.VERSION}"

    sh "rm deployment.yaml"
    writeYaml file: "deployment.yaml", data: newDeployment

    sh "kubectl --kubeconfig ${kubeconfig} apply -f deployment.yaml"
}

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

                            if (deployments.toInteger() >= 2) {
                                def deploymentToDelete = sh(
                                    script: "kubectl --kubeconfig ${kubeconfig} get deploy --no-headers --sort-by=.metadata.creationTimestamp -l app=api-sample-java | head -1 | awk '{print \$1}'",
                                    returnStdout: true
                                )

                                sh(
                                    script: "kubectl --kubeconfig ${kubeconfig} delete deploy ${deploymentToDelete}"
                                )

                                // Deploy new version
                                deployApp(newDeployment, kubeconfig)

                            } else if (deployments.toInteger() == 1) {
                                // Deploy new version
                                deployApp(newDeployment, kubeconfig)

                            } else if (deployments.toInteger() == 0) {
                                // Deploy service
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
            stage('Do Blue/Green') {
                agent {
                    docker { 
                        label 'docker'
                        image 'bitnami/kubectl' 
                        args '--entrypoint=""'
                    }
                }
                steps {
                    script {
                        input message: 'Do you want to switch environment?', ok: 'Switch!'
                        def patch = readYaml file: 'manifests/service-patch.yaml'
                        def deployVersion
                        def actualVersion
                        

                        withCredentials([file(credentialsId: 'kubeconfig', variable: 'kubeconfig')]) {
                            actualVersion = readYaml text: sh(script: "kubectl --kubeconfig ${kubeconfig} get svc ${env.SVC_NAME} -o yaml", returnStdout: true)
                            actualVersion = actualVersion.spec.selector.version
                            deployVersion = readYaml text: sh(script: "kubectl --kubeconfig ${kubeconfig} get deployment -l version!=${actualVersion} -o yaml", 
                                                                returnStdout: true)
                        }
                        
                        deployVersion = deployVersion.items[0].metadata.labels.version

                        patch.spec.selector.version = deployVersion

                        sh "rm patch.yaml"
                        writeYaml file: 'patch.yaml', data: patch

                        withCredentials([file(credentialsId: 'kubeconfig', variable: 'kubeconfig')]) {
                            sh(script: "kubectl --kubeconfig ${kubeconfig} patch service ${env.SVC_NAME} --patch \"\$(cat patch.yaml)\"")
                        }
                    }
                }
            }
        }
    }
}