#!groovy
properties([
        [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/hmcts/tribunals-case-api'],
        parameters([
                string(description: 'environment to  deploy', defaultValue: 'dev', name: 'deployEnvironment')
        ]),
        pipelineTriggers([[$class: 'GitHubPushTrigger']])
])

@Library("Infrastructure")

import uk.gov.hmcts.contino.WebAppDeploy

def product = "sscs-tribunals"
def javaDeployer = new WebAppDeploy(this, product, "api")
def computeCluster = "core-compute-sample"

node {
    deployEnvironment =  (deployEnvironment in ['dev', 'test'])
                                ? deployEnvironment : error ("${deployEnvironment} is not one of reforms deployment environments")

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
        }

    }

    stage('Deploy - Dev') {
        javaDeployer.deployJavaWebApp(deployEnvironment,"${computeCluster}-dev", 'build/libs/tribunals-case-api-1.0.0.jar',
                'src/main/resources/application_env.yml', 'web.config')
    }
    stage('Smoke Test -Dev') {
        sleep(208)
        SMOKETEST_URL = "http://sscs-tribunals-api-"+deployEnvironment+".${computeCluster}-dev.p.azurewebsites.net/health"
        sh "curl -vf $SMOKETEST_URL"
    }
}