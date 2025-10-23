
# disa-returns-stubs
App Description to be confirmed

### Before running the app

This repository relies on having mongodb running locally. You can start it with:

```bash
# first check to see if mongo is already running
docker ps | grep mongodb

# if not, start it
docker run --restart unless-stopped --name mongodb -p 27017:27017 -d percona/percona-server-mongodb:7.0 --replSet rs0
```

Reference instructions for [setting up docker](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/install-docker.html) and [running mongodb](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-mongodb.html#install-mongodb-applesilicon-mac).

### Running the app locally

```bash
sbt 'run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes'
```

You can then query the app to ensure it is working with the following command:

```bash
# other useful commands
sbt clean

sbt reload

sbt compile
```

### Running the test suite

To run the unit tests:

```bash
sbt test
```

To run the integration tests:

```bash
sbt it/test
```

### Before you commit

This service leverages scalaFmt to ensure that the code is formatted correctly.

Before you commit, please run the following commands to check that the code is formatted correctly:

```bash
# checks all source and sbt files are correctly formatted
sbt prePrChecks

# if checks fail, you can format with the following commands

# formats all source files
sbt scalafmtAll

# formats all sbt files
sbt scalafmtSbt

# formats just the main source files (excludes test and configuration files)
sbt scalafmt
```
# Stubbed Endpoints:

## NPS submit monthly return

- This endpoint is used to submit ISA monthly reporting data to NPS.

### Endpoint:
```bash
POST /nps/submit/:isaManagerReferenceNumber
```

### ISA Manager Reference Based Responses:

| ISA_MANAGER_REF | Status |        Type         |
|:---------------:|:------:|:-------------------:|
|      Z1400      |  400   |     BAD REQUEST     |
|      Z1503      |  503   | SERVICE UNAVAILABLE |
|       Any       |  204   |     NO CONTENT      |

## NPS - Notify Obligation Status Update

- This endpoint is used to notify NPS of the obligation status update.

### Endpoint:
```bash
POST /nps/declaration/:isaManagerReferenceNumber
```
### ISA Manager Reference Based Responses:

| ISA_MANAGER_REF  | Status |         Type          |
|:-----:|:------:|:---------------------:|
| Z1500 |  500   | INTERNAL SERVER ERROR |
|  Any  |  204   |      NO CONTENT       |

## NPS Retrieve Reconciliation Report

- This endpoint is used to retrieve reconciliation report from NPS.

### Endpoint:
```bash
GET /monthly/:isaManagerReferenceNumber/:taxYear/:month/results
```

- This endpoint requires a report to be generated either via the stub test-only endpoint or disa-returns-test-support-api. 
- If no report is generated then any ISA_MANAGER_REF other than Z1500 will return 404 NOT_FOUND

### ISA Manager Reference Based Responses:

| ISA_MANAGER_REF  | Status  |         Type          |
|:----------------:|:-------:|:---------------------:|
|      Z1500       |   500   | INTERNAL SERVER ERROR |
|       Any        | 200/404 | NO CONTENT/NOT FOUND  |


## ETMP Retrieve Obligation Status

- This endpoint is used to check the obligation status in ETMP.
- If the supplied isaManagerReferenceNumber is not found in mongo, then it will store the obligation as open.
- If the supplied isaManagerReferenceNumber is found in mongo, the store obligation status will be returned.

### Endpoint:
```bash
GET /etmp/check-obligation-status/:isaManagerReferenceNumber
```
### Responses:

|                         Scenario                         | Status |   Type    |
|:--------------------------------------------------------:|:------:|:---------:|
|          Successfully returns obligation status          |  200   |    OK     |

## ETMP Retrieve Reporting Window Status

- This endpoint is used to check the reporting window status in ETMP.

### Endpoint:
```bash
GET /etmp/check-reporting-window
```

### Responses:

|                   Scenario                   | Status |    Type    |
|:--------------------------------------------:|:------:|:----------:|
| Successfully returns reporting window status |  204   | NO CONTENT |
|          Reporting window not found          |  404   | NOT FOUND  |

## ETMP Submit Updated Obligation Status

- This endpoint is used to update the obligation status to closed/already met in ETMP.

### Endpoint:
```bash
POST /etmp/declaration/:isaManagerReferenceNumber
```

### Responses:

|                   Scenario                   | Status |   Type    |
|:--------------------------------------------:|:------:|:---------:|
|         Successful         |  204   | NO CONTENT |

# Test-Only Endpoints:

## ETMP Open Obligation Status

- This test-only endpoint is used to open the obligation status for the supplied isaManagerReferenceNumber.

### Endpoint:
```bash
POST /etmp/open-obligation-status/:isaManagerReferenceNumber
```

### Responses:

|               Scenario                | Status | Type |
|:-------------------------------------:|:------:|:----:|
| Successfully opened obligation status |  200   |  OK  |


## ETMP Set Reporting Window Status

- This test-only endpoint is used to set the ETMP reporting window status.
- Simulates both reporting window open and closed for the stubbed ETMP reporting window status endpoint.

### Endpoint:
```bash
POST /etmp/reporting-window-state
```

### Responses:

|             Scenario              | Status |    Type     |
|:---------------------------------:|:------:|:-----------:|
|  Missing or invalid request body  |  400   | BAD REQUEST |
| Successfully set reporting window |  200   | NO CONTENT  |

## ETMP Retrieve Reporting Window Status

- This test-only endpoint is used to retrieve the ETMP reporting window status.

### Endpoint:
```bash
GET /etmp/reporting-window-state
```

### Responses:

|               Scenario                | Status |   Type    |
|:-------------------------------------:|:------:|:---------:|
|      Not found reporting window       |  404   | NOT FOUND |
| Successfully returns reporting window |  200   |    OK     |

## NPS Generate Reconciliation report

- This test-only endpoint is used to generate an NPS reconciliation report for the supplied isaManagerReferenceNumber, taxYear & month. 
- You can generate reports containing issues identified: traceAndMatch, oversubscribed & failedEligibility.
- The number supplied for each field in the request body determines how many issues of that type will be generated in the report.

### Endpoint:
```bash
POST /:isaManagerReferenceNumber/:year/:month/reconciliation
```

### Request Body Example:
```bash 
json { "oversubscribed": 100000, "traceAndMatch": 100000, "failedEligibility": 100000 } 
```

### Responses:

|           Scenario            | Status |         Type          |
|:-----------------------------:|:------:|:---------------------:|
| Successfully generated report |  204   |    NO CONTENT         | 
|            failed             |  500   | INTERNAL SERVER ERROR |


### Further documentation

You can view further information regarding this service via our [service guide](#).

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").