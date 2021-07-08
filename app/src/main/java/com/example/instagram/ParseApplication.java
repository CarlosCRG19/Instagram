package com.example.instagram;

import android.app.Application;

import com.example.instagram.models.Comment;
import com.example.instagram.models.Post;
import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        ParseObject.registerSubclass(Post.class);
        ParseObject.registerSubclass(Comment.class);

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("5fMlL5b2J8lSQIxItFrHzx8A84i75LBOi1ZLmuja")
                .clientKey("45kX5He0CGz6I9a5caVCjUuIXAHr0ZstKkrq8wSl")
                .server("https://parseapi.back4app.com")
                .build()
        );
    }
}
