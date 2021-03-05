def call(env){
    pipeline {
        agent none
        stages {
            stage('First Test') {
                agent none
                steps {
                    script {
                        println "hello world"
                    }
                }
            }
        }
    }
}
