package com.infinitycode.repaircity;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.parse.Parse;

/**
 * Created by Infinity Code on 11/08/2015.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this); //attiva il localdatastore
        Parse.initialize(this, getString(R.string.parse_id), getString(R.string.parse_key));

        // Inizializza Facebook SDK
        FacebookSdk.sdkInitialize(App.this);
    }

}
