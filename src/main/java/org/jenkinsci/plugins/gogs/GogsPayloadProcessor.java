package org.jenkinsci.plugins.gogs;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;
import hudson.model.Job;
import hudson.model.Cause;
import hudson.security.ACL;
import hudson.model.AbstractProject;
import jenkins.model.Jenkins;
import hudson.model.Cause;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

public class GogsPayloadProcessor {
  private static final Logger LOGGER = Logger.getLogger(GogsPayloadProcessor.class.getName());

  public GogsPayloadProcessor() {
  }

  public GogsResults triggerJobs(String jobName, String deliveryID) {
    Boolean didJob = false;
    GogsResults result = new GogsResults();

    SecurityContext old = Jenkins.getInstance().getACL().impersonate(ACL.SYSTEM);
    for (AbstractProject<?,?> project : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
      if ( project.getName().equals(jobName)) {

        Cause cause = new GogsCause(deliveryID);
        project.scheduleBuild(0, cause);
        didJob = true;
        result.Message = String.format("Job '%s' is executed",jobName);
      }
    }
    if (!didJob) {
      result.Status = 404;
      result.Message = String.format("Job '%s' is not defined in Jenkins",jobName);
      LOGGER.warning(result.Message);
    }
    SecurityContextHolder.setContext(old);

    return result;
  }
}
