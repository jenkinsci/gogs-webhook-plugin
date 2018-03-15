FROM jenkinsci/jenkins:latest

# Load the required dependencies for the plugin under test
RUN /usr/local/bin/install-plugins.sh git workflow-aggregator pipeline-model-extensions cloudbees-folder

# Copy the newly built plugin in the container so that it gets loaded at startup
ADD gogs-webhook.hpi /usr/share/jenkins/ref/plugins/gogs-webhook.hpi

# Copy the groovy script used to setup the Gogs password to a place were it will be executed at startup
ADD setup-gogs-user-credentials.groovy /usr/share/jenkins/ref/init.groovy.d/setup-gogs-user-credentials.groovy.override