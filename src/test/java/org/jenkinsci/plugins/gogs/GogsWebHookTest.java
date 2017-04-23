package org.jenkinsci.plugins.gogs;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.ResponseImpl;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GogsWebHookTest {
    final Logger log = LoggerFactory.getLogger(GogsWebHookTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            GogsWebHookTest.this.log.info("\t **************  Start Test ({})  *******************", description
                    .getMethodName());
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void callDoIndexWithNullReqMessageMustFail() throws IOException {
        GogsWebHook gogsWebHook = new GogsWebHook();
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);
        try {
            gogsWebHook.doIndex(null, staplerResponse);
        } catch (NullPointerException e) {
            String expectedErrMsg = "Null request submitted to doIndex method";
            assertEquals("Not the expected error message.", expectedErrMsg, e.getMessage());
            log.info("call failed as expected.");
            return;
        }
        fail("The call should have failed.");
    }

    @Test
    public void callDoIndexWithNullResponseMessageMustFail() throws IOException {
        GogsWebHook gogsWebHook = new GogsWebHook();
        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        try {
            gogsWebHook.doIndex(staplerRequest, null);
        } catch (NullPointerException e) {
            String expectedErrMsg = "Null reply submitted to doIndex method";
            assertEquals("Not the expected error message.", expectedErrMsg, e.getMessage());
            log.info("call failed as expected.");
            return;
        }
        fail("The call should have failed.");
    }

    @Test
    public void whenEmptyHeaderTypeMustReturnError() throws Exception {
        //Prepare the SUT
        File uniqueFile = File.createTempFile("webHookTest_", ".txt", new File("target"));
        String uniqueFileName = uniqueFile.getAbsolutePath();

        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);

        //perform the test
        performDoIndexTest(staplerRequest, staplerResponse,uniqueFileName);

        //validate that everything was done as planed
        verify(staplerResponse).setStatus(403);

        String output = FileUtils.readFileToString(uniqueFile, "utf-8");
        uniqueFile.delete();
        String expectedOutput = "{\"result\":\"ERROR\",\"message\":\"Only push event can be accepted.\"}";
        assertEquals("Not the expected output file content",expectedOutput,output);

        log.info("Test succeeded.");
    }

    @Test
    public void whenWrongHeaderTypeMustReturnError() throws Exception {
        //Prepare the SUT
        File uniqueFile = File.createTempFile("webHookTest_", ".txt", new File("target"));
        String uniqueFileName = uniqueFile.getAbsolutePath();

        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);
        when(staplerRequest.getHeader("X-Gogs-Event")).thenReturn("junk");

        //perform the testÎ
        performDoIndexTest(staplerRequest, staplerResponse, uniqueFileName);

        //validate that everything was done as planed
        verify(staplerResponse).setStatus(403);

        String output = FileUtils.readFileToString(uniqueFile, "utf-8");
        uniqueFile.delete();
        String expectedOutput = "{\"result\":\"ERROR\",\"message\":\"Only push event can be accepted.\"}";
        assertEquals("Not the expected output file content",expectedOutput,output);

        log.info("Test succeeded.");
    }

    private void performDoIndexTest(StaplerRequest staplerRequest, StaplerResponse staplerResponse, String fileName) throws IOException {
        PrintWriter printWriter = new PrintWriter(fileName, "UTF-8");
        when(staplerResponse.getWriter()).thenReturn(printWriter);

        GogsWebHook gogsWebHook = new GogsWebHook();

        //Call the method under test
        gogsWebHook.doIndex(staplerRequest, staplerResponse);

        //Save the Jason log file so we can check it
        printWriter.flush();
        printWriter.close();
    }
}
