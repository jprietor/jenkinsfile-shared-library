def call(env){
    pipeline {
        agent none
        stages {
            stage('First Test') {
                script {
                    println "hello world"
                }
            }
        }
    }
}
