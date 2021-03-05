def call(env){
    pipeline(env) {
        stages {
            stage('First Test') {
                script {
                    println "hello world"
                }
            }
        }
    }
}
