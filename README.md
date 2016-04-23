Gogs-Webhook Plugin
===================

This plugin integrates [Gogs](https://gogs.io/) to Jenkins.
Download the plugin [here](https://github.com/sanderv32/gogs-webhook-plugin/raw/master/bin/gogs-webhook.hpi).

In Gogs configure your webhook like this:
http(s)://<< jenkins-server >>/gogs-webhook/?job=<< jobname >>

Example how your the webhook in Gogs should look like:
![Example webhook](https://raw.githubusercontent.com/sanderv32/gogs-webhook-plugin/master/bin/gogs-webhook-screenshot.png)

#### TODO:
- Implement Gogs secret
