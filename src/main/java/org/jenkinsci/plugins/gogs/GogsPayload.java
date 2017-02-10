package org.jenkinsci.plugins.gogs;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GogsPayload extends InvisibleAction implements EnvironmentContributingAction {
    private  Map<String,String> payload = new HashMap<String, String>();

    public GogsPayload(Map<String, String> payload) {
        this.payload = payload;
    }

    @Nonnull
    public Map<String, String> getPayload() {
        return payload;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
        final Map<String, String> payload = getPayload();
        LOGGER.log(Level.FINEST, "Injecting GOGS_PAYLOAD: {0}", payload);
        for(String key: payload.keySet()) {
            envVars.put("GOGS_"+key.toUpperCase(), this.payload.get(key));
        }
    }

    private static final Logger LOGGER = Logger.getLogger(GogsPayload.class.getName());
}