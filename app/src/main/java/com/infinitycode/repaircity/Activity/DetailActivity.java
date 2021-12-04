package com.infinitycode.repaircity.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.R;
import com.infinitycode.repaircity.adapters.ListDialogAdapter;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.parse.Utente;
import com.infinitycode.repaircity.parse.Valutazione;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.Holder;
import com.orhanobut.dialogplus.ListHolder;
import com.orhanobut.dialogplus.OnItemClickListener;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.pkmmte.view.CircularImageView;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Infinity Code on 11/08/2015.
 */

public class DetailActivity extends AppCompatActivity {

    private static final String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";
    private static final String TWITTER_PACKAGE_NAME = "com.twitter.android";
    private static final String USER_IMAGE = "user";
    private static final String SEGNALATION_IMAGE = "segnalation";

    private ParseObject segnalationObject;
    private ParseObject currUser;
    private Bitmap userProfilePhoto;
    private Bitmap segnalationPhoto;
    private Boolean segnalationResolved;

    private Toolbar toolbar;
    private CircularImageView userImageView;
    private ImageView segnalationImage;
    private TextView userNameView;
    private TextView viewDescription;
    private ImageView likeImg;
    private ImageView notLikeImg;
    private TextView lastUpdateDate;
    private TextView likesNumber;
    private TextView notLikesNumber;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private DialogPlus dialog;
    private AlertDialog GPSdialog;
    private AlertDialog networkDialog;

    private int valutationType;
    private int like;
    private int notLike;

