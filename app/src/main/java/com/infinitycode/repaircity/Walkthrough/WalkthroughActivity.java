package com.infinitycode.repaircity.walkthrough;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.infinitycode.repaircity.activity.MapActivity;
import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.parse.Utente;
import com.infinitycode.repaircity.parse.Valutazione;
import com.infinitycode.repaircity.R;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Infinity Code on 14/08/15.
 */

public class WalkthroughActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    /*
    * Define a request code to send to Google Play services
    * This code is returned in Activity.onActivityResult
    */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static final int latestPage = 5;
    private ImageButton next;
    private ImageButton prev;
    private ImageButton close;
    private FragmentManager fragmentManager;
    private int currentPage = 0;
    private boolean finishDownload = false;
    private ProgressDialog loading;
    private AsyncDownload asyncDownload;

    private AlertDialog networkDialog;
    private AlertDialog GPSdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_walkthrough);
        new Global(WalkthroughActivity.this).writeBooleanInPreferences(Global.keyPreferencesFirstLaunch, false);

        if (new Global(WalkthroughActivity.this).getIntFromPreferences(Global.keyPreferencesCurrentActivity) != 4) {
            new Global(WalkthroughActivity.this).writeIntInPreferences(Global.keyPreferencesSegnalationRange, 25);
        } else {
            this.finishDownload = true;
        }

        this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        this.mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds

        this.loading = new ProgressDialog(WalkthroughActivity.this);
        this.fragmentManager = getSupportFragmentManager();
        this.nextPage(this.currentPage);

        this.prev = (ImageButton) this.findViewById(R.id.prevButton);
        this.prev.setVisibility(View.INVISIBLE);
        this.close = (ImageButton) this.findViewById(R.id.closeButton);
        this.close.setVisibility(View.INVISIBLE);
        this.next = (ImageButton) this.findViewById(R.id.nextButton);

        this.next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPage++;

                nextPage(currentPage);

                prev.setVisibility(View.VISIBLE);
                if (currentPage == latestPage) {
                    next.setVisibility(View.INVISIBLE);
                    close.setVisibility(View.VISIBLE);
                }
            }
        });

        this.prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPage--;
                prevPage();

                if (currentPage < latestPage) {
                    close.setVisibility(View.INVISIBLE);
                }
                if (currentPage == 0) {
                    prev.setVisibility(View.INVISIBLE);
                }
                next.setVisibility(View.VISIBLE);
            }
        });

        this.close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGoogleApiClient.connect();
                if (new Global(WalkthroughActivity.this).checkGeolocalization()) {
                    if (finishDownload) {
                        Log.d("WALKTHROUGH", "finish Download");
                        if (new Global(WalkthroughActivity.this).getIntFromPreferences(Global.keyPreferencesCurrentActivity) != 4) {
                            Intent intent = new Intent(WalkthroughActivity.this, MapActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            onBackPressed();
                        }
                    } else {
                        Log.d("WALKTHROUGH", "wait download");
                        if (!loading.isShowing()) {
                            loading = ProgressDialog.show(WalkthroughActivity.this, null, getString(R.string.first_use), true, false);
                        }
                    }
                } else {
                    showErrorDialogGPS();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("WALKTHROUGH", "onResume");
        if (new Global(WalkthroughActivity.this).getIntFromPreferences(Global.keyPreferencesCurrentActivity) != 4) {
            if (!new Global(WalkthroughActivity.this).checkGeolocalization()) {
                showErrorDialogGPS();
            }
        }
        this.mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("WALKTHROUGH", "onPause");
        if (this.mGoogleApiClient.isConnected()) {
            this.mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("WALKTHROUGH", "onConnected");
        final Location location = LocationServices.FusedLocationApi.getLastLocation(this.mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(this.mGoogleApiClient, this.mLocationRequest, this);
        } else {
            Log.d("WALKTHROUGH", "location != null");
            if (new Global(WalkthroughActivity.this).getIntFromPreferences(Global.keyPreferencesCurrentActivity) != 4) {
                if (asyncDownload == null) {
                    Log.d("WALKTHROUGH", "Avviato download");
                    this.asyncDownload = new AsyncDownload();
                    this.asyncDownload.execute(location);
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("WALKTHROUGH", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("WALKTHROUGH", "onConnectionFailed");
    /*
     * Google Play services can resolve some errors it detects.
     * If the error has a resolution, try sending an Intent to
     * start a Google Play services activity that can resolve
     * error.
     */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            /*
             * Thrown if Google Play services canceled the original
             * PendingIntent
             */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("WALKTHROUGH", "onLocationChanged");
        if (new Global(WalkthroughActivity.this).getIntFromPreferences(Global.keyPreferencesCurrentActivity) != 4) {
            if (asyncDownload == null) {
                Log.d("WALKTHROUGH", "Avviato download in onLocationChanged");
                this.asyncDownload = new AsyncDownload();
                this.asyncDownload.execute(location);
            }
        }
    }

    private void nextPage(int page) {
        FragmentTransaction fragmentTransaction = this.fragmentManager.beginTransaction();
        WTPageFragment pageFragment = new WTPageFragment();
        pageFragment.setPage(page);
        if (page > 0) {
            fragmentTransaction.setCustomAnimations(R.anim.right_in, R.anim.right_out, R.anim.right_in, R.anim.right_out);
        }
        fragmentTransaction.add(R.id.listFragment, pageFragment).addToBackStack("" + page);
        fragmentTransaction.commit();
    }

    private void prevPage() {
        this.fragmentManager.popBackStack();
    }

    private void showErrorDialogGPS() {
        if (this.GPSdialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(WalkthroughActivity.this);
            builder.setTitle(R.string.GPS_status)
                    .setIcon(R.drawable.ic_error)
                    .setMessage(R.string.GPS_status_description)
                    .setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    });
            this.GPSdialog = builder.create();
        }
        if (!this.GPSdialog.isShowing()) {
            this.GPSdialog.show();
        }
    }

    private void showErrorDialogNetwork() {
        if (this.networkDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(WalkthroughActivity.this);
            builder.setTitle(R.string.title_no_network)
                    .setIcon(R.drawable.ic_error)
                    .setMessage(R.string.description_no_network)
                    .setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
                        }
                    });
            this.networkDialog = builder.create();
        }
        if (!this.networkDialog.isShowing()) {
            this.networkDialog.show();
        }
    }

    private class AsyncDownload extends AsyncTask<Location, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Location... params) {
            if (Global.isNetworkAvailable()) {
                try {
                    ParseGeoPoint currentLocation = new ParseGeoPoint(params[0].getLatitude(), params[0].getLongitude());
                    List<ParseObject> segnalations = new Segnalazione(WalkthroughActivity.this).updateWithOnline(currentLocation);
                    new Valutazione(WalkthroughActivity.this).updateWithOnline(segnalations);
                    Set<String> usersId = new HashSet<>();
                    for (ParseObject currentSegnalation : segnalations) {
                        usersId.add(((ParseObject) currentSegnalation.get(Segnalazione.user)).getObjectId());
                    }
                    new Utente(WalkthroughActivity.this).updateWithOnline(usersId);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("WALKTHROUGH", "onPostExecute");
            if (!result){
                if (networkDialog != null && !networkDialog.isShowing()) {
                    showErrorDialogNetwork();
                }
                if (loading != null && loading.isShowing()) {
                    loading.dismiss();
                }
                asyncDownload = null;
            }else{
                if (loading != null && loading.isShowing()) {
                    WalkthroughActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loading.dismiss();
                            Intent intent = new Intent(WalkthroughActivity.this, MapActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                }
                finishDownload = true;
            }
        }
    }
}
