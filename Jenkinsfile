node {
    def WORKSPACE = "/var/lib/jenkins/workspace/keycloak-springboot-microservice"
    def dockerImageTag = "keycloak-springboot-microservice${env.BUILD_NUMBER}"

    try{
//          notifyBuild('STARTED')
         stage('Clone Repo') {
            // for display purposes
            // Get some code from a GitHub repository
            git url: 'https://github.com/nhatdoanexpo/keycloak-springboot-microservice.git',
                credentialsId: 'deploy-admin',
                branch: 'main'
         }
          stage('Build docker') {
                 dockerImage = docker.build("keycloak-springboot-microservice:${env.BUILD_NUMBER}")
          }
         stage('Deploy docker') {
             echo "Docker Image Tag Name: ${dockerImageTag}"
             sh 'docker stop keycloak-springboot-microservice || true && docker rm keycloak-springboot-microservice || true'

              // Lấy danh sách tất cả các hình ảnh có tên "keycloak-springboot-microservice"
                def all_images = sh(script: 'docker images | grep "keycloak-springboot-microservice" | awk \'{print $1":"$2}\'', returnStdout: true).trim().split('\n')

                // In log để kiểm tra danh sách các hình ảnh
                echo "All Images:"
                all_images.each { image ->
                    echo image
                }

                // Xóa các hình ảnh cũ trừ hình ảnh mới
                all_images.each { image ->
                    if (image != "keycloak-springboot-microservice:${env.BUILD_NUMBER}") {
                        // In log để kiểm tra việc xóa hình ảnh
                        echo "Deleting image: $image"
                        sh "docker rmi $image || true"
                    }
                }
             sh "docker run --name keycloak-springboot-microservice -d -p 9797:8080 --mount type=bind,source=/Files-Upload,target=/Files-Upload keycloak-springboot-microservice:${env.BUILD_NUMBER}"
         }
    }catch(e){
//         currentBuild.result = "FAILED"
        throw e
    }finally{
//         notifyBuild(currentBuild.result)
    }
}

def notifyBuild(String buildStatus = 'STARTED'){

// build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def now = new Date()
  // message
  def subject = "${buildStatus}, Job: ${env.JOB_NAME} FRONTEND - Deployment Sequence: [${env.BUILD_NUMBER}] "
  def summary = "${subject} - Check On: (${env.BUILD_URL}) - Time: ${now}"
  def subject_email = "Spring boot Deployment"
  def details = """<p>${buildStatus} JOB </p>
    <p>Job: ${env.JOB_NAME} - Deployment Sequence: [${env.BUILD_NUMBER}] - Time: ${now}</p>
    <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME}</a>"</p>"""


  // Email notification
    emailext (
         to: "nhat.doan.expo@gmail.com",
         subject: subject_email,
         body: details,
         recipientProviders: [[$class: 'DevelopersRecipientProvider']]
       )
}
