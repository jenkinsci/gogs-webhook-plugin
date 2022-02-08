package org.jenkinsci.plugins.gogs;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

/*
 *  Class to test gogs webhook in cooperation with other plugins
 */
public class GogsWebHookPluginsTest {
    private final String FOLDERNAME = "testFolder";
    private final String PROJECTNAME = "testProject";

    final Logger log = LoggerFactory.getLogger(GogsWebHookPluginsTest.class);

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testCloudBeesFolder() throws Exception {
        Folder folder = createFolder(FOLDERNAME);

        FreeStyleProject project = folder.createProject(FreeStyleProject.class, PROJECTNAME);

        Job job = GogsUtils.find(FOLDERNAME + "/" + PROJECTNAME, Job.class);
        assertThat("Couldn't find " + FOLDERNAME + "/" + PROJECTNAME, job, is(equalTo(project)));
    }

    //
    // Helper methods
    //
    private Folder createFolder(String folder) throws IOException {
        return r.jenkins.createProject(Folder.class, folder);
    }

}
