#!groovy
properties([
        [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/hmcts/tribunals-case-api'],
        pipelineTriggers([[$class: 'GitHubPushTrigger']])
])

@Library("Infrastructure")

import uk.gov.hmcts.contino.WebAppDeploy

def product = "sscs-tribunals"
def javaDeployer = new WebAppDeploy(this, product, "api")


node {

    stage('Checkout') {

        deleteDir()
        checkout scm
    }

    stage("Build + Test") {

        def mvnHome = tool 'apache-maven-3.3.9'
        env.PATH = "${mvnHome}/bin:${env.PATH}"
        stage('Compile') {
            sh "./gradlew clean compileJava"
        }

        stage('Test (Unit)') {
            sh "./gradlew test"
        }

        stage('Package (JAR)') {
            sh "./gradlew clean build -x test"
            stash name: product, includes: "build/libs/*.jar"
        }

    }

    stage('Deploy - Dev') {

        deployEnvironment = 'dev';

        unstash product

        javaDeployer.deployJavaWebApp(deployEnvironment, 'build/libs/tribunals-case-api-1.0.0.jar', 'web.config')

        javaDeployer.healthCheck('dev')
    }


    stage('Deploy - Prod') {

        deployEnvironment = 'prod';

        unstash product

        javaDeployer.deployJavaWebApp(deployEnvironment, 'build/libs/tribunals-case-api-1.0.0.jar', 'web.config')

        javaDeployer.healthCheck('prod')
    }


}
