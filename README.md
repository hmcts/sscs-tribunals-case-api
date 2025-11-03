# SSCS - Tribunals Case API

A Spring Boot application for creating new appeals for SSCS appellants.

## Table of Contents
- [Background](#background)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Development](#development)
- [Testing](#testing)
- [Docker](#docker)
- [Useful Commands](#useful-commands)
- [Gotchas](#gotchas)

## Background

The SSCS Tribunals Case API is part of the SSCS ecosystem, enabling appellants to submit appeals and interact with the tribunal system. It integrates with CCD and other services to manage appeals.

## Prerequisites

- **Java 21** - Required for running the application
- **Spring Boot** - Framework for building the application
- **Gradle** - Build tool for managing dependencies and tasks
- **Docker** - Used for containerizing the application and running dependencies such as CCD and Pact Broker locally. Ensure Docker is installed and running on your machine. [Download Docker](https://www.docker.com/products/docker-desktop)

For versions and a complete list of dependencies, see `build.gradle`.

## Quick Start

**NOTE:** If you haven't already connected to the HMCTS Azure environment through Azure CLI, you will need to do this. Please contact the development team for instructions.

1. **Build the application:**
   ```bash
   ./gradlew build
   ```

2. **Run the application:**
   ```bash
   ./gradlew bootWithCCD
   ```

3. **Test the application health:**
   ```bash
   curl http://localhost:8008/health
   ```
   Expected response:
   ```json
   {"status":"UP"}
   ```

## Development

### Configuration

The application integrates with the `rse-cft-library`. Additional Docker Compose files can be provided in `src/cftlib/resources/docker`.

### Running Cron Tasks

- Run a cron task locally:
   ```bash
   TASK_NAME=MigrateCasesTask ./gradlew bootRun
   ```
- Run a cron task in preview by adding the `pr-labels:job` label to the PR in GitHub. Configure additional settings in `charts/sscs-tribunals-api/values.job.preview.template.yaml`.

Refer to [this sample](https://github.com/hmcts/cnp-flux-config/tree/8a819d0f5d1d35f5d8c1e8610d8662419f0a0d1b/apps/sscs/sscs-cron) for setting up cron tasks in Flux.

## Testing

### Unit Tests
Run all unit tests:
```bash
./gradlew test
```

### Running Smoke Tests Locally

1. Ensure your local CCD is running with subscription ID `7S9MxdSBpt`.
2. Execute:
   ```bash
   ./gradlew --info smoke
   ```

### Contract or Pact Tests

1. Run contract tests:
   ```bash
   ./gradlew contract
   ```

2. Publish pact tests locally:
   ```bash
   docker-compose -f docker-pactbroker-compose.yml up
   ./gradlew pactPublish
   ```

3. Run and publish tests in one go:
   ```bash
   ./gradlew runAndPublishConsumerPactTests
   ```

4. Verify CCD Provider Tests:
   - Clone the [ccd-data-store-api](https://github.com/hmcts/ccd-data-store-api/tree/TA-82_CcdProviderPactTest).
   - Run the `CasesControllerProviderTest` with the following configuration:
     ```java
     @PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
         host = "${PACT_BROKER_URL:localhost}",
         port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
         @VersionSelector(tag = "Dev")})
     ```
   - Publish results:
     ```java
     System.getProperties().setProperty("pact.verifier.publishResults", "true");
     ```

## Docker (Work in progress...)

### Build and Run with Docker

1. **Create the distribution:**
   ```bash
   ./gradlew installDist
   ```

2. **Build the Docker image:**
   ```bash
   docker-compose build
   ```

3. **Run the application:**
   ```bash
   docker-compose up
   ```

## Useful Commands

### Gradle Commands
```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootWithCCD

# Run unit tests
./gradlew test

# Run smoke tests
./gradlew --info smoke
```

### Docker Commands
```bash
# Build Docker image
docker-compose build

# Run Docker containers
docker-compose up

# Test application health
curl http://localhost:8008/health
```

## Gotchas

### PRs Starting with "Bump"
Preview environments are not created for PRs starting with "Bump" due to a decision to avoid unnecessary environments created by Dependabot.

### Preview Cases Not Listing
Elastic indices may be missing on preview. Recreate them by logging into CCD admin (e.g., `https://admin-web-sscs-tribunals-api-pr-4091.preview.platform.hmcts.net/`) and clicking "Create Elasticsearch Indices."

This avoids re-triggering the pipeline build and saves time.

### Work allocation in preview
Work allocation is now enabled in preview. To enable work allocation in preview, you need to add the `pr-values:wa` label to your PR. This will ensure that the work allocation service is started and configured correctly for the preview environment.
If some events are not triggering WA tasks, or are not being recognised by the WA service, double check that the case-event.json entry for the event has the `Publish` field set to `"Y"`. If it is not set, then CCD will not publish the event to the message listener and the WA service will not be able to process it.



