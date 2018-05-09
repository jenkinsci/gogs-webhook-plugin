package org.jenkinsci.plugins.gogs;

import java.util.List;

@SuppressWarnings("unused")
public class GogsPayloadData {
    @SuppressWarnings("unused")
    public static class UserDetails {
        public String name;
        public String email;
        public String username;
    }

    @SuppressWarnings("unused")
    public static class Owner {
        public Long id;
        public String login;
        public String full_name;
        public String email;
        public String avatar_url;
        public String username;
    }

    @SuppressWarnings("unused")
    public static class Commits {
        public String id;
        public String message;
        public String url;
        public UserDetails author;
        public UserDetails committer;
        public List<String> added;
        public List<String> removed;
        public List<String> modified;
        public String timestamp;
    }

    @SuppressWarnings("unused")
    public static class Repository {
        public Long id;
        public Owner owner;
        public String name;
        public String full_name;
        public String description;
        public Boolean Private;
        public Boolean fork;
        public Boolean parent;
        public Boolean empty;
        public Boolean mirror;
        public Long size;
        public String html_url;
        public String ssh_url;
        public String clone_url;
        public String website;
        public Long stars_count;
        public Long forks_count;
        public Long watchers_count;
        public Long open_issues_count;
        public String default_branch;
        public String created_at;
        public String updated_at;
    }

    public String ref;
    public String before;
    public String after;
    public String compare_url;
    public List<Commits> commits;
    public Repository repository;
    public Owner pusher;
    public Owner sender;
}
