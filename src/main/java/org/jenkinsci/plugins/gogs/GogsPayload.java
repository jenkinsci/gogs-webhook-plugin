package org.jenkinsci.plugins.gogs;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

import java.util.logging.Level;
import java.util.logging.Logger;

class GogsPayload extends InvisibleAction implements EnvironmentContributingAction {
    private final GogsCause gogsCause;

    public GogsPayload(GogsCause gogsCause) {
        this.gogsCause = gogsCause;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
        LOGGER.log(Level.FINEST, "Injecting GOGS_PAYLOAD: {0}", gogsCause.getEnvVars());
        envVars.putAll(gogsCause.getEnvVars());
    }

    private static final Logger LOGGER = Logger.getLogger(GogsPayload.class.getName());
}