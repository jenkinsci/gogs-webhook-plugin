FROM jenkinsci/jenkins:latest

# Load the required dependencies for the plugin under test
RUN /usr/local/bin/install-plugins.sh git workflow-aggregator

# Copy the newly built plugin in the container so that it gets loaded at startup
ADD gogs-webhook.hpi /usr/share/jenkins/ref/plugins/gogs-webhook.hpi