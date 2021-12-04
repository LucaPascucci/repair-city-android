package com.infinitycode.repaircity.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.NavigationDrawerFragment;
import com.infinitycode.repaircity.R;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.parse.Utente;

/**
 * Created by Infinity Code on 14/08/15.
 */

public class SettingsActivity extends AppCompatActivity {

    private static final int ACTIVITY_NUMBER = 4;
    private NavigationDrawerFragment drawerFragment;
    private static final int SYNC_SEGNALATIONS = 1;
    private static final int SYNC_USERS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        new Global(SettingsActivity.this).writeIntInPreferences(Global.keyPreferencesCurrentActivity, ACTIVITY_NUMBER);

        this.drawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_settings_drawer);
        this.drawerFragment.setUp(R.id.fragment_settings_drawer, (DrawerLayout) findViewById(R.id.drawer_layout_settings), toolbar);

        if (new Global(SettingsActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncSegnalation) && !new Global(SettingsActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncSegnalation)) {
            Log.d("MapActivity", "Ci sono segnalazioni locali");
            new SyncLocalData().execute(SYNC_SEGNALATIONS);
        }
        if (new Global(SettingsActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncUser) && !new Global(SettingsActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncUser)) {
            Log.d("MapActivity", "Ci sono utenti locali");
            new SyncLocalData().execute(SYNC_USERS);
        }
    }

    @Override
    public void onBackPressed() {
        if (this.drawerFragment.isVisible()) {
            this.drawerFragment.closeDrawer();
        } else {
            super.onBackPressed();
            this.finish();
        }
    }

    private class SyncLocalData extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {

            if (Global.isNetworkAvailable()) {
                switch (params[0]) {
                    case SYNC_SEGNALATIONS:
                        Log.d("MapActivity", "Avviata sync segnalazioni locali");
                        new Global(SettingsActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, true);
                        new Segnalazione(SettingsActivity.this).syncingLocalChanges();
                        break;
                    case SYNC_USERS:
                        Log.d("MapActivity", "Avviata sync utenti locali");
                        new Global(SettingsActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, true);
                        new Utente(SettingsActivity.this).syncingLocalChanges();
                        break;
                }
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(Integer value) {
            super.onPostExecute(value);
            switch (value) {
                case SYNC_SEGNALATIONS:
                    Log.d("MapActivity", "Conclusa sync segnalazioni locali");
                    new Global(SettingsActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, false);
                    break;
                case SYNC_USERS:
                    Log.d("MapActivity", "Conclusa sync utenti locali");
                    new Global(SettingsActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, false);
                    break;
            }
        }
    }

}
