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

### Running Smoke Test locally
Once the application running locally, please make sure
1. Your local CCD is up and running with subscription id "7S9MxdSBpt"
2. Set Environment variable TEST_URL to where your api is running
    For example:  export TEST_URL=http://localhost:8080
3. Execute ./gradlew --info smoke

### Configure local hosts
If you are using the [sscs-docker](https://github.com/hmcts/sscs-docker) backend, add the following to your hosts file

```
127.0.0.1       dm-store
127.0.0.1       smtp-server
```

This allows your locally-running copy of the case api to reference the same URLs as the internally-running docker images.

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
/gradlew pitest

```