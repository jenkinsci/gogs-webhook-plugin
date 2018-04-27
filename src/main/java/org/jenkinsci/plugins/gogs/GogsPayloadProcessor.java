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
import jenkins.triggers.SCMTriggerItem;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

class GogsPayloadProcessor {
    private static final Logger LOGGER = Logger.getLogger(GogsPayloadProcessor.class.getName());
    private final Map<String, String> payload = new HashMap<>();

    GogsPayloadProcessor() {
    }

    public void setPayload(String k, String v) {
        this.payload.put(k, v);
    }

    public GogsResults triggerJobs(String jobName, String deliveryID) {
        AtomicBoolean jobdone = new AtomicBoolean(false);
        SecurityContext saveCtx = ACL.impersonate(ACL.SYSTEM);
        GogsResults result = new GogsResults();

        try {
            BuildableItem project = GogsUtils.find(jobName, BuildableItem.class);
            if (project != null) {
                Cause cause = new GogsCause(deliveryID);

                if (project instanceof ParameterizedJob) {
                    ParameterizedJob pJob = (ParameterizedJob) project;
                    pJob.getTriggers().values().stream()
                            .filter(trigger1 -> trigger1 instanceof GogsTrigger).findFirst()
                            .ifPresent((g) -> {
                                GogsPayload gogsPayload = new GogsPayload(this.payload);
                                Optional.ofNullable(SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(project))
                                        .ifPresent((item) -> {
                                            item.scheduleBuild2(0, gogsPayload);
                                            jobdone.set(true);
                                        });
                            });
                }
                if (!jobdone.get()) {
                    project.scheduleBuild(0, cause);
                    jobdone.set(true);
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.severe(sw.toString());
        } finally {
            if (jobdone.get()) {
                result.setMessage(String.format("Job '%s' is executed", jobName));
            } else {
                String msg = String.format("Job '%s' is not defined in Jenkins", jobName);
                result.setStatus(404, msg);
                LOGGER.warning(msg);
            }
            SecurityContextHolder.setContext(saveCtx);
        }

        return result;
    }
}