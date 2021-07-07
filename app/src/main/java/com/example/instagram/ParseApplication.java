package com.example.instagram;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        /*Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("BCrUQVkk80pCdeImSXoKXL5ZCtyyEZwbN7mAb11f")
                .clientKey("rWFPEbTs7UzkaVsIXnQ4qmmr9oWqwXfiiJehtIZu")
                .server("https://parseapi.back4app.com")
                .build()
        );*/

        ParseObject.registerSubclass(Post.class);

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("5fMlL5b2J8lSQIxItFrHzx8A84i75LBOi1ZLmuja")
                .clientKey("45kX5He0CGz6I9a5caVCjUuIXAHr0ZstKkrq8wSl")
                .server("https://parseapi.back4app.com")
                .build()
        );
    }
}
