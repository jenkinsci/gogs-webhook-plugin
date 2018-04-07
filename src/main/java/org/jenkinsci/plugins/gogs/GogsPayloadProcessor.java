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
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

class GogsPayloadProcessor {
    private static final Logger LOGGER = Logger.getLogger(GogsPayloadProcessor.class.getName());
    private final Map<String, String> payload = new HashMap<>();

    GogsPayloadProcessor() {
    }

    public void setPayload(String k, String v) {
        this.payload.put(k, v);
    }

    public GogsResults triggerJobs(String jobName, String deliveryID) {
        SecurityContext saveCtx = ACL.impersonate(ACL.SYSTEM);
        GogsResults result = new GogsResults();

        try {
            BuildableItem project = GogsUtils.find(jobName, BuildableItem.class);
            if (project != null) {
                GogsTrigger gTrigger = null;
                Cause cause = new GogsCause(deliveryID);

                if (project instanceof ParameterizedJobMixIn.ParameterizedJob) {
                    ParameterizedJobMixIn.ParameterizedJob pJob = (ParameterizedJobMixIn.ParameterizedJob) project;
                    gTrigger = (GogsTrigger) pJob.getTriggers().values().stream()
                            .filter(trigger1 -> trigger1 instanceof GogsTrigger).findFirst().get();
                }

                if (gTrigger != null) {
                    SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(project);
                    GogsPayload gogsPayload = new GogsPayload(this.payload);
                    if (item != null) {
                        item.scheduleBuild2(0, gogsPayload);
                    }
                } else {
                    project.scheduleBuild(0, cause);
                }
                result.setMessage(String.format("Job '%s' is executed", jobName));
            } else {
                String msg = String.format("Job '%s' is not defined in Jenkins", jobName);
                result.setStatus(404, msg);
                LOGGER.warning(msg);
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.severe(sw.toString());
        } finally {
            SecurityContextHolder.setContext(saveCtx);
        }

        return result;
    }
}