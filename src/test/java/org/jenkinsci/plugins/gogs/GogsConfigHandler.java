package org.jenkinsci.plugins.gogs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

/**
 * A class to handle the configuration of the Gogs server used during the integration tests.
 */
public class GogsConfigHandler {

    private String gogsServer_nodeName;
    private int gogsServer_port;
    private String gogsServer_user;
    private String gogsServer_password;
    private Executor executor = null;
    private String gogsServer_apiUrl = null;
    private String gogsAccessToken;

    /**
     * Instantiates an object used to handle operations on a Gogs server.
     * Operations can be verifying that the server up, create a web hook, etc.
     *
     * @param gogsUrl  the Url of the Gogs server (in the form of "http://localhost:3000" for example)
     * @param user     the Gogs user under which the manipulations are done (in general "butler")
     * @param password the password of the Gogs user (in general "butler")
     * @throws MalformedURLException
     */
    public GogsConfigHandler(String gogsUrl, String user, String password) throws MalformedURLException {
        URL aURL = new URL(gogsUrl);

        this.gogsServer_nodeName = aURL.getHost();
        this.gogsServer_port = aURL.getPort();
        this.gogsServer_user = user;
        this.gogsServer_password = password;
        this.gogsAccessToken = getGogsAccessToken();
    }

    /**
     * A method to wait for the availability of the Gogs server
     *
     * @param retries    the number of times we should try to connect
     * @param retryDelay the number of seconds to wait between tentatives
     * @throws TimeoutException     thrown when number of retries was exhausted
     * @throws InterruptedException thrown when the wait has been interrupted
     */
    void waitForServer(int retries, int retryDelay) throws TimeoutException, InterruptedException {
        String testUrl = this.getGogsUrl() + "/";

        for (int i = 0; i < retries; i++) {
            int status;
            try {
                status = Request.Get(testUrl)
                                .execute().returnResponse().getStatusLine().getStatusCode();
            } catch (IOException e) {
                TimeUnit.SECONDS.sleep(retryDelay);
                continue;
            }
            if (status == 200) {
                return;
            } else {
                TimeUnit.SECONDS.sleep(retryDelay);
            }
        }
        throw new TimeoutException("Timeout waiting for availability of " + testUrl);
    }

    /**
     * Rebuilds the complete Gogs URL from the stored properties
     *
     * @return the complete Gogs URL
     */
    private String getGogsUrl() {
        return "http://" + this.gogsServer_nodeName + ":" + this.gogsServer_port;
    }

    /**
     * Creates a web hook in Gogs with the passed json configuration string
     *
     * @param jsonCommand A json buffer with the creation command of the web hook
     * @param projectName the project (owned by the user) where the webHook should be created
     * @throws IOException something went wrong
     */
    int createWebHook(String jsonCommand, String projectName) throws IOException {

        String gogsHooksConfigUrl = getGogsServer_apiUrl()
                + "repos/" + this.gogsServer_user
                + "/" + projectName + "/hooks";

        Executor executor = getExecutor();
        Request request = Request.Post(gogsHooksConfigUrl);

        if (gogsAccessToken != null) {
            request.addHeader("Authorization", "token " + gogsAccessToken);
        }

        String result = executor
                .execute(request.bodyString(jsonCommand, ContentType.APPLICATION_JSON))
                .returnContent().asString();
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON( result );
        return jsonObject.getInt("id");
    }

    /**
     * Creates a webhook in Gogs with the passed json configuration string
     *
     * @param jsonCommandFile A json file containing the creation command of the web hook
     * @param projectName     the project (owned by the user) where the webHook should be created
     * @throws IOException something went wrong
     */
    int createWebHook(File jsonCommandFile, String projectName) throws IOException {
        byte[] encoded = Files.readAllBytes(jsonCommandFile.toPath());
        String jsonCommand = new String(encoded, Charset.defaultCharset());

        return createWebHook(jsonCommand, projectName);
    }


