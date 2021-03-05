def call(env){
    pipeline {
        stages {
            stage('First Test') {
                script {
                    println "hello world"
                }
            }
        }
    }
}
