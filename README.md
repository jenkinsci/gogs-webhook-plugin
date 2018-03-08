[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/gogs-webhook-plugin/master)](https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Fgogs-webhook-plugin/activity/)

Gogs-Webhook Plugin
===================

This plugin integrates [Gogs](https://gogs.io/) to Jenkins.<br>

In Gogs configure your webhook like this:<br>
http(s)://<< jenkins-server >>/gogs-webhook/?job=<< jobname >>

Example how your the webhook in Gogs should look like:
![Example webhook](https://github.com/jenkinsci/gogs-webhook-plugin/raw/master/bin/gogs-webhook-screenshot.png)

### About integration tests

This project has some integration tests available. For more details see the [dedicated readme](about_integration_tests.md).
