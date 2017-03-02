/*

The MIT License (MIT)
Copyright (c) 2016 Alexander Verhaar

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/

package org.jenkinsci.plugins.gogs;

import hudson.model.BuildableItem;
import hudson.model.Cause;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

import java.util.logging.Logger;

public class GogsPayloadProcessor {
  private static final Logger LOGGER = Logger.getLogger(GogsPayloadProcessor.class.getName());

  public GogsPayloadProcessor() {
  }

  public GogsResults triggerJobs(String jobName, String deliveryID) {
    SecurityContext saveCtx = null;
    GogsResults result = new GogsResults();

    try {
      saveCtx = SecurityContextHolder.getContext();

      Jenkins instance = Jenkins.getActiveInstance();
      if (instance!=null) {
        ACL acl = instance.getACL();
        acl.impersonate(ACL.SYSTEM);

        BuildableItem project = GogsUtils.find(jobName, BuildableItem.class);
        if (project != null) {
          Cause cause = new GogsCause(deliveryID);
          project.scheduleBuild(0, cause);
          result.setMessage(String.format("Job '%s' is executed",jobName));
        } else {
          String msg = String.format("Job '%s' is not defined in Jenkins",jobName);
          result.setStatus(404, msg);
          LOGGER.warning(msg);
        }
      }
    } catch (Exception e) {
    } finally {
        SecurityContextHolder.setContext(saveCtx);
    }

    return result;
  }
}
