package org.jenkinsci.plugins.gogs;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import hudson.model.Item;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Hex;

class GogsUtils {

    private GogsUtils() {
    }

    /**
     * Search in Jenkins for a item with type T based on the job name
     *
     * @param jobName job to find, for jobs inside a folder use : {@literal <folder>/<folder>/<jobName>}
     * @return the Job matching the given name, or {@code null} when not found
     */
    static <T extends Item> T find(String jobName, Class<T> type) {
        Jenkins jenkins = Jenkins.get();
        // direct search, can be used to find folder based items <folder>/<folder>/<jobName>
        T item = jenkins.getItemByFullName(jobName, type);
        if (item == null) {
            // not found in a direct search, search in all items since the item might be in a folder but given without folder structure
            // (to keep it backwards compatible)
            item = jenkins.getAllItems(type).stream().filter(i -> i.getName().equals(jobName)).findFirst().orElse(null);
        }
        return item;
    }

    /**
     * Converts Querystring into Map<String,String>
     *
     * @param qs Querystring
     * @return returns map from querystring
     */
    static Map<String, String> splitQuery(String qs) {
        return Pattern.compile("&").splitAsStream(qs)
                .map(p -> p.split("="))
                .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? urlDecode(a[1]) : ""));
    }

    /**
     * Decode URL
     *
     * @param s String to decode
     * @return returns decoded string
     */
    static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ignored) { }
        return "";
    }

    /**
     * encode sha256 hmac
     *
     * @param data data to hex
     * @param key  key of HmacSHA256
     * @return a String with the encoded sha256 hmac
     * @throws Exception Something went wrong getting the sha256 hmac
     */
    static String encode(String data, String key) throws Exception {
        final Charset asciiCs = Charset.forName("UTF-8");
        final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        final SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(asciiCs.encode(key).array(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
    }

    /**
     * Cast object
     *
     * @param obj object to cast
     * @param <T> cast to type
     * @return Casted object
     */
    @SuppressWarnings("unchecked")
    static public <T> T cast(Object obj) {
        return (T) obj;
    }
}
