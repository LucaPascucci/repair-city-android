package com.infinitycode.repaircity.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.NavigationDrawerFragment;
import com.infinitycode.repaircity.ProfileViewPager;
import com.infinitycode.repaircity.R;
import com.infinitycode.repaircity.adapters.ProfileAdapter;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.parse.Utente;
import com.infinitycode.repaircity.parse.Valutazione;
import com.infinitycode.repaircity.tabs.SlidingTabLayout;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.malinskiy.superrecyclerview.swipe.SwipeItemManagerInterface;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.pkmmte.view.CircularImageView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Infinity Code on 14/08/15.
 */

public class ProfileActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int ACTIVITY_NUMBER = 3;
    private static final int SYNC_SEGNALATIONS = 1;
    private static final int SYNC_USERS = 2;

    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private static Location currentLocation;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private NavigationDrawerFragment drawerFragment;
    private static ProfileViewPager pager;

    private CircularImageView profileImageView;
    private TextView userNameTxt;
    private TextView likesNumberTxt;
    private TextView notLikesNumberTxt;

    private ProgressDialog loading;
    private AlertDialog GPSdialog;
    private AlertDialog networkDialog;

    private static ParseObject user;

    private boolean userForcedSync = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        this.profileImageView = (CircularImageView) findViewById(R.id.profile_image_view);
        this.userNameTxt = (TextView) findViewById(R.id.profile_username);
        this.likesNumberTxt = (TextView) findViewById(R.id.profile_likes_number);
        this.notLikesNumberTxt = (TextView) findViewById(R.id.profile_notlikes_number);

        new PrepareUI().execute();

        new Global(ProfileActivity.this).writeIntInPreferences(Global.keyPreferencesCurrentActivity, ACTIVITY_NUMBER);

        this.drawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_profile_drawer);
        this.drawerFragment.setUp(R.id.fragment_profile_drawer, (DrawerLayout) findViewById(R.id.drawer_layout_profile), toolbar);

        pager = (ProfileViewPager) findViewById(R.id.pager_profile);
        SlidingTabLayout tabs = (SlidingTabLayout) findViewById(R.id.tabs_profile);
        tabs.setDistributeEvenly(true);
        tabs.setCustomTabView(R.layout.custom_tab_view_profile, R.id.tab_text_profile);

        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {

            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.accent);
            }
        });
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
        tabs.setViewPager(pager);
        pager.setPagingEnabled(false);

        if (new Global(ProfileActivity.this).checkGeolocalization()) {
            if (new Global(ProfileActivity.this).getBooleanFromPreferences(Global.keyPreferencesForcedSync) && currentLocation != null) {
                this.loading = ProgressDialog.show(ProfileActivity.this, null, getString(R.string.wait_forced_sync), true, false);
                new SyncAllWithOnline().execute();
            }
        } else {
            showErrorDialogGPS();
        }

        if (new Global(ProfileActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncSegnalation) && !new Global(ProfileActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncSegnalation)) {
            Log.d("MapActivity", "Ci sono segnalazioni locali");
            new SyncLocalData().execute(SYNC_SEGNALATIONS);
        }
        if (new Global(ProfileActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncUser) && !new Global(ProfileActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncUser)) {
            Log.d("MapActivity", "Ci sono utenti locali");
            new SyncLocalData().execute(SYNC_USERS);
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
            if (new Global(ProfileActivity.this).checkGeolocalization() && currentLocation != null) {
                if (!new Global(ProfileActivity.this).getBooleanFromPreferences(Global.keyPreferencesForcedSync)) {
                    if (!this.userForcedSync) {
                        this.userForcedSync = true;
                        new SyncAllWithOnline().execute();

                    } else {
                        Toast.makeText(ProfileActivity.this, R.string.wait_sync, Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                showErrorDialogGPS();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mGoogleApiClient.connect();
        MyProfileFragment fragment = MyProfileFragment.getInstance(pager.getCurrentItem());
        if (fragment != null) {
            fragment.refreshLocalList(pager.getCurrentItem());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(this.mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
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

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onConnected(Bundle bundle) {
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (currentLocation == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
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
            Log.i("LOCATION", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    private void showErrorDialogGPS() {
        if (this.GPSdialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
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

    private class MyPagerAdapter extends FragmentPagerAdapter {

        private final String[] tabs;
        final SparseArray<Fragment> registeredFragments = new SparseArray<>();

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            this.tabs = getResources().getStringArray(R.array.profile_tabs);
        }

        @Override
        public Fragment getItem(int position) {
            return MyProfileFragment.getInstance(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return this.tabs[position];
        }

        @Override
        public int getCount() {
            return tabs.length;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }

    public static class MyProfileFragment extends Fragment {

        private SuperRecyclerView recyclerView;
        private ProfileAdapter adapter;

        private AsyncLocalUpdate allSegnalationTab = new AsyncLocalUpdate();
        private AsyncLocalUpdate solvedSegnalationTab = new AsyncLocalUpdate();
        private AsyncLocalUpdate notSolvedSegnalationTab = new AsyncLocalUpdate();
        private List<ParseObject> allSegnalationList;
        private List<ParseObject> solvedSegnalationList;
        private List<ParseObject> notSolvedSegnalationList;

        public static MyProfileFragment getInstance(final int position) {
            MyProfileFragment fragment = new MyProfileFragment();
            Bundle args = new Bundle();
            args.putInt("position", position);
            fragment.setArguments(args);
            return fragment;
        }

        private ProfileAdapter getAdapter() {
            return this.adapter;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.fragment_profile, container, false);

            this.recyclerView = (SuperRecyclerView) layout.findViewById(R.id.segnalation_profile_list);
            ClickItemListener listener = new ClickItemListener();
            this.adapter = new ProfileAdapter(getActivity(), listener);
            this.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            this.recyclerView.setAdapter(this.adapter);
            this.adapter.setMode(SwipeItemManagerInterface.Mode.Single);

            Bundle bundle = getArguments();
            if (bundle != null) {
                refreshLocalList(bundle.getInt("position"));
            }
            return layout;
        }

        private void refreshLocalList(int tab) {
            switch (tab) {
                case 0:
                    if (this.allSegnalationTab.getStatus() == AsyncTask.Status.PENDING) {
                        this.allSegnalationTab.execute(tab);
                    }
                    if (this.allSegnalationTab.getStatus() == AsyncTask.Status.FINISHED) {
                        this.adapter.swap(allSegnalationList, 0);
                        this.allSegnalationTab = new AsyncLocalUpdate();
                        this.allSegnalationTab.execute(tab);
                    }
                    break;
                case 1:
                    if (this.solvedSegnalationTab.getStatus() == AsyncTask.Status.PENDING) {
                        this.solvedSegnalationTab.execute(tab);
                    }
                    if (this.solvedSegnalationTab.getStatus() == AsyncTask.Status.FINISHED) {
                        this.adapter.swap(solvedSegnalationList, 1);
                        this.solvedSegnalationTab = new AsyncLocalUpdate();
                        this.solvedSegnalationTab.execute(tab);
                    }
                    break;
                case 2:
                    if (this.notSolvedSegnalationTab.getStatus() == AsyncTask.Status.PENDING) {
                        this.notSolvedSegnalationTab.execute(tab);
                    }
                    if (this.notSolvedSegnalationTab.getStatus() == AsyncTask.Status.FINISHED) {
                        this.adapter.swap(notSolvedSegnalationList, 2);
                        this.notSolvedSegnalationTab = new AsyncLocalUpdate();
                        this.notSolvedSegnalationTab.execute(tab);
                    }
                    break;
            }
        }

        public class ClickItemListener implements View.OnClickListener {

            @Override
            public void onClick(View view) {
                if (!((ProfileActivity) getActivity()).userForcedSync) {
                    if (view instanceof Button) {
                        View v = (View) view.getParent().getParent().getParent();
                        int itemPosition = recyclerView.getRecyclerView().getChildAdapterPosition(v);
                        new SegnalationUpdater().execute(itemPosition);
                    } else {
                        int itemPosition = recyclerView.getRecyclerView().getChildAdapterPosition(view);
                        Intent intent = new Intent(recyclerView.getContext(), DetailActivity.class);

                        MyPagerAdapter pagerAdapter = (MyPagerAdapter) pager.getAdapter();
                        MyProfileFragment fragment = (MyProfileFragment) pagerAdapter.getRegisteredFragment(pager.getCurrentItem());

                        ParseObject obj = fragment.getAdapter().getListItem(itemPosition);

                        Log.d("STAMPE", "position: " + itemPosition + " -> title: " + obj.get(Segnalazione.title));

                        intent.putExtra("segnalationId", obj.getObjectId());
                        recyclerView.getContext().startActivity(intent);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.wait_sync, Toast.LENGTH_SHORT).show();
                }
            }
        }

        public class AsyncLocalUpdate extends AsyncTask<Integer, Void, Integer> {

            @Override
            protected Integer doInBackground(Integer... params) {
                switch (params[0]) {
                    case 0:
                        allSegnalationList = new Segnalazione(getActivity()).getSegnalationsOfAnUser(user, null);
                    case 1:
                        solvedSegnalationList = new Segnalazione(getActivity()).getSegnalationsOfAnUser(user, true);
                    case 2:
                        notSolvedSegnalationList = new Segnalazione(getActivity()).getSegnalationsOfAnUser(user, false);
                }
                return params[0];
            }

            @Override
            protected void onPostExecute(Integer tab) {
                super.onPostExecute(tab);
                MyPagerAdapter pagerAdapter = (MyPagerAdapter) pager.getAdapter();
                MyProfileFragment fragment = (MyProfileFragment) pagerAdapter.getRegisteredFragment(tab);
                if (fragment != null) {
                    switch (tab) {
                        case 0:
                            fragment.getAdapter().swap(allSegnalationList, 0);
                            break;
                        case 1:
                            fragment.getAdapter().swap(solvedSegnalationList, 1);
                            break;
                        case 2:
                            fragment.getAdapter().swap(notSolvedSegnalationList, 2);
                            break;
                    }
                }
            }
        }

        public MyPagerAdapter getMyPagerAdapter() {
            return (MyPagerAdapter) pager.getAdapter();
        }

        public int getCurrentItemFromPager() {
            return pager.getCurrentItem();
        }

        public class SegnalationUpdater extends AsyncTask<Integer, Void, Boolean> {

            @Override
            protected Boolean doInBackground(final Integer... params) {
                if (Global.isNetworkAvailable()) {
                    MyPagerAdapter pagerAdapter = getMyPagerAdapter();
                    MyProfileFragment fragment = (MyProfileFragment) pagerAdapter.getRegisteredFragment(getCurrentItemFromPager());
                    final ParseObject segnalation = fragment.getAdapter().getListItem(params[0]);
                    new Segnalazione(getActivity()).updateSolved(segnalation.getObjectId(), !segnalation.getBoolean(Segnalazione.solved));
                    return true;
                }
                return false ;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                if (result) {
                    refreshLocalList(0);
                    refreshLocalList(1);
                    refreshLocalList(2);
                } else {
                    ((ProfileActivity)getActivity()).showErrorDialogNetwork();
                }
            }
        }
    }

    private class SyncAllWithOnline extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            if (Global.isNetworkAvailable()) {
                try {
                    ProfileActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ProfileActivity.this, R.string.forced_sync_message, Toast.LENGTH_LONG).show();
                        }
                    });
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
                ProfileActivity.this.runOnUiThread(new Runnable() {
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
                MyPagerAdapter pagerAdapter = (MyPagerAdapter) pager.getAdapter();
                MyProfileFragment fragment0 = (MyProfileFragment) pagerAdapter.getRegisteredFragment(0);
                MyProfileFragment fragment1 = (MyProfileFragment) pagerAdapter.getRegisteredFragment(1);
                MyProfileFragment fragment2 = (MyProfileFragment) pagerAdapter.getRegisteredFragment(2);

                if (fragment0 != null) {
                    fragment0.refreshLocalList(0);
                }

                if (fragment1 != null) {
                    fragment1.refreshLocalList(1);
                }

                if (fragment2 != null) {
                    fragment2.refreshLocalList(2);
                }

                new Global(ProfileActivity.this).writeBooleanInPreferences(Global.keyPreferencesForcedSync, false);
                Toast.makeText(ProfileActivity.this, R.string.finished_sync, Toast.LENGTH_LONG).show();
            } else {
                showErrorDialogNetwork();
            }

        }
    }

    private class PrepareUI extends AsyncTask<Void, Void, Void> {

        Bitmap profileImage;
        int numberOfLikes;
        int numberOfDislikes;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                user = new Utente(getApplicationContext()).getLocalUserObject((new Global(getApplicationContext())).getStringFromPreferences(Global.keyPreferencesFB));
                profileImage = new Utente(getApplicationContext()).getUserProfilePhoto(user);
                numberOfLikes = new Valutazione(getApplicationContext()).getNumberOfLikeOrDislikesOfAnUser(user, 1);
                numberOfDislikes = new Valutazione(getApplicationContext()).getNumberOfLikeOrDislikesOfAnUser(user, -1);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            userNameTxt.setText(user.getString(Utente.name));
            profileImageView.setImageBitmap(profileImage);
            likesNumberTxt.setText("" + numberOfLikes);
            notLikesNumberTxt.setText("" + numberOfDislikes);
        }
    }

    private class SyncLocalData extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {

            if (Global.isNetworkAvailable()) {
                switch (params[0]) {
                    case SYNC_SEGNALATIONS:
                        Log.d("MapActivity", "Avviata sync segnalazioni locali");
                        new Global(ProfileActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, true);
                        new Segnalazione(ProfileActivity.this).syncingLocalChanges();
                        break;
                    case SYNC_USERS:
                        Log.d("MapActivity", "Avviata sync utenti locali");
                        new Global(ProfileActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, true);
                        new Utente(ProfileActivity.this).syncingLocalChanges();
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
                    new Global(ProfileActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, false);
                    break;
                case SYNC_USERS:
                    Log.d("MapActivity", "Conclusa sync utenti locali");
                    new Global(ProfileActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, false);
                    break;
            }
        }
    }

}