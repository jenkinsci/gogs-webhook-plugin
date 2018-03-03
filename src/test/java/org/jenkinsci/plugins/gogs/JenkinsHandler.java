package org.jenkinsci.plugins.gogs;

import com.offbytwo.jenkins.JenkinsServer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by jmm on 14/05/2017.
 */
public class JenkinsHandler {
    /**
     * The time we wait until we break the beforeSuit method to prevent to wait
     * forever.
     */
    public static final Long TIME_OUT_MILLISECONDS = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);

    public static void waitUntilJenkinsHasBeenStartedUp(JenkinsServer jenkinsServer) throws TimeoutException {
        final long start = System.currentTimeMillis();
        System.out.print("Wait until Jenkins is started...");
        while (!jenkinsServer.isRunning() && !timeOut(start)) {
            try {
                System.out.print(".");
                Thread.sleep(TimeUnit.MILLISECONDS.convert(1L, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!jenkinsServer.isRunning() && timeOut(start)) {
            System.out.println("Failure.");
            throw new TimeoutException("Jenkins startup check has failed. Took more than one minute.");
        }

        System.out.println("done.");
    }

    /**
     * Check if we have reached timeout related to the
     * {@link #TIME_OUT_MILLISECONDS}.
     *
     * @param start
     *            The start time in milliseconds.
     * @return true if timeout false otherwise.
     */
    private static boolean timeOut(final long start) {
        boolean result = false;

        long elapsed = System.currentTimeMillis() - start;
        if (elapsed >= TIME_OUT_MILLISECONDS) {
            result = true;
        }

        return result;
    }
}
