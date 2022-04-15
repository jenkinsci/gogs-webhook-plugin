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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.codec.binary.Hex;
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
    static final String URLNAME = "gogs-webhook";
    private static final String DEFAULT_CHARSET = "UTF-8";

    public String getDisplayName() {
        return null;
    }

    public String getIconFileName() {
        return null;
    }

    public String getUrlName() {
        return URLNAME;
    }

    /**
     * encode sha256 hmac
     *
     * @param data data to hex
     * @param key  key of HmacSHA256
     * @return a String with the encoded sha256 hmac
     * @throws Exception Something went wrong getting the sha256 hmac
     */
    private static String encode(String data, String key) throws Exception {
        final Charset asciiCs = Charset.forName("UTF-8");
        final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        final SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(asciiCs.encode(key).array(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
    }

    /**
     * Receives the HTTP POST request send by Gogs.
     *
     * @param req request
     * @param rsp response
     * @throws IOException problem while parsing
     */
    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {

        GogsResults result = new GogsResults();

        try {
            internalDoIndex(result, req, rsp);

        } catch (final RuntimeException re) {
            LOGGER.severe(re.toString());
            result.setStatus(500, "GogsWebHook execution error.");
            exitWebHook(result, rsp);
            return;
        }
    }

    void internalDoIndex(GogsResults result, StaplerRequest req, StaplerResponse rsp) throws IOException {

        GogsPayloadProcessor payloadProcessor = new GogsPayloadProcessor();

        //Check that we have something to process
        checkNotNull(req, "Null request submitted to doIndex method");
        checkNotNull(rsp, "Null reply submitted to doIndex method");

        // Get X-Gogs-Event
        String event = req.getHeader("X-Gogs-Event");
        if (!"push".equals(event) && !"release".equals(event)) {
            result.setStatus(403, "Only push or release events are accepted.");
            exitWebHook(result, rsp);
            return;
        }

        // Get X-Gogs-Delivery header with deliveryID
        String gogsDelivery = req.getHeader("X-Gogs-Delivery");
        if (gogsDelivery == null || gogsDelivery.isEmpty()) {
            gogsDelivery = "Triggered by Jenkins-Gogs-Plugin. Delivery ID unknown.";
        } else {
            gogsDelivery = "Gogs-ID: " + gogsDelivery;
        }

        // Get X-Gogs-Signature
        String gogsSignature = req.getHeader("X-Gogs-Signature");
        if (gogsSignature == null || gogsSignature.isEmpty()) {
            gogsSignature = null;
        }


        // Get queryStringMap from the URI
        String queryString = checkNotNull(req.getQueryString(), "The queryString in the request is null");
        Map queryStringMap = checkNotNull(splitQuery(queryString), "Null queryStringMap");

        //Do we have the job name parameter ?
        if (!queryStringMap.containsKey("job")) {
            result.setStatus(404, "Parameter 'job' is missing.");
            exitWebHook(result, rsp);
            return;
        }
        Object jobObject = queryStringMap.get("job");
        String jobName;
        if (jobObject == null) {
            result.setStatus(404, "No value assigned to parameter 'job'");
            exitWebHook(result, rsp);
            return;
        } else {
            jobName = jobObject.toString();
        }

        final Object branchName = queryStringMap.get("branch");

        // Get the POST stream
        String body = IOUtils.toString(req.getInputStream(), DEFAULT_CHARSET);
        if (!body.isEmpty() && req.getRequestURI().contains("/" + URLNAME + "/")) {
            JSONObject jsonObject = JSONObject.fromObject(body);
            JSONObject commits = (JSONObject) jsonObject.getJSONArray("commits").get(0);
            String message = (String) commits.get("message");

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

            String jSecret = null;
            boolean foundJob = false;

            // filter branch, if ref not match branch filter, skip trigger job.
            boolean isRefMatched = true;


            payloadProcessor.setPayload("ref", ref);
            payloadProcessor.setPayload("before", jsonObject.getString("before"));

            SecurityContext saveCtx = ACL.impersonate(ACL.SYSTEM);

            try {
                Job job = GogsUtils.find(jobName, Job.class);

                if (job != null) {
                    foundJob = true;
                    /* secret is stored in the properties of Job */
                    final GogsProjectProperty property = (GogsProjectProperty) job.getProperty(GogsProjectProperty.class);
                    if (property != null) { /* only if Gogs secret is defined on the job */
                        jSecret = Secret.toString(property.getGogsSecret()); /* Secret provided by Jenkins */
                        isRefMatched = property.filterBranch(ref);
                    }
                } else {
                    String[] components = ref.split("/");
                    if (components.length > 3) {
                        /* refs contains branch/tag with a slash */
                        List<String> test = Arrays.asList(ref.split("/"));
                        ref = String.join("%2F", test.subList(2, test.size()));
                    } else {
                        ref = components[components.length - 1];
                    }

                    job = GogsUtils.find(jobName + "/" + ref, Job.class);

                    if (job != null) {
                        foundJob = true;
                        /* secret is stored in the properties of Job */
                        final GogsProjectProperty property = (GogsProjectProperty) job.getProperty(GogsProjectProperty.class);
                        if (property != null) { /* only if Gogs secret is defined on the job */
                            jSecret = Secret.toString(property.getGogsSecret()); /* Secret provided by Jenkins */
                            isRefMatched = property.filterBranch(ref);
                        }
                    }
                }
            } finally {
                SecurityContextHolder.setContext(saveCtx);
            }

            String gSecret = null;
            if (gogsSignature == null) {
                gSecret = jsonObject.optString("secret", null);  /* Secret provided by Gogs < 0.10.x   */
            } else {
                try {
                    if (gogsSignature.equals(encode(body, jSecret))) {
                        gSecret = jSecret;
                        // now hex is right, continue to old logic
                    }
                } catch (Exception e) {
                    LOGGER.warning(e.getMessage());
                }
            }

            if (!foundJob) {
                String msg = String.format("Job '%s' is not defined in Jenkins", jobName);
                result.setStatus(404, msg);
                LOGGER.warning(msg);
            } else if (!isRefMatched) {
                String msg = String.format("received ref ('%s') is not matched with branch filter in job '%s'", ref, jobName);
                result.setStatus(200, msg);
                LOGGER.info(msg);
            } else if (isNullOrEmpty(jSecret) && isNullOrEmpty(gSecret)) {
                /* No password is set in Jenkins and Gogs, run without secrets */
                result = payloadProcessor.triggerJobs(jobName, gogsDelivery);
            } else if (!isNullOrEmpty(jSecret) && jSecret.equals(gSecret)) {
                /* Password is set in Jenkins and Gogs, and is correct */
                result = payloadProcessor.triggerJobs(jobName, gogsDelivery);
            } else {
                /* Gogs and Jenkins secrets differs */
                result.setStatus(403, "Incorrect secret");
            }
        } else {
            result.setStatus(404, "No payload or URI contains invalid entries.");
        }

        exitWebHook(result, rsp);
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

    /**
     * Converts Querystring into Map<String,String>
     *
     * @param qs Querystring
     * @return returns map from querystring
     */
    private static Map<String, String> splitQuery(String qs) throws UnsupportedEncodingException {
        final Map<String, String> query_pairs = new LinkedHashMap<>();
        final String[] pairs = qs.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), DEFAULT_CHARSET) : pair;
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), DEFAULT_CHARSET) : null;
            query_pairs.put(key, value);
        }
        return query_pairs;
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}

// vim: set ts=4 sw=4 tw=0 ft=java et :
