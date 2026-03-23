# Nova Imports Notification Frontend

## Service Overview

This is a frontend microservice for handling vehicle import notifications to HMRC.

This service connects to the `nova-imports` backend microservice.

---

## Running the service
Using Service Manager, **sm2** uses the **NOVA_IMPORTS_ALL** profile to start all services with the latest tagged releases.

```bash
sm2 --start NOVA_IMPORTS_ALL
```
Run ```sm2 -s``` to check what services are running.

## Launching the service locally

Run the **sm2** command below to start all the services required for the NoVA frontend service.

```bash
sm2 --start NOVA_IMPORTS_ALL
```
Run the **sm2** command below to stop the NoVA frontend service.

```bash
sm2 --stop NOVA_IMPORTS_NOTIFICATION_FRONTEND
```
Run the command below to start the NoVA frontend service locally.
> Note: this service runs on port 10300 by default

```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

## Testing locally

1. Go to `http://localhost:9949/auth-login-stub/gg-sign-in`
2. Set the **Redirect URL** to `http://localhost:10300/nova-imports/<page-path>`
3. Set the **Affinity Group** to `Individual`, `Organisation`, or `Agent`
4. Click **Submit** — you will be authenticated and redirected to the service

### Editing session data

Most pages require session data (user answers) to be present. A test-only session editor is available when running with test-only routes:

1. Log in via the auth-login-stub with the **Redirect URL** set to `http://localhost:10300/nova-imports/test-only/session`
2. Enter session data as JSON (e.g. `{}` for an empty session) and click **Save**
3. Navigate to the page you want to test — session data will now be available

***

### Running the test suite

```bash
sbt clean coverage test it/test coverageReport
```
This will also generate a coverage report.


## Persistence

This service uses MongoDB to persist user answers.

---

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
