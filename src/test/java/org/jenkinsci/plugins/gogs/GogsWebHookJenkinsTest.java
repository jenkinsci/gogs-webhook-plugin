package org.jenkinsci.plugins.gogs;

import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

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

    @Test
    @Issue("SECURITY-1438")
    public void ensureTheSecretIsEncryptedOnDisk() throws Exception {
        Secret secret = Secret.fromString("s3cr3t");
        FreeStyleProject p = prepareProjectWithGogsProperty(secret);

        File configFile = p.getConfigFile().getFile();
        String configFileContent = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
        assertThat(configFileContent, not(containsString(secret.getPlainText())));
        assertThat(configFileContent, containsString(secret.getEncryptedValue()));
    }

    @Test
    @Issue("SECURITY-1438")
    public void ensureTheSecretIsEncryptedInHtml() throws Exception {
        Secret secret = Secret.fromString("s3cr3t");
        FreeStyleProject p = prepareProjectWithGogsProperty(secret);

        JenkinsRule.WebClient wc = j.createWebClient();
        // there are some errors in the page and thus the status is 500 but the content is there
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
        HtmlPage htmlPage = wc.goTo(p.getUrl() + "configure");
        String pageContent = htmlPage.getWebResponse().getContentAsString();
        assertThat(pageContent, not(containsString(secret.getPlainText())));
        assertThat(pageContent, containsString(secret.getEncryptedValue()));
    }

    private FreeStyleProject prepareProjectWithGogsProperty(Secret secret) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        GogsProjectProperty prop = new GogsProjectProperty(secret, true, "master");
        p.addProperty(prop);

        p.save();

        return p;
    }
}
