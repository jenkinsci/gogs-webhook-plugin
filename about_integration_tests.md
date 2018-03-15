# About GOGS webhooks integration tests

## Introduction

End-to-end Webhook integration tests are available during the Maven `verify` phase.

For a detailed explanation of how it works refer to the [Integration Test Internals](#integration-test-internals) section. 
 
The `verify` phase will start a local docker environment composed of a Jenkins and a Gogs server during the Maven `pre-integration-test` phase. 
The plugin under development as  well as all necessary plugins are pre-loaded in the docker container. No security is set on the Jenkins server. The Gogs server 
is configured with a user called "butler" and password "butler". The credentials are pre-loaded 
in the Jenkins server.

The tests, running with Failsafe, are located in the `Webhook_IT.java` class.

A single smoke test is currently available.

The test environment is torn down during the Maven`post-integration-test` phase. Note that all traces of the docker environment are removed, 
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
- A windows dev environment has not been tested (and will probably not work as it misses bash support).
- Others I might not be aware of (sorry about that)...

(#integration-test-internals)
## Integration test internals

The Integration test use a Docker environment that is started with a bash script. This bash script is integrated in Maven via the exec plugin. 
The reason for that choice is discussed in [following article](https://www.the-captains-shack.com/post/MavenAndDocker/).

### Environment setup

The environment needed to perform the Integration Tests is described and managed by a Docker-Compose file. It is located in the `src/test/docker` directory. 
In that same directory are the Dockerfiles that describe the two needed entities: the Jenkins server and the Gogs server.

Note that either for Jenkins and Gogs, the test is based on the LATEST version (as well as for the Jenkins plugins).

#### The Jenkins Docker Image

The Jenkins container image is built from the `jenkinsci/jenkins:latest` image. The necessary dependencies for the Gogs Webhook plugin under test are installed using the built-in `install-plugins.sh` script (git, workflow-aggregator, pipeline-model-extensions, cloudbees-folder).
The previously built plugin hpi is copied in the container so that it will automatically be loaded at startup.
The password credential for Gogs is loaded in the Jenkins container with the `setup-gogs-user-credentials.groovy.override` groovy script (executed at startup).
It is hardcoded in the script to user "butler" with password "butler". 

The jenkins container is started via the Docker-compose file. With environment variables, Jenkins is configured to listen to a paricular port and not to stat the iinstall wizard.
With the Docker port command, the Jenkins port is published so that it is available on machine level (for debugging for exemple).
The current version of the Compose file will lead to port collision if several webhook integration tests are fired at the same time.

Note that the container is given a fixed name that can also lead to collisions.

For speed, the JENKINS_HOME is stored in an unamed data volume (that is destroyed at tear down).

#### The Gogs Docker Image

The Gogs container image is built on `gogs/gogs:0.11.4`. Note that this version is pinned as it is the Webhook (and underlaying Jenkins that is under test).

Several configuration scripts are loaded in the image for execution during build time.

* `app.ini` the static settings of the Gogs server
* `setup-gogs.sh` the dynamic values or operations (ex setting up users or preloading a repository). It uses the REST API to perform these actions.
* `repos-to-mirror` the path to the repository to preload in the Gogs container.

The setup script is than executed (after changing the base URL in the app.ini)

#### Starting the images with Docker-compose

The docker-compose and its parameters are controlled by the scripts located in `src/test/scripts`.

To start the environment, the script `start-test-environmnent.sh` first calls the docker-compose "build" command so that the images are recreated. The "--no-cache" parameter is to avoid the reuse of cached artifacts. 
The "--pull" parameter is to always attempt to pull a newer version of the image.

It then starts the environment with the classical "up -d" parameters (start as daemon) but also specifying the "--force-recreate" switch to avoid the reuse of an already existing container. 

These precautions are taken to be sure that we always start with a clean and equivalent environment.

The environment is torn down with the `stop-test-environment.sh` script. It first stops all containers ("stop") and than removes the dead containers and associated data volumes.

Note: this automatic cleanup will destroy all logfiles available in the containers. 

### Running the integration tests interactively

This is a use case where you want to understand why a test is failing and want to use your debugger on the Integration test.

As the Integration tests rely on a properly compiled plugin release candidate, the easiest is to run the Maven lifecycle up to the environment setup.
This will take care of compiling it, running the unit test, copy the resulting compiled plugin (hpi) to the appropriate directory so that it gets properly included in the new Jenkins test image.
To achieve this just use `mvn clean pre-integration-test -P withIntegrationTest`. 
The test can be run with the maven command `mvn failsafe:integration-test  -P withIntegrationTest` or with your IDE. 

To tear down the docker environment execute `src/test/scripts/stop-test-environment.sh` from the project's root.  

To connect to the servers when the environment is up, connect to `http://localhost:8080` for the Jenkins server (no password) and to 
 `http://localhost:3000` for the Gogs server. The user is "butler" with password "butler".

---

### Test workflow

This section describes how the smoke-test (happy case) is constructed. From this structure, other test cases can be built.


### Step 1: Setting up the Gogs server

The first part is dealing with the setup of the Gogs server. For that, it uses a "handler" class that hides the complexity of the various REST commands.
This class is instantiated with the Gogs URL, the User and Password to use to issue the command.
The first command used is the waitForServer that will try for a minute, every 5 seconds, to establish a contact with the server. If that period expires without a contact, the test fails.

The "createEmptyRepo" method is then used to create a blank repository on the Gogs server.

We then create and configure a local test git repository under "`target/test-repos`" by using the java git API. The files for that repository are copied there during 
the Integration Test setup from the "`src/test/test-repos`" directory. This configuration encompasses the setup of the Origin remote server.

Note that the URL is relative to the machine launching the Integration tests. Thus localhost in most cases.
 
The files are added to the local git repository and then pushed to the Gogs server.
Using this technique will help to setup a repository with branches to automate testing of complex multi-branch setups.


### Step 2: Setting up the Jenkins server
 
After establishing the contact with the Jenkins server and waiting for it to be fully operational, we setup a test job.
The Job definition XML is stored in the `src/test/resources/Jenkins-config` directory. Note that this is a very simple pipeline job defined in a Jenkinsfile.

We then check whether everything is properly working. 
Using the Jenkins java API, we get the next build number, launch the build and check that the resulting build number is the one we expected.

The build process created a marker file with the git reference of what it built. This marker file is archived during the build and 
retrieved to compare the local git commit reference with the one that has been built.

If these two validations succeed, this means that Gogs and Jenkins are ready to perform the actual hook test.


### Step 3: Setting up and using the webhook

We then setup the hook in gogs by using the definitions stored in `src/test/resources/`. Note that the URL for Jenkins is Docker-compose defined one (`http://jenkins:8080/job/test%20project/build?delay=0`).

We are now ready to try if a new commit pushed to the Gogs repository will trigger a build in the correct job. For that, we store the next expected build number.

A file in the local git repository is modified, committed and pushed to the Gogs repository. The git commit ID is stored validate later that the correct commit was built.

We then wait for a preset time for the build to complete (the "last build number" should match the "next build number" before the commit to Gogs).

If completed successfully, we assert with the archived maker file that the git commit ids match as we did earlier.

This demonstrates that a push to the Gogs repository (master branch) correctly triggered a build of the expected commit.
