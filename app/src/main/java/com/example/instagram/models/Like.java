package com.example.instagram.models;

import android.util.Log;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.Date;

@ParseClassName("Like")
public class Like extends ParseObject {

    public static final String KEY_USER = "user";
    public static final String KEY_POST = "post";

    public void setPost(Post post){ put(KEY_POST, post); }
    public Post getPost() {return (Post) get(KEY_POST); }


    public ParseUser getUser() {
        return getParseUser(KEY_USER);
    }

    public void setUser(ParseUser user) {
        put(KEY_USER, user);
    }
}
