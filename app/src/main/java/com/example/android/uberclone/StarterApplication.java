package com.example.android.uberclone;

/**
 * Created by MAC-WFR on 8/10/17.
 */

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class StarterApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Add your initialization code here
        Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
                .applicationId("4dca2e8894fc889559d683f0b62d6907dff77cac")
                .clientKey("3b2f860d883685f11bfd5a5fe1865e7d346b62e4")
                .server("http://ec2-18-220-119-100.us-east-2.compute.amazonaws.com:80/parse/")
                .build()
        );

        //ParseUser.enableAutomaticUser();

        ParseACL defaultACL = new ParseACL();
        defaultACL.setPublicReadAccess(true);
        defaultACL.setPublicWriteAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);

    }
}
