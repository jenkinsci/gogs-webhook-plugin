package org.jenkinsci.plugins.gogs;

public class GogsResults {
    int Status;
    String Message;

    public GogsResults() {
      this.Status = 200;
      this.Message = "OK";
    }

    public void setStatus(int status, String msg) {
      this.Status = status;
      this.Message = msg;
    }
}
