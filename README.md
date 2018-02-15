./# SSCS - Tribunals Case API

##Purpose
Tribunals case api is a spring boot based application to create new appeals for the SSCS Appellants


###Prerequisites

For versions and complete list of dependencies see build.gradle

* Java 8
* Spring Boot
* Gradle

##Building and deploying the application
  
###Building the application

To build the project execute the following command:

```
./gradlew build
```

### Running the application

Run the application by executing:

```
./gradlew bootRun
```

###Running in Docker
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


###Unit tests

To run all unit tests execute the following command:

```
./gradlew test
```
