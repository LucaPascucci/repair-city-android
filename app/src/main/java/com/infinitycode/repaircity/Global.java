package com.infinitycode.repaircity;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Infinity Code on 11/08/2015.
 */
public class Global {

    public static final String keyPreferencesFB = "facebookID";
    public static final String keyPreferencesMap = "mapType";
    public static final String keyPreferencesSegnalationRange = "segnalationRange";
    public static final String keyPreferencesFirstLaunch = "firstLaunch";
    public static final String keyPreferencesCurrentActivity = "currentActivity";
    public static final String keyPreferencesSelectedNavigation = "selectedActivity";
    public static final String keyPreferencesForcedSync = "forcedSync";
    public static final String keyPreferencesUsyncSegnalation = "unsyncSegnalation";
    public static final String getKeyPreferencesLocalSyncSegnalation = "localSyncSegnalation";
    public static final String keyPreferencesUsyncUser = "unsyncUser";
    public static final String getKeyPreferencesLocalSyncUser = "localSyncUser";

    private final SharedPreferences preferences;
    private final Context context;

    // Costruttore
    public Global(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
    }

    // Salvo una stringa nelle preferences
    public void writeStringInPreferences(final String key, final String value) {
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    // Salvo un int nelle preferences
    public void writeIntInPreferences(final String key, final int value) {
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    // Salvo un bool nelle preferences
    public void writeBooleanInPreferences(final String key, final boolean value) {
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    // Prengo una stringa dalle preferences
    public String getStringFromPreferences(final String key) {
        return preferences.getString(key, null);
    }

    // Prendo un int dalle preferences
    public int getIntFromPreferences(final String key) {
        return this.preferences.getInt(key, 25);
    }

    public boolean getBooleanFromPreferences(final String key) {
        return this.preferences.getBoolean(key, false);
    }

    public void removeFromSharedPreferences(final String key) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public static boolean isNetworkAvailable() {
        try {
            final URL url = new URL("https://google.com/");
            final URLConnection conn = url.openConnection();
            conn.connect();
            Log.d("Global", "Network Available");
            return true;
        } catch (MalformedURLException e) {
            Log.d("Global", "Network Not Available");
            return false;
        } catch (IOException e) {
            Log.d("Global", "Network Not Available");
            return false;
        }
    }

    public boolean checkGeolocalization (){
        try {
            int locationMode = Secure.getInt(this.context.getContentResolver(), Secure.LOCATION_MODE);
            switch (locationMode) {
                case Secure.LOCATION_MODE_HIGH_ACCURACY:
                case Secure.LOCATION_MODE_SENSORS_ONLY:
                    return true;
                case Secure.LOCATION_MODE_BATTERY_SAVING:
                case Secure.LOCATION_MODE_OFF:
                default:
                    return false;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
