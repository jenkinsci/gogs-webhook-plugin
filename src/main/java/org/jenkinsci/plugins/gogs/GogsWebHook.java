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

import hudson.Extension;
import hudson.model.UnprotectedRootAction;

import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashMap;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.UnsupportedEncodingException;
import javax.inject.Inject;
import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Alexander Verhaar
 */
@Extension
public class GogsWebHook implements UnprotectedRootAction {
    private final static Logger LOGGER = Logger.getLogger(GogsWebHook.class.getName());
    public static final String URLNAME = "gogs-webhook";
    private StaplerResponse resp;

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
     * Receives the HTTP POST request send by Gogs.
     *
     * @param req request
     * @return response to the request
     */
    public void doIndex(StaplerRequest req, StaplerResponse rsp)  throws IOException {
      GogsResults result = new GogsResults();
      GogsPayloadProcessor payloadProcessor = new GogsPayloadProcessor();
      this.resp = rsp;

      // Get X-Gogs-Event
      String event = req.getHeader("X-Gogs-Event");
      if (!"push".equals(event)) {
        result.setStatus(403, "Only push event can be accepted.");
        exitWebHook(result);
      }

      // Get X-Gogs-Delivery header with deliveryID
      String gogsDelivery = req.getHeader("X-Gogs-Delivery");
      if (gogsDelivery.isEmpty()) {
        gogsDelivery = "Triggered by Jenkins-Gogs-Plugin. Delivery ID unknown.";
      } else {
        gogsDelivery = "Gogs-ID: " + gogsDelivery;
      }

      // Get querystring from the URI
      Map querystring = splitQuery(req.getQueryString());
      String jobName = querystring.get("job").toString();
      if ( jobName!=null && jobName.isEmpty()) {
        result.setStatus(404, "Parameter 'job' is missing or no value assigned.");
        exitWebHook(result);
      }

      // Get the POST stream
      String body = IOUtils.toString(req.getInputStream());
      if ( !body.isEmpty()  && req.getRequestURI().contains("/" + URLNAME + "/") ) {
        String contentType = req.getContentType();
        if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
          body = URLDecoder.decode(body);
        }
        if (body.startsWith("payload=")) {
          body = body.substring(8);
        }

        JSONObject jsonObject = JSONObject.fromObject(body);
        String url = jsonObject.getJSONObject("repository").getString("url");

        result = payloadProcessor.triggerJobs(jobName, gogsDelivery);
      } else {
        result.setStatus(404, "No payload or URI contains invalid entries.");
      }

      exitWebHook(result);
    }

    /**
     * Exit the WebHook
     *
     * @param results GogsResults
     */
    private void exitWebHook(GogsResults result)  throws IOException {
      if ( result.Status != 200 ) {
        LOGGER.warning(result.Message);
      }
      JSONObject json = new JSONObject();
      json.put("result", result.Status==200 ? "OK" : "ERROR");
      json.put("message", result.Message);
      resp.setStatus(result.Status);
      resp.addHeader("Content-Type","application/json");
      resp.getWriter().print(json.toString());
    }

    /**
     * Converts Querystring into Map<String,String>
     *
     * @param qs Querystring
     * @return returns map from querystring
     */
    private static Map<String, String> splitQuery(String qs) throws UnsupportedEncodingException {
      final Map<String, String> query_pairs = new LinkedHashMap<String, String>();
      final String[] pairs = qs.split("&");
      for (String pair : pairs) {
        final int idx = pair.indexOf("=");
        final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
        final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
        query_pairs.put(key,value);
      }
      return query_pairs;
    }
}

// vim: set ts=4 sw=4 tw=0 ft=java et :
