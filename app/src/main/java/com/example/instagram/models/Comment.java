package com.example.instagram.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("Comment")
public class Comment extends ParseObject {

    public static final String KEY_BODY = "body";
    public static final String KEY_USER = "user";
    public static final String KEY_POST = "post";

    public void setPost(Post post){ put(KEY_POST, post); }
    public Post getPost() {return (Post) get(KEY_POST); }


    public String getBody() { return getString(KEY_BODY); }

    public void setBody(String body) { put(KEY_BODY, body); }

    public ParseUser getUser() {
        return getParseUser(KEY_USER);
    }

    public void setUser(ParseUser user) {
        put(KEY_USER, user);
    }

}