    /**
     * Removes a web hook from a GOGS project
     *
     * @param projectName Name of the Gogs project to remove the hook from
     * @param hookId      The ID of the hook to remove
     * @throws IOException something went wrong
     */
    void removeHook(String projectName, int hookId) throws IOException {
        String gogsHooksConfigUrl = getGogsServer_apiUrl()
                + "repos/" + this.gogsServer_user
                + "/" + projectName + "/hooks/" + hookId;

        Executor executor = getExecutor();
        Request request = Request.Delete(gogsHooksConfigUrl);

        if (gogsAccessToken != null) {
            request.addHeader("Authorization", "token " + gogsAccessToken);
        }

        int result = executor
                .execute(request)
                .returnResponse().getStatusLine().getStatusCode();

        if (result != 204) {
            throw new IOException("Delete hook did not return the expected value (returned " + result + ")");
        }
    }

    /**
     * Creates an empty repository (under the configured user).
     * It is created as a public repository, un-initialized.
     *
     * @param projectName the project name (repository) to create
     * @throws IOException Something went wrong (example: the repo already exists)
     */
    void createEmptyRepo(String projectName) throws IOException {

        Executor executor = getExecutor();
        String gogsHooksConfigUrl = getGogsServer_apiUrl() + "user/repos";
        Request request = Request.Post(gogsHooksConfigUrl);

        if (this.gogsAccessToken != null) {
            request.addHeader("Authorization", "token " + this.gogsAccessToken);
        }

        int result = executor
                .execute(request
                        .bodyForm(Form.form()
                                      .add("name", projectName)
                                      .add("description", "API generated repository")
                                      .add("private", "true")
                                      .add("auto_init", "false")
                                      .build()
                        )
                )
                .returnResponse().getStatusLine().getStatusCode();


        if (result != 201) {
            throw new IOException("Repository creation call did not return the expected value (returned " + result + ")");
        }
    }

    void removeRepo(String projectName) throws IOException {
        Executor executor = getExecutor();
        String gogsHookConfigUrl = getGogsServer_apiUrl() + "repos/" + this.gogsServer_user + "/" + projectName;
        Request request = Request.Delete(gogsHookConfigUrl);

        if (this.gogsAccessToken != null) {
            request.addHeader("Authorization", "token " + this.gogsAccessToken);
        }

        int result = executor.execute(request).returnResponse().getStatusLine().getStatusCode();
        if (result != 204) {
            throw new IOException("Repository deletion call did not return the expected value (returned " +  result + ")");
        }
    }

    /**
     * Gets a Executor object. The Executor object allows to cache the authentication data.
     * If it was not initialized, the method instantiates one.
     *
     * @return an initialized Executor object
     */
    private Executor getExecutor() {
        if (this.executor == null) {
            HttpHost httpHost = new HttpHost(this.gogsServer_nodeName, this.gogsServer_port);
            this.executor = Executor.newInstance()
                                    .auth(httpHost, this.gogsServer_user, this.gogsServer_password)
                                    .authPreemptive(httpHost);
        }
        return this.executor;
    }

    /**
     * Get Access token of the user.
     *
     * @return an access token of the user
     */
    public String getGogsAccessToken() {
        String resp;
        String sha1 = null;
        Executor executor = getExecutor();
        try {
            resp = executor.execute(
                    Request.Get(this.getGogsUrl() + "/api/v1/users/" + this.gogsServer_user + "/tokens")
            ).returnContent().toString();
            JSONArray jsonArray = JSONArray.fromObject(resp);
            if (!jsonArray.isEmpty()) {
                sha1 = ((JSONObject) jsonArray.get(0)).getString("sha1");
            }
        } catch (IOException e) { }
        return sha1;
    }

    public String getGogsServer_apiUrl() {
        if (this.gogsServer_apiUrl == null) {
            this.gogsServer_apiUrl = this.getGogsUrl() + "/api/v1/";
        }
        return gogsServer_apiUrl;
    }
}
