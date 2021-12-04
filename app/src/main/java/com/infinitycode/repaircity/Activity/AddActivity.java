package com.infinitycode.repaircity.activity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.NavigationDrawerFragment;
import com.infinitycode.repaircity.R;
import com.infinitycode.repaircity.adapters.GridDialogAdapter;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.parse.Utente;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.GridHolder;
import com.orhanobut.dialogplus.Holder;
import com.orhanobut.dialogplus.OnItemClickListener;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.pkmmte.view.CircularImageView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Infinity Code on 11/08/2015.
 */

public class AddActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final int TAKE_PHOTO = 1;
    private final int CHOOSE_PHOTO = 2;
    private static final int ACTIVITY_NUMBER = 2;
    private static final int SYNC_SEGNALATIONS = 1;
    private static final int SYNC_USERS = 2;
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private NavigationDrawerFragment drawerFragment;

    private Button bassa;
    private Button media;
    private Button alta;
    private int selectedPriority;

    private boolean selectedImage = false;
    private ImageView imageView;
    private final ByteArrayOutputStream imageStream = new ByteArrayOutputStream();

    private CircularImageView userImageView;
    private TextView userNameView;
    private TextView viewTitle;
    private EditText viewDescription;

    private ParseObject currentUser;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location location;
    private ProgressDialog loading;
    private Uri imageUri;
    private ScrollView scrollView;
    private final int[] descriptionLocation = new int[2];

    private AlertDialog GPSdialog;
    private AlertDialog networkDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        this.scrollView = (ScrollView) findViewById(R.id.scroll_add);

        this.userImageView = (CircularImageView) findViewById(R.id.user_image_add);
        this.userNameView = (TextView) findViewById(R.id.user_name_add);

        this.imageView = (ImageView) findViewById(R.id.segnalation_photo_add);
        this.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyBoard();
                showAddPhotoDialog();
            }
        });
        this.imageView.setImageResource(R.drawable.add_image);

        this.viewTitle = (TextView) findViewById(R.id.titolo_add);
        this.viewDescription = (EditText) findViewById(R.id.descrizione_add);
        this.viewTitle.requestFocus();

        this.viewDescription.getLocationInWindow(this.descriptionLocation);
        this.viewDescription.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    scrollView.scrollTo(descriptionLocation[0], descriptionLocation[1] + 200);
                }
            }
        });
        setUpMapIfNeeded();

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        new Global(AddActivity.this).writeIntInPreferences(Global.keyPreferencesCurrentActivity, ACTIVITY_NUMBER);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        this.drawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_add_drawer);
        this.drawerFragment.setUp(R.id.fragment_add_drawer, (DrawerLayout) findViewById(R.id.drawer_layout_add), toolbar);

        //per rendere fissa la mappa
        this.mMap.getUiSettings().setScrollGesturesEnabled(false);
        this.mMap.getUiSettings().setTiltGesturesEnabled(false);
        this.mMap.getUiSettings().setRotateGesturesEnabled(false);
        this.mMap.getUiSettings().setZoomGesturesEnabled(false);
        this.mMap.getUiSettings().setCompassEnabled(false);
        this.mMap.getUiSettings().setMyLocationButtonEnabled(false);

        int mapType = new Global(getApplicationContext()).getIntFromPreferences(Global.keyPreferencesMap);
        if (mapType == 1 || mapType == 2 || mapType == 4) {
            this.mMap.setMapType(new Global(getApplicationContext()).getIntFromPreferences(Global.keyPreferencesMap));
        } else {
            this.mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        //setto l'alpha per i bottoni priorità
        this.bassa = (Button) findViewById(R.id.bassa_add);
        this.selectedPriority = 1;
        this.media = (Button) findViewById(R.id.media_add);
        this.media.setAlpha((float) 0.3);
        this.alta = (Button) findViewById(R.id.alta_add);
        this.alta.setAlpha((float) 0.3);

        //setto i listener per i bottoni priorità
        this.bassa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyBoard();
                selectedPriority = 1;
                bassa.setAlpha((float) 1);
                media.setAlpha((float) 0.3);
                alta.setAlpha((float) 0.3);
            }
        });
        this.media.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyBoard();
                selectedPriority = 2;
                bassa.setAlpha((float) 0.3);
                media.setAlpha((float) 1);
                alta.setAlpha((float) 0.3);
            }
        });
        this.alta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyBoard();
                selectedPriority = 3;
                media.setAlpha((float) 0.3);
                bassa.setAlpha((float) 0.3);
                alta.setAlpha((float) 1);
            }
        });

        new PrepareUI().execute(new Global(AddActivity.this).getStringFromPreferences(Global.keyPreferencesFB));

        if (new Global(AddActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncSegnalation) && !new Global(AddActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncSegnalation)) {
            Log.d("MapActivity", "Ci sono segnalazioni locali");
            new SyncLocalData().execute(SYNC_SEGNALATIONS);
        }
        if (new Global(AddActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncUser) && !new Global(AddActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncUser)) {
            Log.d("MapActivity", "Ci sono utenti locali");
            new SyncLocalData().execute(SYNC_USERS);
        }

        if (!new Global(AddActivity.this).checkGeolocalization()) {
            showErrorDialogGPS();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.setUpMapIfNeeded();
        this.mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (this.mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(this.mGoogleApiClient, this);
            this.mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.add_segnalation) {
            this.hideSoftKeyBoard();

            if (new Global(AddActivity.this).checkGeolocalization() && location != null) {
                if (this.viewDescription.getText().toString().length() > 0 && this.viewTitle.getText().toString().length() > 0 && this.selectedImage) {
                    new SaveSegnalation().execute(this.viewTitle.getText().toString(), this.viewDescription.getText().toString());

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
                    builder.setTitle(R.string.warning_title)
                            .setIcon(R.drawable.ic_warning)
                            .setMessage(R.string.warning_description)
                            .setPositiveButton(R.string.text_ok_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    builder.create().show();
                }
            } else {
                showErrorDialogGPS();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (this.drawerFragment.isVisible()) {
            Log.d("ADD", "navigation fragment visible");
            this.drawerFragment.closeDrawer();
        } else {
            super.onBackPressed();
            this.finish();
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (this.mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            this.mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_add)).getMap();
            this.mMap.setMyLocationEnabled(true);
        }
    }

    private void handleNewLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        float zoomLevel = 17.0f; //This goes up to 21
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onConnected(Bundle bundle) {
        this.location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (this.location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(this.mGoogleApiClient, this.mLocationRequest, this);
        } else {
            handleNewLocation(this.location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
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
        Log.d("AddActivity","onLocationChanged");
        Toast.makeText(AddActivity.this,"onLocationChanged",Toast.LENGTH_SHORT).show();
        this.location = location;
        handleNewLocation(location);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            try {
                String imagePath = getRealPathFromURI(this.imageUri);
                ExifInterface ei = new ExifInterface(imagePath);

                String orientString = ei.getAttribute(ExifInterface.TAG_ORIENTATION);
                Log.d("TEST", "orientString: " + orientString);

                int orientation2 = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
                Log.d("TEST", "orientation: " + orientation2);

                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        Picasso.with(AddActivity.this).load(this.imageUri).placeholder(R.drawable.loading_image).rotate(90).transform(new CustomTrasformation()).into(this.imageView);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        Picasso.with(AddActivity.this).load(this.imageUri).placeholder(R.drawable.loading_image).rotate(180).transform(new CustomTrasformation()).into(this.imageView);
                        break;
                }
                this.selectedImage = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == CHOOSE_PHOTO && resultCode == RESULT_OK && data != null) {
            this.imageUri = data.getData();
            Picasso.with(AddActivity.this).load(this.imageUri).placeholder(R.drawable.loading_image).transform(new CustomTrasformation()).into(this.imageView);
            this.selectedImage = true;
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] values = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, values, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    private void showAddPhotoDialog() {
        GridDialogAdapter adapter = new GridDialogAdapter(AddActivity.this);
        Holder holder = new GridHolder(2);
        DialogPlus dialog = DialogPlus.newDialog(AddActivity.this)
                .setContentHolder(holder)
                .setAdapter(adapter)
                .setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(DialogPlus dialog, Object item, View view, int position) {
                        if (position == 0) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.TITLE, "Segnalation Photo");
                            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                            dialog.dismiss();
                            startActivityForResult(takePicture, TAKE_PHOTO);
                        } else {
                            Intent choose = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            dialog.dismiss();
                            startActivityForResult(choose, CHOOSE_PHOTO);
                        }
                    }
                })
                .setCancelable(true)
                .setHeader(R.layout.dialog_grid_header)
                .setGravity(Gravity.BOTTOM)
                .create();
        dialog.show();
    }

    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (imm.isAcceptingText() && getCurrentFocus() != null) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showErrorDialogGPS() {
        if (this.GPSdialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
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

    private class CustomTrasformation implements Transformation {

        @Override
        public Bitmap transform(Bitmap source) {
            int x = source.getWidth() / 2;
            int y = source.getHeight() / 2;
            Bitmap result = Bitmap.createScaledBitmap(source, x, y, false);
            source.recycle();
            result.compress(Bitmap.CompressFormat.JPEG, 30, imageStream);
            return result;
        }

        @Override
        public String key() {
            return "custom()";
        }
    }

    private class SaveSegnalation extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            if (Global.isNetworkAvailable()) {

                AddActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading = ProgressDialog.show(AddActivity.this, null, getString(R.string.upload_message), true, false);
                    }
                });
                String strTitle = params[0];
                String strDescription = params[1];
                byte[] imageData = imageStream.toByteArray();
                return new Segnalazione(getApplicationContext()).insert(strTitle, strDescription, selectedPriority, location.getLatitude(), location.getLongitude(), imageData, currentUser);

            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (loading != null && loading.isShowing()) {
                loading.dismiss();
            }
            if (result != null) {
                if (result) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
                    builder.setTitle(R.string.online_upload_title)
                            .setIcon(R.drawable.ic_success)
                            .setMessage(R.string.online_upload_message)
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    onBackPressed();
                                }
                            })
                            .setPositiveButton(R.string.text_ok_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    onBackPressed();
                                }
                            });
                    builder.create().show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
                    builder.setTitle(R.string.local_upload_title)
                            .setIcon(R.drawable.ic_info)
                            .setMessage(R.string.local_upload_message)
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    onBackPressed();
                                }
                            })
                            .setPositiveButton(R.string.text_ok_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    onBackPressed();
                                }
                            });
                    builder.create().show();
                }
            } else {
                showErrorDialogNetwork();
            }
            super.onPostExecute(result);
        }
    }

    private class PrepareUI extends AsyncTask<String, Void, List<Object>> {

        @Override
        protected List<Object> doInBackground(String... params) {
            List<Object> result = new ArrayList<>();
            if (params[0] != null) {
                try {
                    currentUser = new Utente(AddActivity.this).getLocalUserObject(params[0]);
                    result.add(currentUser.get(Utente.name));
                    result.add(new Utente(AddActivity.this).getUserProfilePhoto(currentUser));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<Object> list) {
            if (list.get(0) != null) {
                userNameView.setText((String) list.get(0));
            }
            if (list.get(1) != null) {
                userImageView.setImageBitmap((Bitmap) list.get(1));
            }
            super.onPostExecute(list);
        }
    }

    private class SyncLocalData extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {

            if (Global.isNetworkAvailable()) {
                switch (params[0]) {
                    case SYNC_SEGNALATIONS:
                        Log.d("MapActivity", "Avviata sync segnalazioni locali");
                        new Global(AddActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, true);
                        new Segnalazione(AddActivity.this).syncingLocalChanges();
                        break;
                    case SYNC_USERS:
                        Log.d("MapActivity", "Avviata sync utenti locali");
                        new Global(AddActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, true);
                        new Utente(AddActivity.this).syncingLocalChanges();
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
                    new Global(AddActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, false);
                    break;
                case SYNC_USERS:
                    Log.d("MapActivity", "Conclusa sync utenti locali");
                    new Global(AddActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, false);
                    break;
            }
        }
    }
}