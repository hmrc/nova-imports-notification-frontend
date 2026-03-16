# nova-imports-notification-frontend

## Service Overview

This is a frontend microservice for handling vehicle import notifications to HMRC.

This service connects to the `nova-imports` backend microservice.

---

## How to run the service using Service Manager

Service Manager details TBC below:

```
sm2 -start <SERVICE_NAME_TBC>
```
Run ```sm2 -s ``` to check what services are running

---

## Launching the service locally

To start the service locally:

> Note: this service runs on port 'TBC' by default

```
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

## Launching the service locally

Run the **sm2** command below to start all the services required for the nova-imports-notification-frontend.

```bash
sm2 -start <TBC_SERVICE>
```
Run the **sm2** command below to stop nova-imports-notification-frontend.

```bash
sm2 -stop <TBC_SERVICE>
```
Run the **sm2** command below to start nova-imports-notification-frontend service locally.
> Note: this service runs on port <TBC> by default

```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

---

## Testing

To run the unit tests:

```
sbt test
```

To run the unit tests with coverage report:

```
sbt clean coverage test it/test coverageReport
```

---

## Persistence

This service uses MongoDB to persist user answers.

---

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
