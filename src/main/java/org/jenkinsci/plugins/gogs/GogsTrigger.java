package org.jenkinsci.plugins.gogs;


import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.logging.Logger;


public class GogsTrigger extends Trigger<Job<?,?>> {

    @DataBoundConstructor
    public GogsTrigger() {
    }

    @Deprecated
    public void onPost(String triggeredByUser) {
        onPost(triggeredByUser, "");
    }

    public void onPost(String triggeredByUser, final String payload) {
        final String pushBy = triggeredByUser;
        getDescriptor().queue.execute(new Runnable() {

            @Override
            public void run() {

            }
        });
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Hudson.MasterComputer.threadPoolForRemoting);

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job && SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(item) != null
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        @Override
        public String getDisplayName() {
            return "Build when a change is pushed to Gogs";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(GogsTrigger.class.getName());
}
