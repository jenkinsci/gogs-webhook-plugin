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
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

@SuppressWarnings("ALL")
public class GogsProjectProperty extends JobProperty<Job<?, ?>> {
    private final Secret gogsSecret;
    private final boolean gogsUsePayload;
    private final String gogsBranchFilter;

    @Deprecated
    public GogsProjectProperty(String gogsSecret, boolean gogsUsePayload, String gogsBranchFilter) {
        this.gogsSecret = Secret.fromString(gogsSecret);
        this.gogsUsePayload = gogsUsePayload;
        this.gogsBranchFilter = gogsBranchFilter;
    }

    @DataBoundConstructor
    public GogsProjectProperty(Secret gogsSecret, boolean gogsUsePayload, String gogsBranchFilter) {
        this.gogsSecret = gogsSecret;
        this.gogsUsePayload = gogsUsePayload;
        this.gogsBranchFilter = gogsBranchFilter;
    }
    public String getGogsSecret() {
        return Secret.toString(this.gogsSecret);
    }

    public boolean getGogsUsePayload() {
        return this.gogsUsePayload;
    }

    public String getGogsBranchFilter() {
        return this.gogsBranchFilter;
    }

    public boolean getHasBranchFilter() {
        return gogsBranchFilter != null && gogsBranchFilter.length() > 0;
    }

    public boolean filterBranch(String ref) {
        if (gogsBranchFilter != null && gogsBranchFilter.length() > 0 && !gogsBranchFilter.equals("*")) {
            return ref == null || ref.length() == 0 || ref.endsWith(gogsBranchFilter);
        }
        return true;
    }

    private static final Logger LOGGER = Logger.getLogger(GogsWebHook.class.getName());

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {
        public static final String GOGS_PROJECT_BLOCK_NAME = "gogsProject";
        private Secret gogsSecret;
        private boolean gogsUsePayload;
        private String gogsBranchFilter;

        public String getGogsSecret() {
            return Secret.toString(gogsSecret);
        }

        public boolean getGogsUsePayload() {
            return gogsUsePayload;
        }

        public String getGogsBranchFilter() {
            return gogsBranchFilter;
        }

        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) {
            GogsProjectProperty tpp = null;

            if (req != null) {
                tpp = req.bindJSON(
                        GogsProjectProperty.class,
                        formData.getJSONObject(GOGS_PROJECT_BLOCK_NAME)
                );
            }
            if (tpp != null) {
                LOGGER.finest(formData.toString());
                LOGGER.finest(tpp.gogsBranchFilter);

                gogsSecret = tpp.gogsSecret;
                gogsBranchFilter = tpp.gogsBranchFilter;
            }
            return tpp;
        }

        @Override
        public String getDisplayName() {
            return "Gogs Project Property";
        }
    }
}
