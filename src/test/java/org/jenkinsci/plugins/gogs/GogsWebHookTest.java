package org.jenkinsci.plugins.gogs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.function.Executable;
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
    public void callDoIndexWithNullReqMessageMustThrowException() throws IOException {
        GogsWebHook gogsWebHook = new GogsWebHook();
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);

        final Executable testMethod = () -> {
            gogsWebHook.internalDoIndex(new GogsResults(), null, staplerResponse);
        };
        final NullPointerException thrown = assertThrows(NullPointerException.class, testMethod);
        assertThat("Not the expected error message.", thrown.getMessage(), is(equalTo("Null request submitted to doIndex method")));
    }

    @Test
    public void callDoIndexWithNullResponseMessageMustThrowException() throws IOException {
        GogsWebHook gogsWebHook = new GogsWebHook();
        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);

        final Executable testMethod = () -> {
            gogsWebHook.internalDoIndex(new GogsResults(), staplerRequest, null);
        };
        final NullPointerException thrown = assertThrows(NullPointerException.class, testMethod);
        assertThat("Not the expected error message.", thrown.getMessage(), is(equalTo("Null reply submitted to doIndex method")));
    }

    @Test
    public void whenEmptyHeaderTypeMustReturnError() throws Exception {
        //Prepare the SUT
        File uniqueFile = File.createTempFile("webHookTest_", ".txt", new File("target"));

        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);

        //perform the test
        performDoIndexTest(staplerRequest, staplerResponse, uniqueFile);

        //validate that everything was done as planed
        verify(staplerResponse).setStatus(403);

        String expectedOutput = "Only push event can be accepted.";
        isExpectedOutput(uniqueFile, expectedOutput);

        log.info("Test succeeded.");
    }

    @Test
    public void whenWrongHeaderTypeMustReturnError() throws Exception {
        //Prepare the SUT
        File uniqueFile = File.createTempFile("webHookTest_", ".txt", new File("target"));

        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);
        when(staplerRequest.getHeader("X-Gogs-Event")).thenReturn("junk");

        //perform the testÎ
        performDoIndexTest(staplerRequest, staplerResponse, uniqueFile);

        //validate that everything was done as planed
        verify(staplerResponse).setStatus(403);

        String expectedOutput = "Only push event can be accepted.";
        isExpectedOutput(uniqueFile, expectedOutput);

        log.info("Test succeeded.");
    }


    @Test
    public void whenQueryStringIsNullMustThrowException() throws Exception {
        //Prepare the SUT
        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);
        when(staplerRequest.getHeader("X-Gogs-Event")).thenReturn("push");
        when(staplerRequest.getQueryString()).thenReturn(null);
        GogsWebHook gogsWebHook = new GogsWebHook();

        final Executable testMethod = () -> {
            gogsWebHook.internalDoIndex(new GogsResults(), staplerRequest, staplerResponse);
        };
        final NullPointerException thrown = assertThrows(NullPointerException.class, testMethod);
        assertThat("Not the expected error message.", thrown.getMessage(), is(equalTo("The queryString in the request is null")));
    }


    @Test
    public void whenNoJobInQueryStringMustReturnError() throws Exception {
        //Prepare the SUT
        File uniqueFile = File.createTempFile("webHookTest_", ".txt", new File("target"));

        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);
        when(staplerRequest.getHeader("X-Gogs-Event")).thenReturn("push");
        when(staplerRequest.getQueryString()).thenReturn("foo=bar&blaah=blaah");

        //perform the testÎ
        performDoIndexTest(staplerRequest, staplerResponse, uniqueFile);

        //validate that everything was done as planed
        verify(staplerResponse).setStatus(404);

        String expectedOutput = "Parameter 'job' is missing.";
        isExpectedOutput(uniqueFile, expectedOutput);

        log.info("Test succeeded.");
    }

    @Test
    public void whenEmptyJobInQueryStringMustReturnError() throws Exception {
        //Prepare the SUT
        File uniqueFile = File.createTempFile("webHookTest_", ".txt", new File("target"));

        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);
        when(staplerRequest.getHeader("X-Gogs-Event")).thenReturn("push");
        when(staplerRequest.getQueryString()).thenReturn("job&foo=bar");

        //perform the testÎ
        performDoIndexTest(staplerRequest, staplerResponse, uniqueFile);

        //validate that everything was done as planed
        verify(staplerResponse).setStatus(404);

        String expectedOutput = "No value assigned to parameter 'job'";
        isExpectedOutput(uniqueFile, expectedOutput);

        log.info("Test succeeded.");
    }

    @Test
    public void whenEmptyJob2InQueryStringMustReturnError() throws Exception {
        //Prepare the SUT
        File uniqueFile = File.createTempFile("webHookTest_", ".txt", new File("target"));

        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);
        when(staplerRequest.getHeader("X-Gogs-Event")).thenReturn("push");
        when(staplerRequest.getQueryString()).thenReturn("job=&foo=bar");

        //perform the testÎ
        performDoIndexTest(staplerRequest, staplerResponse, uniqueFile);

        //validate that everything was done as planed
        verify(staplerResponse).setStatus(404);

        String expectedOutput = "No value assigned to parameter 'job'";
        isExpectedOutput(uniqueFile, expectedOutput);

        log.info("Test succeeded.");
    }

    @Test
    public void whenUriDoesNotContainUrlNameMustReturnError() throws Exception {
        //Prepare the SUT
        File uniqueFile = File.createTempFile("webHookTest_", ".txt", new File("target"));

        StaplerRequest staplerRequest = Mockito.mock(RequestImpl.class);
        StaplerResponse staplerResponse = Mockito.mock(ResponseImpl.class);
        when(staplerRequest.getHeader("X-Gogs-Event")).thenReturn("push");
        when(staplerRequest.getQueryString()).thenReturn("job=myJob");


        MockServletInputStream inputStream = new MockServletInputStream("body");
        when(staplerRequest.getInputStream()).thenReturn(inputStream);
        when(staplerRequest.getRequestURI()).thenReturn("/badUri/aaa");

        //perform the testÎ
        performDoIndexTest(staplerRequest, staplerResponse, uniqueFile);

        //validate that everything was done as planed
        verify(staplerResponse).setStatus(404);

        String expectedOutput = "No payload or URI contains invalid entries.";
        isExpectedOutput(uniqueFile, expectedOutput);

        log.info("Test succeeded.");
    }

    //
    // Helper methods
    //


    private void performDoIndexTest(StaplerRequest staplerRequest, StaplerResponse staplerResponse, File file) throws IOException {
        PrintWriter printWriter = new PrintWriter(file.getAbsoluteFile(), "UTF-8");
        when(staplerResponse.getWriter()).thenReturn(printWriter);

        GogsWebHook gogsWebHook = new GogsWebHook();

        //Call the method under test
        gogsWebHook.doIndex(staplerRequest, staplerResponse);

        //Save the Jason log file so we can check it
        printWriter.flush();
        printWriter.close();
    }

    private void isExpectedOutput(File uniqueFile, String expectedOutput) throws IOException {
        String output = FileUtils.readFileToString(uniqueFile, "utf-8");
        uniqueFile.delete();
        String completeExpectedOutput = "{\"result\":\"ERROR\",\"message\":\"" + expectedOutput + "\"}";
        assertThat("Not the expected output file content", output, is(equalTo(completeExpectedOutput)));
    }


    class MockServletInputStream extends ServletInputStream {
        InputStream inputStream;

        MockServletInputStream(String string) {
            this.inputStream = IOUtils.toInputStream(string);
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }
    }
}
