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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Cause;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.jenkinsci.plugins.gogs.GogsUtils.cast;

class GogsCause extends Cause {
    private String deliveryID;
    private final Map<String, String> envVars = new HashMap<>();
    private final static Logger LOGGER = Logger.getLogger(GogsCause.class.getName());


    public GogsCause() {
    }

    public GogsCause(String deliveryID) {
        this.deliveryID = deliveryID;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public void setDeliveryID(String deliveryID) {
        this.deliveryID = deliveryID;
    }

    public void setGogsPayloadData(String json) {
        Map<String, Object> gogsPayloadData = null;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            gogsPayloadData = cast(objectMapper.readValue(json, Map.class));
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
        }
        if (gogsPayloadData != null) {
            iterate(gogsPayloadData, null);
        }
    }

    private void iterate(Map<String, Object> map, String prefix) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            StringBuilder env_name = new StringBuilder();
            if (prefix != null)
                env_name.append(prefix.toUpperCase()).append("_");

            if (entry.getValue() instanceof Map) {
                //noinspection unchecked
                iterate((Map<String, Object>) entry.getValue(), env_name + entry.getKey().toUpperCase());
            } else if (entry.getValue() instanceof String || entry.getValue() instanceof Long || entry.getValue() instanceof Boolean) {
                env_name.append(entry.getKey().toUpperCase());
                envVars.put("GOGS_" + env_name.toString(), entry.getValue().toString());
            }
        }
    }

    @Override
    public String getShortDescription() {
        return this.deliveryID;
    }
}
