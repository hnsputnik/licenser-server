def DIFF
def COMMITMENT
pipeline {
    agent none
    environment {
        product = 'traveling'
        project_name = 'hotel-server'
        gopath = '/go/src/git.zapa.cloud/traveling'
        owner = 'tantnd@vng.com.vn'
        checkout_url = 'git@git.zapa.cloud:traveling/hotel-server.git'
        CHECKOUT_BRANCH = 'master'
        checkout_credentialsId = '5afa21c6-45d8-4bd3-9498-36e6694a6a6d'
        jenkins_credentialsId = '5687860f-ea2a-4092-b8b3-7d6a6b2345f4'
        registry = 'repo.zapa.cloud:8443'
        image_build = 'mep/gocompiler:1.13.3-travelling-cache'
        //Kubernetes server
        sshuser_kubernetes = 'jenkins'
        k8s_prod_server_ip = '10.50.32.22'
        k8s_sand_server_ip = '10.109.3.12'
        kubernetes_deployment_yaml = 'hotel-server.yaml'
        prod_pwd_kubernetes_scenario = '/mep_server/travelling/deployments'
        sand_pwd_kubernetes_scenario = '/mep_staging/stg-travelling/deployments'
    }
    // Pipeline
    stages {
        stage('Initializing') {
            agent any
            steps {
                echo "${FLOW}"
                echo "${RELEASE}"
                echo "${CHECKOUT_BRANCH}"
            }
        }
        stage ('Friday Build Sequences') {
            parallel {
                stage ('Staging') {
                    when {
                        allOf {
                            environment name: 'CHECKOUT_BRANCH', value: 'master'
                            environment name: 'FLOW', value: 'staging'
                        }
                    }
                    stages {
                        // Git pulling
                        stage('Git pulling') {
                            agent any
                            steps {
                                dir("$project_name") { 
                                    git url: "$checkout_url",  branch: "$CHECKOUT_BRANCH", credentialsId: "$checkout_credentialsId"
                                }
                                script {
                                    DIFF = sh (
                                        script: "cd $project_name/ && git diff HEAD^ HEAD",
                                        returnStdout: true
                                    ).trim()
                                    COMMITMENT = sh (
                                        script: "cd $project_name/ && git log -2 --pretty=%B",
                                        returnStdout: true
                                    ).trim()
                                }
                            }
                        }
                        // Golang build
                        stage('Golang build') {
                            agent {
                                docker {
                                    image '$registry/$image_build'
                                    //Mount environment for building
                                    args '-v ${WORKSPACE}/$project_name:$gopath/$project_name -v /var/lib/jenkins/workspace/.ssh:/root/.ssh --user root -e http_proxy=http://10.50.32.3:3128/ -e https_proxy=http://10.50.32.3:3128/ '
                                }   
                            } 
                            steps {
                                script {
                                    try {
                                        sh 'echo "StrictHostKeyChecking no " > /root/.ssh/config'
                                        sh 'echo "10.109.3.27   git.zapa.cloud" >> /etc/hosts'
                                        
                                        sh 'cd $gopath/$project_name && git config --global url."git@git.zapa.cloud:".insteadOf "https://git.zapa.cloud/"'
                                        sh 'cd $gopath/$project_name && go get -v .'
                                        //Golang build
                                        sh 'cd $gopath/$project_name && GOOS=linux CGO_ENABLED=0 go build -o hotel-server ./main.go'
                                        currentBuild.result = 'SUCCESS'
                                    }
                                    catch(Exception e) {
                                        currentBuild.result = 'ABORTED' 
                                        error('Stopping early…')
                                    }
                                }
                            } 
                        }
                        //Docker build & push
                        stage('Docker Build & Push') {
                            agent any
                            steps {
                                script {
                                    try {
                                        //Docker build project
                                        sh 'cd ${WORKSPACE}/$project_name && chmod +x entry-point.sh'
                                        sh 'cd ${WORKSPACE}/$project_name && docker build --build-arg http_proxy=http://10.50.32.3:3128 --build-arg https_proxy=http://10.50.32.3:3128 -f Dockerfile -t $registry/${FLOW}/$product/$project_name:v${RELEASE} .'
                                        withDockerRegistry([ credentialsId: "$jenkins_credentialsId", url: "https://$registry" ]) {
                                            sh 'cd ${WORKSPACE}/$project_name && docker push $registry/${FLOW}/$product/$project_name:v${RELEASE}'
                                        }
                                        currentBuild.result = 'SUCCESS'
                                    }
                                    catch(Exception e) {
                                        currentBuild.result = 'ABORTED' 
                                        error('Stopping early…')
                                    }
                                }    
                            }
                        }
                    }
                }
                stage ('Production') {
                    when {
                        allOf {
                            environment name: 'CHECKOUT_BRANCH', value: 'master'
                            environment name: 'FLOW', value: 'production'
                        }
                    }
                    stages {
                        // Git pulling
                        stage('Git pulling') {
                            agent any
                            steps {
                                dir("$project_name") { 
                                    git url: "$checkout_url",  branch: "$CHECKOUT_BRANCH", credentialsId: "$checkout_credentialsId"
                                }
                                script {
                                    DIFF = sh (
                                        script: "cd $project_name/ && git diff HEAD^ HEAD",
                                        returnStdout: true
                                    ).trim()
                                    COMMITMENT = sh (
                                        script: "cd $project_name/ && git log -2 --pretty=%B",
                                        returnStdout: true
                                    ).trim()
                                }
                            }
                        }
                        // Golang build
                        stage('Golang build') {
                            agent {
                                docker {
                                    image '$registry/$image_build'
                                    //Mount environment for building
                                    args '-v ${WORKSPACE}/$project_name:$gopath/$project_name -v /var/lib/jenkins/workspace/.ssh:/root/.ssh --user root -e http_proxy=http://10.50.32.3:3128/ -e https_proxy=http://10.50.32.3:3128/ '
                                }   
                            } 
                            steps {
                                script {
                                    try {
                                        sh 'echo "StrictHostKeyChecking no " > /root/.ssh/config'
                                        sh 'echo "10.109.3.27   git.zapa.cloud" >> /etc/hosts'

                                        sh 'cd $gopath/$project_name && git config --global url."git@git.zapa.cloud:".insteadOf "https://git.zapa.cloud/"'
                                        sh 'cd $gopath/$project_name && go get -v .'
                                        //Golang build
                                        sh 'cd $gopath/$project_name && GOOS=linux CGO_ENABLED=0 go build -o hotel-server ./main.go'
                                        currentBuild.result = 'SUCCESS'
                                    }
                                    catch(Exception e) {
                                        currentBuild.result = 'ABORTED' 
                                        error('Stopping early…')
                                    }
                                }
                            } 
                        }
                        //Docker build & push
                        stage('Docker Build & Push') {
                            agent any
                            steps {
                                script {
                                    try {
                                        //Docker build project
                                        sh 'cd ${WORKSPACE}/$project_name && chmod +x entry-point.sh'
                                        sh 'cd ${WORKSPACE}/$project_name && docker build --build-arg http_proxy=http://10.50.32.3:3128 --build-arg https_proxy=http://10.50.32.3:3128 -f Dockerfile -t $registry/${FLOW}/$product/$project_name:v${RELEASE} .'
                                        withDockerRegistry([ credentialsId: "$jenkins_credentialsId", url: "https://$registry" ]) {
                                            sh 'cd ${WORKSPACE}/$project_name && docker push $registry/${FLOW}/$product/$project_name:v${RELEASE}'
                                        }
                                        currentBuild.result = 'SUCCESS'
                                    }
                                    catch(Exception e) {
                                        currentBuild.result = 'ABORTED' 
                                        error('Stopping early…')
                                    }
                                }    
                            }
                        }
                        // stage('Get staging Release version') {
                        //     agent any
                        //     steps {
                        //         script {
                        //             RELL = sh (
                        //                 script: 'echo "${RELEASE}-0.1" | bc -l',
                        //                 returnStdout: true
                        //             ).trim()
                        //         }
                        //         echo "${RELL}"
                        //     }
                        // }
                    }
                }
            }
        }
        // Deploy
        stage('Deploy to Staging') {
            agent any
            when {
                allOf {
                    environment name: 'FLOW', value: 'staging'
                }
            }
            steps {
                script {
                    try {
                        retry(3) {
                            sleep 3
                            //kube edit then apply
                            // sh 'ssh $sshuser_kubernetes@$k8s_sand_server_ip sudo sed -i "s/$project_name:v[0-9].*[0-9]/$project_name:v${RELEASE}/g" $sand_pwd_kubernetes_scenario/$kubernetes_deployment_yaml'
                            // sh 'ssh $sshuser_kubernetes@$k8s_sand_server_ip sudo kubectl apply -f $sand_pwd_kubernetes_scenario/$kubernetes_deployment_yaml'
                            sh 'kubectl patch deployment myapp-deployment -p '{"spec":{"template":{"spec":{"containers":[{"name":"myapp","image":"$IMAGE"}]}}}}''

                        }
                    } catch(Exception e) {
                        currentBuild.result = 'ABORTED'
                        echo 'CANCELLED DEPLOYMENT'
                    }
                }
            }
        }
        // Deploy
        stage('Deploy to Production') {
            agent any
            when {
                allOf {
                    environment name: 'FLOW', value: 'production'
                }
            }
            steps {
                script {
                    emailext ( 
                        subject: "Jenkins:TRAVELLING: ${PROJECT_NAME} - Waiting for your approval",
                        mimeType: 'text/html',
                        to: 'hungnv4@vng.com.vn, vunt@vng.com.vn',
                        body: """
                            Hi manager,<br/>
                            Project <a style="text-transform: uppercase">travelling: <b>${env.PROJECT_NAME}</b></a> are waiting for your approval.<br/>
                            Click: <a href="${env.RUN_DISPLAY_URL}" target="_blank">HERE</a> to let me know your response! <b>You have 5 hours to submit your response.</b>
                            <p><i><b>PLEASE NOTE THIS</b></i></p>
                            <i>The last 2-Commitment:</i>
                            <pre style="color: rgba(224, 97, 0, 1);; word-wrap: break-word; white-space: pre-wrap;">${COMMITMENT}</pre>
                            <i>What's diff:</i>
                            <pre style="color:rgb(22, 41, 150); word-wrap: break-word; white-space: pre-wrap;">${DIFF}</pre>
                            </html>
                        """
                    )
                    try {
                        timeout(time: 5, unit: "HOURS") {
                            input message: 'Bạn có chắc chắn muốn Deploy service này không?', ok: 'Yes'
                        }
                        //SSH to Kubernetes Server
                        retry(3) {
                            sleep 3
                            //kubectl edit then apply
                            // sh 'ssh $sshuser_kubernetes@$k8s_prod_server_ip sudo sed -i "s/$project_name:v[0-9].*[0-9]/$project_name:v${RELEASE}/g" $prod_pwd_kubernetes_scenario/$kubernetes_deployment_yaml'
                            // sh 'ssh $sshuser_kubernetes@$k8s_prod_server_ip sudo kubectl apply -f $prod_pwd_kubernetes_scenario/$kubernetes_deployment_yaml'
                            sh 'kubectl patch deployment myapp-deployment -p '{"spec":{"template":{"spec":{"containers":[{"name":"myapp","image":"$IMAGE"}]}}}}''
                        }
                        
                        currentBuild.result = 'SUCCESS'
                    }
                    catch(Exception e) {
                        currentBuild.result = 'ABORTED'
                        echo 'CANCELLED DEPLOYMENT'
                    }                   
                }
            }
            post {
                success{
                    emailext ( 
                        subject: "Jenkins:TRAVELLING: ${PROJECT_NAME} - ${currentBuild.currentResult}",
                        mimeType: 'text/html',
                        to: 'tiendk@vng.com.vn, hungnv4@vng.com.vn, tantnd@vng.com.vn, vunt@vng.com.vn, vinhnm@vng.com.vn',
                        body: """
                            <!DOCTYPE html>
                            <html lang="en" >
                            <style>
                            p{text-transform: uppercase}
                            </style>
                            <meta charset="UTF-8">
                            <b><p>travelling: ${env.PROJECT_NAME} has been deployed to server successfully</p></b>
                            <i>Click here for <a href="${env.RUN_DISPLAY_URL}">[Build detail]</a></i>
                        """
                    )
                }
            }
        }
        // Cleanup
        stage('Clean up') {
            agent any
            steps {
                //DO NOT put the slash "/" before * [ex: (/*) or . (/.)]
                sh('#!/bin/sh -e\n' + 'cd ${WORKSPACE} && sudo rm -rf *')
            }
        }
    }
}