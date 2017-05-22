# About GOGS webhooks integration tests

End-to-end Webhook integration tests are available during the Maven `verify` phase.
 
The `verify` phase will start a local docker environment composed of a Jenkins and a Gogs server during the `pre-integration-test`. 
The plugin under development as  well as all necessary plugins are pre-loaded in the docker container. No security is set on the Jenkins server. The Gogs server 
is configured with a user called "butler" and password "butler". The credentials are pre-loaded 
in the Jenkins server.

The tests, running with Failsafe, are located in the `Webhook_IT.java` class.

A single smoke test is currently available.

The test environment is torn down during the `post-integration-test`. Note that all traces of the docker environment are removed, 
in particular the data volumes used by both servers. 

As the Integration tests are still experimental, they are grouped under a dedicated profile called `withIntegrationTest`. 
To compile the project with the integration tests execute `mvn clean install -P withIntegrationTest`.

To setup an environment in order to debug the integration tests, just use `mvn clean pre-integration-test -P withIntegrationTest`. 
The test can be run with with maven with `mvn failsafe:integration-test  -P withIntegrationTest`. To tear down the docker environment execute
`src/test/scripts/stop-test-environment.sh` from the project's root.  

To connect to the servers when the environment is up, connect to `http://localhost:8080` for the Jenkins server (no password) and to 
 `http://localhost:3000` for the Gogs server. The user is "butler" with password "butler".

### Warning

- does not check for port collision (ports used by an other application)
- is not jenkins ready (see above)
- Smoke test should be refactored to allow a re-use of test setup

### Pre-requisite

- Docker must be pre-installed on the machine running the tests.
- The test machine must have Internet access (to download the Docker images and Jenkins dependencies).
- A windows dev environment has not been tested.
- Others I might not be aware of (sorry about that)...