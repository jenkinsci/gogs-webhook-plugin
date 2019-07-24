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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.Secret;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Alexander Verhaar
 */
@Extension
public class GogsWebHook implements UnprotectedRootAction {
    private final static Logger LOGGER = Logger.getLogger(GogsWebHook.class.getName());
    private static final String DEFAULT_CHARSET = "UTF-8";
    private GogsResults result = new GogsResults();
    private String gogsDelivery = null;
    private String gogsSignature = null;
    private String jobName = null;
    private String xGogsEvent = null;
    static final String URLNAME = "gogs-webhook";

    public String getDisplayName() {
        return null;
    }

    public String getIconFileName() {
        return null;
    }

    public String getUrlName() {
        return URLNAME;
    }

    private String getGogsDelivery() {
        if (isNullOrEmpty(gogsDelivery)) {
            return "Triggered by Jenkins-Gogs-Plugin. Delivery ID unknown.";
        }
        return "Gogs-ID: " + gogsDelivery;
    }

    private void setGogsDelivery(String gogsDelivery) {
        this.gogsDelivery = gogsDelivery;
    }

    private String getGogsSignature() {
        if (isNullOrEmpty(gogsSignature)) {
            gogsSignature = null;
        }
        return gogsSignature;
    }

    private void setGogsSignature(String gogsSignature) {
        this.gogsSignature = gogsSignature;
    }

    /**
     * Receives the HTTP POST request send by Gogs.
     *
     * @param req request
     * @param rsp response
     * @throws IOException problem while parsing
     */
    @SuppressWarnings("WeakerAccess")
    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        JSONObject jsonObject = null;

        AtomicReference<String> jSecret = new AtomicReference<>(null);
        AtomicBoolean foundJob = new AtomicBoolean(false);
        AtomicBoolean isRefMatched = new AtomicBoolean(true);

        GogsPayloadProcessor payloadProcessor = new GogsPayloadProcessor();
        GogsCause gogsCause = new GogsCause();

        if (!sanityChecks(req, rsp)) {
            return;
        }

        // Get X-Gogs-Delivery header with deliveryID
        setGogsDelivery(req.getHeader("X-Gogs-Delivery"));

        // Get X-Gogs-Signature
        setGogsSignature(req.getHeader("X-Gogs-Signature"));

        // Get the POST stream
        String body = IOUtils.toString(req.getInputStream(), DEFAULT_CHARSET);
        try {
            jsonObject = JSONObject.fromObject(body);
        } catch (JSONException e) {
            result.setStatus(400, "Invalid JSON");
            exitWebHook(result, rsp);
            return;
        }
        String ref = jsonObject.optString("ref", null);

        if (xGogsEvent.equals("push") && req.getRequestURI().contains("/" + URLNAME + "/") && !body.isEmpty()) {
            JSONObject commits = (JSONObject) jsonObject.getJSONArray("commits").get(0);
            String message = commits.getString("message");

            if (message.startsWith("[IGNORE]")) {
                // Ignore commits starting with message "[IGNORE]"
                result.setStatus(200, "Ignoring push");
                exitWebHook(result, rsp);
                return;
            }

            String ref = jsonObject.getString("ref");
            LOGGER.fine("found ref " + ref);
            LOGGER.fine("found branch " + branchName);
            if (null != branchName && !StringUtils.containsIgnoreCase(ref, (String) branchName)) {
                // ignore all commit if they is not in context
                LOGGER.fine("build was rejected");
                result.setStatus(200, String.format("Commit is not relevant. Relevant context is %s", branchName));
                exitWebHook(result, rsp);
                return;
            }

            String contentType = req.getContentType();
            if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                body = URLDecoder.decode(body, DEFAULT_CHARSET);
            }
            if (body.startsWith("payload=")) {
                body = body.substring(8);
            }

            gogsCause.setGogsPayloadData(jsonObject.toString());
            gogsCause.setDeliveryID(getGogsDelivery());
            payloadProcessor.setCause(gogsCause);

