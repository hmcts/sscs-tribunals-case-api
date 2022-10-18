# SSCS - Tribunals Case API


## Purpose 
Tribunals case api is a spring boot based application to create new appeals for the SSCS Appellants


### Prerequisites

For versions and complete list of dependencies see build.gradle

* Java 8
* Spring Boot
* Gradle

## Building and deploying the application 
  
### Building the application

To build the project execute the following command:

```
./gradlew build
```

### Running the application

Run the application by executing:

```
./gradlew bootRun
```

### Cftlib

This repo is now integrated with the rse-cft-library. For more information see the bootWithCCD task in build.gradle.
Extra compose files can be provided in src/cftlib/resources/docker.

### Running Smoke Test locally
Once the application running locally, please make sure
1. Your local CCD is up and running with subscription id "7S9MxdSBpt"
2. Execute ./gradlew --info smoke

### Running in Docker(Work in progress...)
Create the image of the application by executing the following command:

```
  ./gradlew installDist
```

Create docker image:

```
  docker-compose build
```

Run the distribution by executing the following command:

```
  docker-compose up
```

This will start the API container exposing the application's port

In order to test if the application is up, you can call its health endpoint:

```
  curl http://localhost:8080/health
```

You should get a response similar to this:

```
  {"status":"UP"}
```


### Unit tests

To run all unit tests execute the following command:

```
./gradlew test
```
### Mutation tests

To run all mutation tests execute the following command:

```
./gradlew pitest
```

### Running contract or pact tests:

You can run contract or pact tests as follows:

```
./gradlew contract
```

You can then publish your pact tests locally by first running the pact docker-compose:

```
docker-compose -f docker-pactbroker-compose.yml up
```

and then using it to publish your tests:

```
./gradlew pactPublish
```


or to run the tests and publish in one go you can run the following:

```
./gradlew runAndPublishConsumerPactTests
```

In order to run the CCD Provider Tests to determine that the CCD Consumer contract tests published here can be verified you'll need to check out the following github repo:

```
https://github.com/hmcts/ccd-data-store-api/tree/TA-82_CcdProviderPactTest
```
And run the following test ensuring that the Pacts created here are published to your local Pact Broker instance and that you have an up to date CCD Docker setup running locally with up to date CCD definition deployed

```
https://github.com/hmcts/ccd-data-store-api/blob/TA-82_CcdProviderPactTest/src/contractTest/java/uk/gov/hmcts/ccd/v2/external/controller/CasesControllerProviderTest.java
```
Please change the version tag to point to 'Dev' and ensure the Pact Broker annotations point to your local Pact Broker instance

```
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}",
    port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
    @VersionSelector(tag = "Dev")})
```

Also if you require the Provider verification to be published uncomment the following line before running the CasesControllerProviderTest:

```
  System.getProperties().setProperty("pact.verifier.publishResults", "true");
```

If you need to run the CCD provider test against the Preview environment for a particular branch of code then please ensure that the Pact Broker annotations are pointing to the central reform Pact Broker with the following credentials ( with the tag in the @VersionSelector pointing to your feature branch name of the PR):

```
@PactBroker(scheme = "${PACT_BROKER_SCHEME:https}",
    host = "${PACT_BROKER_URL:pact-broker.platform.hmcts.net}",
    port = "${PACT_BROKER_PORT:443}", consumerVersionSelectors = {
    @VersionSelector(tag = "feature_branch_name")})

```


## Gotchas

PRs that start with _"Bump"_ won't have a preview environment. The decision was made after we realised that most the preview environments were created by Depandabot.
