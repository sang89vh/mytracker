package com.mybox.mytracker.application;

import android.app.Application;

import com.mybox.mytracker.R;
import com.parse.Parse;

/**
 * Created by jack on 3/12/16.
 */
public class MBApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.enableLocalDatastore(this);
        Parse.initialize(new Parse.Configuration.Builder(this)
                        .applicationId(getString(R.string.PARSE_APP_ID))
                        .clientKey(getString(R.string.PARSE_CLIENT_KEY))
                        .server(getString(R.string.PARSE_SERVER_URL))
                        .build()
        );


    }



}