            try (ACLContext ignored = ACL.as(ACL.SYSTEM)) {
                StringJoiner stringJoiner = new StringJoiner("%2F");
                Pattern.compile("/").splitAsStream(jsonObject.getString("ref")).skip(2).forEach(stringJoiner::add);
                String ref_strj = stringJoiner.toString();

                /* secret is stored in the properties of Job */
                Stream.of(jobName, jobName + "/" + ref_strj).map(j -> GogsUtils.find(j, Job.class)).filter(Objects::nonNull).forEach(job -> {
                    foundJob.set(true);
                    final GogsProjectProperty property = (GogsProjectProperty) job.getProperty(GogsProjectProperty.class);
                    if (property != null) { /* only if Gogs secret is defined on the job */
                        jSecret.set(Secret.toString(property.getGogsSecret())); /* Secret provided by Jenkins */
                        isRefMatched.set(property.filterBranch(ref));
                    }
                });
            }
        } else {
            result.setStatus(404, "No payload or URI contains invalid entries.");
        }

        // Gogs release event
        if (xGogsEvent.equals("release")) {
            try (ACLContext ignored = ACL.as(ACL.SYSTEM)) {
                /* secret is stored in the properties of Job */
                Stream.of(jobName).map(j -> GogsUtils.find(j, Job.class)).filter(Objects::nonNull).forEach(job -> {
                    foundJob.set(true);
                    final GogsProjectProperty property = (GogsProjectProperty) job.getProperty(GogsProjectProperty.class);
                    if (property != null) { /* only if Gogs secret is defined on the job */
                        jSecret.set(Secret.toString(property.getGogsSecret())); /* Secret provided by Jenkins */
                    }
                });
            }
        }

        if (result.getStatus() == 200) {
            String gSecret = null;
            if (getGogsSignature() == null) {
                gSecret = jsonObject.optString("secret", null);  /* Secret provided by Gogs < 0.10.x   */
            } else {
                try {
                    if (getGogsSignature().equals(GogsUtils.encode(body, jSecret.get()))) {
                        gSecret = jSecret.get();
                        // now hex is right, continue to old logic
                    }
                } catch (Exception e) {
                    LOGGER.warning(e.getMessage());
                }
            }

            if (!foundJob.get()) {
                String msg = String.format("Job '%s' is not defined in Jenkins", jobName);
                result.setStatus(404, msg);
                LOGGER.warning(msg);
            } else if (!isRefMatched.get()) {
                String msg = String.format("received ref ('%s') is not matched with branch filter in job '%s'", ref, jobName);
                result.setStatus(200, msg);
                LOGGER.info(msg);
            } else if (isNullOrEmpty(jSecret.get()) && isNullOrEmpty(gSecret)) {
                /* No password is set in Jenkins and Gogs, run without secrets */
                result = payloadProcessor.triggerJobs(jobName);
            } else if (!isNullOrEmpty(jSecret.get()) && jSecret.get().equals(gSecret)) {
                /* Password is set in Jenkins and Gogs, and is correct */
                result = payloadProcessor.triggerJobs(jobName);
            } else {
                /* Gogs and Jenkins secrets differs */
                result.setStatus(403, "Incorrect secret");
            }
        }

        exitWebHook(result, rsp);
    }

    /***
     * Do sanity checks
     *
     * @param req Request
     * @param rsp Response
     * @throws IOException Exception
     */
    private boolean sanityChecks(StaplerRequest req, StaplerResponse rsp) throws IOException {
        //Check that we have something to process
        checkNotNull(req, "Null request submitted to doIndex method");
        checkNotNull(rsp, "Null reply submitted to doIndex method");

        // Get X-Gogs-Event
        xGogsEvent = req.getHeader("X-Gogs-Event");
        if (!"push".equals(xGogsEvent) && !"release".equals(xGogsEvent)) {
            result.setStatus(403, "Only push or release events are accepted.");
            exitWebHook(result, rsp);
            return false;
        }

        // Get queryStringMap from the URI
        String queryString = checkNotNull(req.getQueryString(), "The queryString in the request is null");
        Map queryStringMap = checkNotNull(GogsUtils.splitQuery(queryString), "Null queryStringMap");

        //Do we have the job name parameter ?
        if (!queryStringMap.containsKey("job")) {
            result.setStatus(404, "Parameter 'job' is missing.");
            exitWebHook(result, rsp);
            return false;
        }

        jobName = queryStringMap.get("job").toString();
        if (isNullOrEmpty(jobName)) {
            result.setStatus(404, "No value assigned to parameter 'job'");
            exitWebHook(result, rsp);
            return false;
        }
        return true;
    }

    /**
     * Exit the WebHook
     *
     * @param result GogsResults
     */
    private void exitWebHook(GogsResults result, StaplerResponse resp) throws IOException {
        if (result.getStatus() != 200) {
            LOGGER.warning(result.getMessage());
        }
        //noinspection MismatchedQueryAndUpdateOfCollection
        JSONObject json = new JSONObject();
        json.element("result", result.getStatus() == 200 ? "OK" : "ERROR");
        json.element("message", result.getMessage());
        resp.setStatus(result.getStatus());
        resp.addHeader("Content-Type", "application/json");
        PrintWriter printer = resp.getWriter();
        printer.print(json.toString());
    }
}

// vim: set ts=4 sw=4 tw=0 ft=java et :
