package org.jenkinsci.plugins.gogs;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.CoreEnvironmentContributor;

import javax.annotation.Nonnull;
import java.io.IOException;

@Extension
public class GogsEnvironmentContributor extends CoreEnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {
        GogsCause gogsCause;

        gogsCause = (GogsCause) r.getCause(GogsCause.class);
        if (gogsCause != null) {
            envs.putAll(gogsCause.getEnvVars());
        }
    }
}
