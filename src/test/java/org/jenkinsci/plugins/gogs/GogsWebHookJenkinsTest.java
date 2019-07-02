package org.jenkinsci.plugins.gogs;

import hudson.util.Secret;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertSame;

public class GogsWebHookJenkinsTest {
    final Logger log = LoggerFactory.getLogger(GogsWebHookJenkinsTest.class);

    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Test
    public void whenJobBranchNotMatchMustReturnError() {
        Secret secret = Secret.fromString(null);
        String[][] test_vals = {
            {null, "master", "true"},
            {null, "dev", "true"},
            {"", "master", "true"},
            {"", "dev", "true"},
            {"*", "master", "true"},
            {"*", "dev", "true"},
            {"dev", "master", "false"},
            {"dev", "dev", "true"},
            {"master", "master", "true"},
            {"master", "dev", "false"},
        };

        for (int i = 0; i < test_vals.length; ++i) {
            String filter = test_vals[i][0];
            String ref = test_vals[i][1];
            boolean ret = Boolean.parseBoolean(test_vals[i][2]);

            GogsProjectProperty property = new GogsProjectProperty(secret, false, filter);
            assertSame(String.format("branch filter check failed for [%s -> %s]", ref, filter), ret, property.filterBranch(ref));
        }
        
        log.info("Test succeeded.");
    }
}
