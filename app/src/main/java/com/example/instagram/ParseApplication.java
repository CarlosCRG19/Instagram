package com.example.instagram;

import android.app.Application;

import com.example.instagram.models.Comment;
import com.example.instagram.models.Like;
import com.example.instagram.models.Post;
import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Register all classes for objects before initializing parse
        ParseObject.registerSubclass(Like.class);
        ParseObject.registerSubclass(Post.class);
        ParseObject.registerSubclass(Comment.class);

        // Connect to database using secret keys
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.APPLICATION_ID)) // Get keys from secrets
                .clientKey(getString(R.string.CLIENT_KEY))
                .server("https://parseapi.back4app.com")
                .build()
        );
    }
}
