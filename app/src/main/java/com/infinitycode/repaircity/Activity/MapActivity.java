package com.infinitycode.repaircity.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.NavigationDrawerFragment;
import com.infinitycode.repaircity.R;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.parse.Utente;
import com.infinitycode.repaircity.parse.Valutazione;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Infinity Code on 11/08/2015.
 */

public class MapActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = MapActivity.class.getSimpleName();
    private static final int ACTIVITY_NUMBER = 0;
    private static final int SYNC_SEGNALATIONS = 1;
    private static final int SYNC_USERS = 2;


    private FloatingActionButton fabLocalize;
    private FloatingActionMenu menu;
    private NavigationDrawerFragment drawerFragment;
    private ProgressDialog loading;
    private AlertDialog GPSdialog;
    private AlertDialog networkDialog;

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location currentLocation;
    private Location mapCenterLocation;
    private final Map<String, String> markersExtraValues = new HashMap<>();
    private final Map<String, Marker> visibleMarkers = new HashMap<>();
    private final Set<String> removableSegnalation = new HashSet<>();
    private boolean userForcedSync = false;
    private Location lastSyncPosition;
    private Boolean firstCenterMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        setUpMapIfNeeded();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)     // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds

        new Global(MapActivity.this).writeIntInPreferences(Global.keyPreferencesCurrentActivity, ACTIVITY_NUMBER);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        this.drawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map_drawer);
        this.drawerFragment.setUp(R.id.fragment_map_drawer, (DrawerLayout) findViewById(R.id.drawer_layout_map), toolbar);

        this.fabLocalize = (FloatingActionButton) findViewById(R.id.action_button_localize);

        FloatingActionButton fabAddSegnalation = (FloatingActionButton) findViewById(R.id.action_button_add_map);
        fabAddSegnalation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(getApplicationContext(), AddActivity.class);
                startActivity(intent);
            }
        });

        this.menu = (FloatingActionMenu) findViewById(R.id.map_mode_fab);
        FloatingActionButton fabSatellite = (FloatingActionButton) findViewById(R.id.fab_map_satellite);
        FloatingActionButton fabHybrid = (FloatingActionButton) findViewById(R.id.fab_map_hybrid);
        FloatingActionButton fabStandard = (FloatingActionButton) findViewById(R.id.fab_map_standard);

        int mapType = new Global(getApplicationContext()).getIntFromPreferences(Global.keyPreferencesMap);
        if (mapType == 1 || mapType == 2 || mapType == 4) {
            this.mMap.setMapType(new Global(getApplicationContext()).getIntFromPreferences(Global.keyPreferencesMap));
        } else {
            this.mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        fabSatellite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.getMapType() != GoogleMap.MAP_TYPE_SATELLITE) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    new Global(getApplicationContext()).writeIntInPreferences(Global.keyPreferencesMap, GoogleMap.MAP_TYPE_SATELLITE);
                }
                menu.close(true);
            }
        });

        fabHybrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.getMapType() != GoogleMap.MAP_TYPE_HYBRID) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    new Global(getApplicationContext()).writeIntInPreferences(Global.keyPreferencesMap, GoogleMap.MAP_TYPE_HYBRID);
                }
                menu.close(true);
            }
        });

        fabStandard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.getMapType() != GoogleMap.MAP_TYPE_NORMAL) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    new Global(getApplicationContext()).writeIntInPreferences(Global.keyPreferencesMap, GoogleMap.MAP_TYPE_NORMAL);
                }
                menu.close(true);
            }
        });

        this.mMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.mMap.getUiSettings().setMapToolbarEnabled(false);

        this.mapCenterLocation = new Location("MapCenter");

        this.fabLocalize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (new Global(MapActivity.this).checkGeolocalization()) {
                    if (currentLocation != null) {
                        handleNewLocation(currentLocation);
                    }
                } else {
                    showErrorDialogGPS();
                }
            }
        });

        this.mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                LatLng latLng = cameraPosition.target;
                mapCenterLocation.setLatitude(latLng.latitude);
                mapCenterLocation.setLongitude(latLng.longitude);

                if (currentLocation != null) {
                    float distance = currentLocation.distanceTo(mapCenterLocation);
                    if (distance < 75) {
                        fabLocalize.setImageResource(R.drawable.ic_center_direction);
                    } else {
                        fabLocalize.setImageResource(R.drawable.ic_center_direction_gray);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MapActivity", "onResume");
        setUpMapIfNeeded();
        new Global(MapActivity.this).writeIntInPreferences(Global.keyPreferencesCurrentActivity, ACTIVITY_NUMBER);
        if (new Global(MapActivity.this).checkGeolocalization()) {
            mGoogleApiClient.connect();
            if (new Global(MapActivity.this).getBooleanFromPreferences(Global.keyPreferencesForcedSync)) {
                loading = ProgressDialog.show(MapActivity.this, null, getString(R.string.wait_forced_sync), true, false);
                new SyncAllWithOnline().execute();
            }
        } else {
            showErrorDialogGPS();
        }

        if (new Global(MapActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncSegnalation) && !new Global(MapActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncSegnalation)) {
            Log.d("MapActivity", "Ci sono segnalazioni locali");
            new SyncLocalData().execute(SYNC_SEGNALATIONS);
        }
        if (new Global(MapActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncUser) && !new Global(MapActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncUser)) {
            Log.d("MapActivity", "Ci sono utenti locali");
            new SyncLocalData().execute(SYNC_USERS);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MapActivity", "onPause");
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onBackPressed() {
        if (this.drawerFragment.isVisible()) {
            this.drawerFragment.closeDrawer();
        } else {
            super.onBackPressed();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.refresh) {
            if (new Global(MapActivity.this).checkGeolocalization()) {
                if (!new Global(MapActivity.this).getBooleanFromPreferences(Global.keyPreferencesForcedSync)) {
                    if (currentLocation != null && !this.userForcedSync) {
                        this.userForcedSync = true;
                        new SyncAllWithOnline().execute();
                    } else {
                        Toast.makeText(MapActivity.this, R.string.wait_sync, Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                showErrorDialogGPS();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            this.mMap.setMyLocationEnabled(true);
        } else {

            this.mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    if (!userForcedSync) {
                        Intent intent = new Intent(MapActivity.this, DetailActivity.class);
                        intent.putExtra("segnalationId", markersExtraValues.get(marker.getId()));
                        startActivity(intent);
                    } else {
                        Toast.makeText(MapActivity.this, R.string.wait_sync, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void handleNewLocation(Location location) {
        Log.d("MapActivity", "handleNewLocation");
        Log.d("MapActivity", location.toString());

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        float zoomLevel = 16.0f; //This goes up to 21
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("MapActivity", "onConnected");
        final Location location = LocationServices.FusedLocationApi.getLastLocation(this.mGoogleApiClient);

        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(this.mGoogleApiClient, this.mLocationRequest, this);
        } else {
            if (this.lastSyncPosition == null) {
                Log.d("MapActivity", "lastSync");
                this.lastSyncPosition = location;
            }
            this.currentLocation = location;
            new CreationMaker().execute();
            if (!this.firstCenterMap) {
                handleNewLocation(location);
                this.firstCenterMap = true;
            }

        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("MapActivity", "onConnectionSuspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("MapActivity", "onConnectionFailed");
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
        } else {
        /*
         * If no resolution is available, display a dialog to the
         * user with the error.
         */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("MapActicity", "onLocationChanged");
        this.currentLocation = location;

        if (!this.firstCenterMap) {
            new CreationMaker().execute();
            handleNewLocation(location);
            this.firstCenterMap = true;
        }

        if (this.lastSyncPosition != null) {
            float distance = this.lastSyncPosition.distanceTo(location);
            if (distance > 2000) {
                lastSyncPosition = location;
                if (!this.userForcedSync) {
                    this.userForcedSync = true;
                    new SyncAllWithOnline().execute();
                }
            }
        }

    }

    private void showErrorDialogGPS() {
        if (this.GPSdialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
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
            this.firstCenterMap = false;
            this.GPSdialog.show();
        }
    }

    private void showErrorDialogNetwork() {
        if (this.networkDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
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

    private class CreationMaker extends AsyncTask<Void, Void, List<ParseObject>> {

        @Override
        protected List<ParseObject> doInBackground(Void... params) {
            ParseGeoPoint parseGeoPoint = new ParseGeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
            Iterator<String> visibleSegnalationsID = visibleMarkers.keySet().iterator();
            //noinspection WhileLoopReplaceableByForEach
            while (visibleSegnalationsID.hasNext()) {
                removableSegnalation.add(visibleSegnalationsID.next());
            }
            return new Segnalazione(MapActivity.this).getSegnalationsInRange(parseGeoPoint);
        }

        @Override
        protected void onPostExecute(List<ParseObject> parseObjects) {
            super.onPostExecute(parseObjects);
            if (parseObjects != null) {
                Set<String> newSegnalations = new HashSet<>();
                for (ParseObject segnalation : parseObjects) {
                    String segnalationID = segnalation.getObjectId();
                    newSegnalations.add(segnalationID);
                    ParseGeoPoint segnalationPosition = segnalation.getParseGeoPoint(Segnalazione.position);
                    LatLng mapPosition = new LatLng(segnalationPosition.getLatitude(), segnalationPosition.getLongitude());
                    String title = segnalation.getString(Segnalazione.title);
                    String description = segnalation.getString(Segnalazione.description);
                    if (description.length() > 40) {
                        description = description.substring(0, 39);
                        description += "...";
                    }
                    int gravity = segnalation.getInt(Segnalazione.priority);
                    MarkerOptions markerOptions = new MarkerOptions().position(mapPosition).title(title).snippet(description);
                    switch (gravity) {
                        case 1:
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_yellow_marker));
                            break;
                        case 2:
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_orange_marker));
                            break;
                        case 3:
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_red_marker));
                            break;
                    }
                    if (!visibleMarkers.containsKey(segnalationID)) {
                        Marker marker = mMap.addMarker(markerOptions);
                        markersExtraValues.put(marker.getId(), segnalationID);
                        visibleMarkers.put(segnalationID, marker);
                        Log.d("CreationMaker", "Creato marker " + segnalationID + " " + visibleMarkers.size() + " " + visibleMarkers.containsKey(segnalationID));
                    }
                }

                removableSegnalation.removeAll(newSegnalations);
                for (String oldSegnalation : removableSegnalation) {
                    Marker oldMarker = visibleMarkers.get(oldSegnalation);
                    if (oldMarker != null) {
                        markersExtraValues.remove(oldMarker.getId());
                        oldMarker.remove();
                        visibleMarkers.remove(oldSegnalation);
                        Log.d("CreationMaker", "Eliminato un marker " + oldSegnalation);
                    }
                }
            }
            Log.d("CreationMaker", "Finita sync " + visibleMarkers.size() + " " + markersExtraValues.size());
        }
    }

    private class SyncAllWithOnline extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            if (Global.isNetworkAvailable()) {
                try {
                    if ((loading != null && !loading.isShowing()) || loading == null) {
                        MapActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MapActivity.this, R.string.forced_sync_message, Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                    lastSyncPosition = currentLocation;
                    final List<ParseObject> segnalations = new Segnalazione(getApplicationContext()).updateWithOnline(new ParseGeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
                    final Set<String> setUsers = new HashSet<>();
                    for (final ParseObject segnalation : segnalations) {
                        final ParseObject user = segnalation.getParseObject(Segnalazione.user);
                        setUsers.add(user.getObjectId());
                    }
                    new Utente(getApplicationContext()).updateWithOnline(setUsers);
                    new Valutazione(getApplicationContext()).updateWithOnline(segnalations);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (loading != null && loading.isShowing()) {
                MapActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.dismiss();
                    }
                });
            }
            if (userForcedSync) {
                userForcedSync = false;
            }

            if (result) {
                new CreationMaker().execute();
                Toast.makeText(MapActivity.this, R.string.finished_sync, Toast.LENGTH_LONG).show();
                new Global(MapActivity.this).writeBooleanInPreferences(Global.keyPreferencesForcedSync, false);
            } else {
                showErrorDialogNetwork();
            }

            mGoogleApiClient.connect();
        }
    }

    private class SyncLocalData extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {

            if (Global.isNetworkAvailable()) {
                switch (params[0]) {
                    case SYNC_SEGNALATIONS:
                        Log.d("MapActivity", "Avviata sync segnalazioni locali");
                        new Global(MapActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, true);
                        new Segnalazione(MapActivity.this).syncingLocalChanges();
                        break;
                    case SYNC_USERS:
                        Log.d("MapActivity", "Avviata sync utenti locali");
                        new Global(MapActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, true);
                        new Utente(MapActivity.this).syncingLocalChanges();
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
                    new Global(MapActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, false);
                    break;
                case SYNC_USERS:
                    Log.d("MapActivity", "Conclusa sync utenti locali");
                    new Global(MapActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, false);
                    break;
            }
        }
    }
}
