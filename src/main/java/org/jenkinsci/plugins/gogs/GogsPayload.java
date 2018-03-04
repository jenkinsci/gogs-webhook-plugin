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
    private final Map<String, String> payload;

    public GogsPayload(Map<String, String> payload) {
        this.payload = payload;
    }

    @Nonnull
    private Map<String, String> getPayload() {
        return payload;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
        LOGGER.log(Level.FINEST, "Injecting GOGS_PAYLOAD: {0}", getPayload());
        for (String key : payload.keySet()) {
            envVars.put("GOGS_" + key.toUpperCase(), this.payload.get(key));
        }
    }

    private static final Logger LOGGER = Logger.getLogger(GogsPayload.class.getName());
}