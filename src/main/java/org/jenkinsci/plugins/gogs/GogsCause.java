package org.jenkinsci.plugins.gogs;

import hudson.model.Cause;

public class GogsCause extends Cause {
    String deliveryID = "";

    public GogsCause(String deliveryID)
    {
      this.deliveryID = deliveryID;
    }

    @Override
    public String getShortDescription() {
        return this.deliveryID;
    }
}
