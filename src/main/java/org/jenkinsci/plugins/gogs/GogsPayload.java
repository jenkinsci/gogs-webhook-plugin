package org.jenkinsci.plugins.gogs;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class GogsPayload extends InvisibleAction implements EnvironmentContributingAction {
    private Map<String, String> payload;
    private GogsCause gogsCause;

    public GogsPayload(Map<String, String> payload) {
        this.payload = payload;
    }

    public GogsPayload(GogsCause gogsCause) {
        this.gogsCause = gogsCause;
    }

    @Nonnull
    public Map<String, String> getPayload() {
        return payload;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
        LOGGER.log(Level.FINEST, "Injecting GOGS_PAYLOAD: {0}", getPayload());
//        payload.forEach((key, value) -> envVars.put("GOGS_" + key.toUpperCase(), value));
        envVars.putAll(gogsCause.getEnvVars());
    }

    private static final Logger LOGGER = Logger.getLogger(GogsPayload.class.getName());
}