    private PrepareUIImages asyncTaskImageUser;
    private PrepareUIImages asyncTaskImageSegnalation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        this.toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(this.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.setUpMapIfNeeded();

        //per rendere fissa la mappa
        this.mMap.getUiSettings().setScrollGesturesEnabled(false);
        this.mMap.getUiSettings().setTiltGesturesEnabled(false);
        this.mMap.getUiSettings().setRotateGesturesEnabled(false);
        this.mMap.getUiSettings().setZoomGesturesEnabled(false);
        this.mMap.getUiSettings().setCompassEnabled(false);
        this.mMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.mMap.getUiSettings().setMapToolbarEnabled(false);

        int mapType = new Global(getApplicationContext()).getIntFromPreferences(Global.keyPreferencesMap);
        if (mapType == 1 || mapType == 2 || mapType == 4) {
            this.mMap.setMapType(new Global(getApplicationContext()).getIntFromPreferences(Global.keyPreferencesMap));
        } else {
            this.mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        this.viewDescription = (TextView) findViewById(R.id.descrizione_detail);
        this.likesNumber = (TextView) findViewById(R.id.detail_likes_number);
        this.notLikesNumber = (TextView) findViewById(R.id.detail_notlikes_number);
        this.lastUpdateDate = (TextView) findViewById(R.id.detail_last_update_date);
        this.segnalationImage = (ImageView) findViewById(R.id.segnalation_photo_detail);
        this.userImageView = (CircularImageView) findViewById(R.id.user_image_detail);
        this.userNameView = (TextView) findViewById(R.id.user_name_detail);
        this.likeImg = (ImageView) findViewById(R.id.like_image);
        this.notLikeImg = (ImageView) findViewById(R.id.not_like_image);

        this.likeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LikeSegnalation().execute(0);
            }
        });

        this.notLikeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LikeSegnalation().execute(1);
            }
        });

        new PrepareUI().execute(getIntent().getStringExtra("segnalationId"));
        this.asyncTaskImageUser = new PrepareUIImages();
        this.asyncTaskImageUser.execute(getIntent().getStringExtra("segnalationId"), USER_IMAGE);
        this.asyncTaskImageSegnalation = new PrepareUIImages();
        this.asyncTaskImageSegnalation.execute(getIntent().getStringExtra("segnalationId"), SEGNALATION_IMAGE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.options) {
            if (this.segnalationResolved != null) {
                showOptionsDialog();
            }
        }

        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.segnalationImage.setImageDrawable(null);
        this.userImageView.setImageResource(R.drawable.ic_default_user);
        if (userProfilePhoto != null) {
            this.userProfilePhoto.recycle();
        }
        if (segnalationPhoto != null) {
            this.segnalationPhoto.recycle();
        }

        if (asyncTaskImageSegnalation != null) {
            this.asyncTaskImageSegnalation.cancel(true);
        }
        if (asyncTaskImageUser != null) {
            this.asyncTaskImageUser.cancel(true);
        }

        this.finish();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (this.mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            this.mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_detail)).getMap();
        }
    }

    private void showOptionsDialog() {
        ListDialogAdapter adapter = new ListDialogAdapter(DetailActivity.this, this.segnalationResolved);
        Holder holder = new ListHolder();
        this.dialog = DialogPlus.newDialog(DetailActivity.this)
                .setContentHolder(holder)
                .setAdapter(adapter)
                .setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(DialogPlus dialog, Object item, View view, int position) {
                        switch (position) {
                            case 0:
                                if (segnalationResolved) {
                                    showFacebookShare();
                                } else {
                                    new SolveSegnalation().execute();
                                }
                                break;
                            case 1:
                                if (segnalationResolved) {
                                    showTwitterShare();
                                } else {
                                    showFacebookShare();
                                }
                                break;
                            case 2:
                                showTwitterShare();
                                break;
                        }
                    }
                })
                .setCancelable(true)
                .setGravity(Gravity.BOTTOM)
                .create();
        this.dialog.show();
    }

    private boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void showFacebookShare() {
        if (isPackageInstalled(FACEBOOK_PACKAGE_NAME, getApplicationContext())) {
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(((BitmapDrawable) segnalationImage.getDrawable()).getBitmap())
                    .build();
            SharePhotoContent content = new SharePhotoContent.Builder()
                    .addPhoto(photo)
                    .build();

            ShareDialog.show(DetailActivity.this, content);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
            builder.setTitle(R.string.warning_title)
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(R.string.facebook_not_installed)
                    .setPositiveButton(R.string.text_ok_button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            builder.create().show();
        }
        dialog.dismiss();
    }

    private void showTwitterShare() {
        final Intent tweetIntent = new Intent(Intent.ACTION_SEND);
        final String tweetMessage = toolbar.getTitle().toString();
        tweetIntent.putExtra(Intent.EXTRA_TEXT, tweetMessage);
        tweetIntent.setType("image/*");
        tweetIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        boolean resolved = false;
        final List<ResolveInfo> resolvedInfoList = DetailActivity.this.getPackageManager().queryIntentActivities(tweetIntent, 0);
        for (final ResolveInfo resolveInfo : resolvedInfoList) {
            if (resolveInfo.activityInfo.packageName.startsWith(TWITTER_PACKAGE_NAME)) {
                tweetIntent.setClassName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name);
                resolved = true;
                break;
            }
        }
        if (resolved) {
            final Bitmap tweetImage = ((BitmapDrawable) segnalationImage.getDrawable()).getBitmap();
            tweetIntent.putExtra(Intent.EXTRA_STREAM, getImageUri(getApplicationContext(), tweetImage));
            startActivity(tweetIntent);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
            builder.setTitle(R.string.warning_title)
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(R.string.twitter_not_installed)
                    .setPositiveButton(R.string.text_ok_button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            builder.create().show();
        }
        dialog.dismiss();
    }

    private void showErrorDialogNetwork() {
        if (this.networkDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
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

    private class PrepareUI extends AsyncTask<String, Void, Void> {

        private int numberOfLikes;
        private int numberOfDislikes;
        private ParseGeoPoint segnalationPosition;
        private String title;
        private String description;
        private String lastUpdate;
        private String segnalationUserName;
        private String segnalationUserID;
        private int valutation;

        @Override
        protected Void doInBackground(String... params) {
            try {
                segnalationObject = new Segnalazione(getApplicationContext()).getSegnalationFromObjectID(params[0]);

                segnalationResolved = segnalationObject.getBoolean(Segnalazione.solved);
                title = segnalationObject.getString(Segnalazione.title);
                description = segnalationObject.getString(Segnalazione.description);

                @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                lastUpdate = df.format(segnalationObject.getUpdatedAt());

                segnalationPosition = (ParseGeoPoint) segnalationObject.get(Segnalazione.position);
                final LatLng mapPosition = new LatLng(segnalationPosition.getLatitude(), segnalationPosition.getLongitude());

                int gravity = segnalationObject.getInt(Segnalazione.priority);
                final MarkerOptions markerOptions = new MarkerOptions().position(mapPosition);
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

                DetailActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMap.addMarker(markerOptions);
                        float zoomLevel = 16.0f; //This goes up to 21
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mapPosition, zoomLevel);
                        mMap.animateCamera(cameraUpdate);
                    }
                });

                numberOfLikes = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, 1);
                numberOfDislikes = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, -1);

                segnalationUserID = ((ParseObject) segnalationObject.get(Segnalazione.user)).getObjectId();
                ParseObject userSegnalation = new Utente(getApplicationContext()).getUserFromObjectID(segnalationUserID);
                segnalationUserName = userSegnalation.getString(Utente.name);

                currUser = new Utente(getApplicationContext()).getLocalUserObject(new Global(getApplicationContext()).getStringFromPreferences(Global.keyPreferencesFB));
                this.valutation = new Valutazione(getApplicationContext()).getValutationType(currUser, segnalationObject);

            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            toolbar.setTitle(title);
            viewDescription.setText(description);
            userNameView.setText(segnalationUserName);
            likesNumber.setText("" + numberOfLikes);
            notLikesNumber.setText("" + numberOfDislikes);
            lastUpdateDate.setText("Data ultima modifica: " + lastUpdate);

            if (this.valutation == 1) {
                likeImg.setImageResource(R.drawable.ic_thumbs_up_green);
            } else if (this.valutation == -1) {
                notLikeImg.setImageResource(R.drawable.ic_thumbs_down_green);
            }
        }
    }

    @SuppressWarnings("EmptyMethod")
    private class PrepareUIImages extends AsyncTask<String, Void, String> {

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                segnalationObject = new Segnalazione(getApplicationContext()).getSegnalationFromObjectID(params[0]);
                String segnalationUserID = ((ParseObject) segnalationObject.get(Segnalazione.user)).getObjectId();

                switch (params[1]) {
                    case USER_IMAGE:
                        ParseObject userSegnalation = new Utente(getApplicationContext()).getUserFromObjectID(segnalationUserID);
                        userProfilePhoto = new Utente(getApplicationContext()).getUserProfilePhoto(userSegnalation);
                        break;
                    case SEGNALATION_IMAGE:
                        final ParseFile segnalationImageFile = segnalationObject.getParseFile(Segnalazione.segnalationPhoto);
                        byte[] imageData = segnalationImageFile.getData();
                        segnalationPhoto = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                        break;
                }
            } catch (ParseException e) {
                e.printStackTrace();

            }
            return params[1];
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            switch (result) {
                case USER_IMAGE:
                    if (userProfilePhoto != null && !userProfilePhoto.isRecycled()) {
                        userImageView.setImageBitmap(userProfilePhoto);
                    } else {
                        showErrorDialogNetwork();
                    }
                    break;
                case SEGNALATION_IMAGE:
                    if (segnalationPhoto != null && !segnalationPhoto.isRecycled()) {
                        segnalationImage.setImageBitmap(segnalationPhoto);
                    } else {
                        segnalationImage.setImageResource(R.drawable.error_image);
                        showErrorDialogNetwork();
                    }
                    break;
            }
        }
    }

    private class SolveSegnalation extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(final Void... params) {
            if (Global.isNetworkAvailable()) {
                new Segnalazione(getApplicationContext()).updateSolved(segnalationObject.getObjectId(), true);
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);
            dialog.dismiss();
            if (result) {
                Toast.makeText(DetailActivity.this, R.string.resolved_segnalation, Toast.LENGTH_LONG).show();
                onBackPressed();
            } else {
                showErrorDialogNetwork();
            }
        }
    }

    private class LikeSegnalation extends AsyncTask<Integer, Void, Boolean> {

        private int buttonPressed;

        @Override
        protected Boolean doInBackground(final Integer... params) {
            if (Global.isNetworkAvailable()) {
                try {
                    this.buttonPressed = params[0];
                    new Valutazione(getApplicationContext()).checkValutationExistOnline(currUser, segnalationObject);
                    valutationType = new Valutazione(getApplicationContext()).getValutationType(currUser, segnalationObject);
                    return true;
                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Boolean exists) {
            super.onPostExecute(exists);
            if (exists == null) {
                showErrorDialogNetwork();
            } else if (exists) {
                if (this.buttonPressed == 0) {
                    new UpdateValutation().execute(0);
                } else {
                    new UpdateValutation().execute(1);
                }
            } else {
                if (this.buttonPressed == 0) {
                    new InsertValutation().execute(1);
                } else {
                    new InsertValutation().execute(-1);
                }
            }
        }
    }

    private class UpdateValutation extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            if (params[0] == 0) {
                if (valutationType == 1) {
                    new Valutazione(getApplicationContext()).updateValutation(currUser, segnalationObject, 0);
                    like = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, 1);
                    notLike = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, -1);
                    return 0;
                } else if (valutationType == 0) {
                    new Valutazione(getApplicationContext()).updateValutation(currUser, segnalationObject, 1);
                    like = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, 1);
                    notLike = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, -1);
                    return 1;
                } else {
                    new Valutazione(getApplicationContext()).updateValutation(currUser, segnalationObject, 1);
                    like = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, 1);
                    notLike = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, -1);
                    return 2;
                }
            } else {
                if (valutationType == -1) {
                    new Valutazione(getApplicationContext()).updateValutation(currUser, segnalationObject, 0);
                    like = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, 1);
                    notLike = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, -1);
                    return 3;
                } else if (valutationType == 0) {
                    new Valutazione(getApplicationContext()).updateValutation(currUser, segnalationObject, -1);
                    like = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, 1);
                    notLike = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, -1);
                    return 4;
                } else {
                    new Valutazione(getApplicationContext()).updateValutation(currUser, segnalationObject, -1);
                    like = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, 1);
                    notLike = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, -1);
                    return 5;
                }
            }

        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch (result) {
                case 0:
                    likeImg.setImageResource(R.drawable.ic_thumbs_up_empty);
                    break;
                case 1:
                    likeImg.setImageResource(R.drawable.ic_thumbs_up_green);
                    break;
                case 2:
                    likeImg.setImageResource(R.drawable.ic_thumbs_up_green);
                    notLikeImg.setImageResource(R.drawable.ic_thumbs_down_empty);
                    break;
                case 3:
                    notLikeImg.setImageResource(R.drawable.ic_thumbs_down_empty);
                    break;
                case 4:
                    notLikeImg.setImageResource(R.drawable.ic_thumbs_down_green);
                    break;
                case 5:
                    likeImg.setImageResource(R.drawable.ic_thumbs_up_empty);
                    notLikeImg.setImageResource(R.drawable.ic_thumbs_down_green);
                    break;
                default:
                    break;
            }
            likesNumber.setText("" + like);
            notLikesNumber.setText("" + notLike);
        }
    }

    private class InsertValutation extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            new Valutazione(getApplicationContext()).insert(params[0], currUser, segnalationObject);
            like = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, 1);
            notLike = new Valutazione(getApplicationContext()).getNumberOfLikesOrDislikes(segnalationObject, -1);
            return params[0];
        }

        @Override
        protected void onPostExecute(Integer value) {
            super.onPostExecute(value);
            if (value == 1) {
                likeImg.setImageResource(R.drawable.ic_thumbs_up_green);
                likesNumber.setText("" + like);
                notLikesNumber.setText("" + notLike);
            } else {
                notLikeImg.setImageResource(R.drawable.ic_thumbs_down_green);
                likesNumber.setText("" + like);
                notLikesNumber.setText("" + notLike);
            }
        }
    }

}