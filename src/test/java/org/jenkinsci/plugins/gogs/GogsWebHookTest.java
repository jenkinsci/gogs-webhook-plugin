package org.jenkinsci.plugins.gogs;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

        try {
            gogsWebHook.doIndex(null, null);
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
}
