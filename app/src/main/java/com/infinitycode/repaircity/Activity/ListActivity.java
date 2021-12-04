package com.infinitycode.repaircity.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.NavigationDrawerFragment;
import com.infinitycode.repaircity.R;
import com.infinitycode.repaircity.adapters.SegnalationAdapter;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.parse.Utente;
import com.infinitycode.repaircity.parse.Valutazione;
import com.infinitycode.repaircity.tabs.SlidingTabLayout;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Infinity Code on 11/08/2015.
 */

public class ListActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static ViewPager pager;
    private SlidingTabLayout tabs;
    private NavigationDrawerFragment drawerFragment;
    private ProgressDialog loading;

    private static Location location;

    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int ACTIVITY_NUMBER = 1;
    private static final int SYNC_SEGNALATIONS = 1;
    private static final int SYNC_USERS = 2;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static FloatingActionButton fab;
    private static SwipeRefreshLayout refreshLayout;

    private AlertDialog GPSdialog;
    private AlertDialog networkDialog;

    private Boolean userForceSync = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

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

        new Global(ListActivity.this).writeIntInPreferences(Global.keyPreferencesCurrentActivity, ACTIVITY_NUMBER);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        this.drawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_list_drawer);
        this.drawerFragment.setUp(R.id.fragment_list_drawer, (DrawerLayout) findViewById(R.id.drawer_layout_list), toolbar);

        pager = (ViewPager) findViewById(R.id.pager);

        tabs = (SlidingTabLayout) findViewById(R.id.tabs_list);
        tabs.setCustomTabView(R.layout.custom_tab_view_list, R.id.tab_text_list);
        tabs.setDistributeEvenly(true);
        tabs.setBackgroundColor(getResources().getColor(R.color.primary));

        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {

            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.accent);
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.action_button_list);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(ListActivity.this, AddActivity.class);
                startActivity(intent);
                finish();
            }
        });

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        refreshLayout.setColorSchemeResources(R.color.accent_material_dark, R.color.accent_material_light, R.color.primary_dark);

        new Global(ListActivity.this).writeIntInPreferences(Global.keyPreferencesCurrentActivity, ACTIVITY_NUMBER);
        if (new Global(ListActivity.this).getBooleanFromPreferences(Global.keyPreferencesForcedSync)) {
            loading = ProgressDialog.show(ListActivity.this, null, getString(R.string.wait_forced_sync), true, false);
            new SyncAllWithOnline().execute();
        }

        if (new Global(ListActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncSegnalation) && !new Global(ListActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncSegnalation)) {
            Log.d("MapActivity", "Ci sono segnalazioni locali");
            new SyncLocalData().execute(SYNC_SEGNALATIONS);
        }
        if (new Global(ListActivity.this).getBooleanFromPreferences(Global.keyPreferencesUsyncUser) && !new Global(ListActivity.this).getBooleanFromPreferences(Global.getKeyPreferencesLocalSyncUser)) {
            Log.d("MapActivity", "Ci sono utenti locali");
            new SyncLocalData().execute(SYNC_USERS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
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
        ListActivity.location = location;
    }

    @Override
    public void onConnected(Bundle bundle) {
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
        tabs.setViewPager(pager);

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

    private void callSyncAllWithOnline() {
        new SyncAllWithOnline().execute();
    }

    private void showErrorDialogGPS() {
        if (this.GPSdialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ListActivity.this);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(ListActivity.this);
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

    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        private final String[] tabs;
        final SparseArray<Fragment> registeredFragments = new SparseArray<>();

        public MyPagerAdapter(final FragmentManager fm) {
            super(fm);
            this.tabs = getResources().getStringArray(R.array.list_tabs);
        }

        @Override
        public Fragment getItem(final int position) {
            return MyFragment.getInstance(position);
        }

        @Override
        public CharSequence getPageTitle(final int position) {
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

    public static class MyFragment extends Fragment {

        private Context context;

        private RecyclerView recyclerView;
        private SegnalationAdapter adapter;
        private LinearLayoutManager linearLayoutManager;

        private AsyncLocalDownload firstTabDownload = new AsyncLocalDownload();
        private AsyncLocalDownload secondTabDownload = new AsyncLocalDownload();
        private AsyncLocalDownload thirdTabDownload = new AsyncLocalDownload();
        private AsyncLocalDownload fourthTabDownload = new AsyncLocalDownload();
        private List<ParseObject> firstTabSegnalation;
        private List<ParseObject> secondTabSegnalation;
        private List<ParseObject> thirdTabSegnalation;
        private List<ParseObject> fourthTabSegnalation;

        public static MyFragment getInstance(final int position) {
            MyFragment fragment = new MyFragment();
            Bundle args = new Bundle();
            args.putInt("position", position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            this.context = container.getContext();

            View layout = inflater.inflate(R.layout.fragment_list, container, false);
            this.recyclerView = (RecyclerView) layout.findViewById(R.id.segnalationList);

            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    ((ListActivity) getActivity()).callSyncAllWithOnline();
                    ((ListActivity) getActivity()).userForceSync = true;
                }
            });
            ClickItemListener listener = new ClickItemListener();
            this.adapter = new SegnalationAdapter(getActivity(), location, listener);
            this.recyclerView.setAdapter(this.adapter);
            this.linearLayoutManager = new LinearLayoutManager(getActivity());
            this.recyclerView.setLayoutManager(this.linearLayoutManager);

            this.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (Math.abs(dy) > 4) {
                        if (dy > 0) {
                            fab.hide(true);
                        } else {
                            fab.show(true);
                        }
                    }
                    refreshLayout.setEnabled(linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0);
                }
            });


            Bundle bundle = getArguments();
            if (bundle != null) {
                this.refreshLocalList(bundle.getInt("position"));
            }
            return layout;
        }

        private SegnalationAdapter getAdapter() {
            return this.adapter;
        }

        private void refreshLocalList(int value) {
            switch (value) {
                case 0:
                    if (this.firstTabDownload.getStatus() == AsyncTask.Status.PENDING) {
                        this.firstTabDownload.execute(value);
                    }
                    if (this.firstTabDownload.getStatus() == AsyncTask.Status.FINISHED) {
                        this.adapter.swap(firstTabSegnalation);
                        this.firstTabDownload = new AsyncLocalDownload();
                        this.firstTabDownload.execute(value);
                    }
                    break;
                case 1:
                    if (this.secondTabDownload.getStatus() == AsyncTask.Status.PENDING) {
                        this.secondTabDownload.execute(value);
                    }
                    if (this.secondTabDownload.getStatus() == AsyncTask.Status.FINISHED) {
                        this.adapter.swap(secondTabSegnalation);
                        this.secondTabDownload = new AsyncLocalDownload();
                        this.secondTabDownload.execute(value);
                    }
                    break;
                case 2:
                    if (this.thirdTabDownload.getStatus() == AsyncTask.Status.PENDING) {
                        this.thirdTabDownload.execute(value);
                    }
                    if (this.thirdTabDownload.getStatus() == AsyncTask.Status.FINISHED) {
                        this.adapter.swap(thirdTabSegnalation);
                        this.thirdTabDownload = new AsyncLocalDownload();
                        this.thirdTabDownload.execute(value);
                    }
                    break;
                case 3:
                    if (this.fourthTabDownload.getStatus() == AsyncTask.Status.PENDING) {
                        this.fourthTabDownload.execute(value);
                    }
                    if (this.fourthTabDownload.getStatus() == AsyncTask.Status.FINISHED) {
                        this.adapter.swap(fourthTabSegnalation);
                        this.fourthTabDownload = new AsyncLocalDownload();
                        this.fourthTabDownload.execute(value);
                    }
                    break;
            }
        }

        public class ClickItemListener implements View.OnClickListener {

            @Override
            public void onClick(View view) {

                if (!((ListActivity) getActivity()).userForceSync) {
                    int itemPosition = recyclerView.getChildAdapterPosition(view);
                    Intent intent = new Intent(recyclerView.getContext(), DetailActivity.class);

                    MyPagerAdapter pagerAdapter = (MyPagerAdapter) pager.getAdapter();
                    MyFragment fragment = (MyFragment) pagerAdapter.getRegisteredFragment(pager.getCurrentItem());

                    ParseObject obj = fragment.getAdapter().getListItem(itemPosition);

                    Log.d("STAMPE", "position: " + itemPosition + " -> title: " + obj.get(Segnalazione.title));

                    intent.putExtra("segnalationId", obj.getObjectId());
                    recyclerView.getContext().startActivity(intent);
                } else {
                    Toast.makeText(context, R.string.wait_sync, Toast.LENGTH_SHORT).show();
                }
            }
        }

        private class AsyncLocalDownload extends AsyncTask<Integer, Void, Integer> {

            @Override
            protected Integer doInBackground(Integer... params) {
                switch (params[0]) {
                    case 0:
                        if (new Global(context).checkGeolocalization() && location != null) {
                            firstTabSegnalation = new Segnalazione(context).getSegnalationsInRange(new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                        } else {
                            (getActivity()).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((ListActivity) getActivity()).showErrorDialogGPS();
                                }
                            });

                        }

                    case 1:
                        secondTabSegnalation = new Segnalazione(context).getSegnalationsOrderedByPriority();
                    case 2:
                        thirdTabSegnalation = new Segnalazione(context).getSegnalationsOrderedByDate();
                    case 3:
                        fourthTabSegnalation = new Segnalazione(context).getSegnalationOrderedByPopularity();
                }
                return params[0];
            }

            @Override
            protected void onPostExecute(Integer param) {
                super.onPostExecute(param);
                MyPagerAdapter pagerAdapter = (MyPagerAdapter) pager.getAdapter();
                MyFragment fragment = (MyFragment) pagerAdapter.getRegisteredFragment(param);
                if (fragment != null) {
                    switch (param) {
                        case 0:
                            if (firstTabSegnalation != null) {
                                fragment.getAdapter().swap(firstTabSegnalation);
                            }
                            break;
                        case 1:
                            fragment.getAdapter().swap(secondTabSegnalation);
                            break;
                        case 2:
                            fragment.getAdapter().swap(thirdTabSegnalation);
                            break;
                        case 3:
                            fragment.getAdapter().swap(fourthTabSegnalation);
                            break;
                    }
                }
            }
        }
    }

    private class SyncAllWithOnline extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            if (Global.isNetworkAvailable()) {
                if (new Global(ListActivity.this).checkGeolocalization() && location != null) {
                    try {
                        if (!new Global(ListActivity.this).getBooleanFromPreferences(Global.keyPreferencesForcedSync)) {
                            ListActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ListActivity.this, R.string.forced_sync_message, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        final List<ParseObject> segnalations = new Segnalazione(getApplicationContext()).updateWithOnline(new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                        final Set<String> setUsers = new HashSet<>();
                        for (final ParseObject segnalation : segnalations) {
                            final ParseObject user = segnalation.getParseObject(Segnalazione.user);
                            setUsers.add(user.getObjectId());
                        }
                        new Utente(getApplicationContext()).updateWithOnline(setUsers);
                        new Valutazione(getApplicationContext()).updateWithOnline(segnalations);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 2;
                    }
                    return 0;
                }
                return 1;
            }
            return 2;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (loading != null && loading.isShowing()) {
                ListActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.dismiss();
                    }
                });
            }

            if (userForceSync) {
                userForceSync = false;
                refreshLayout.setRefreshing(false);
            }

            switch (result) {
                case 0:
                    new Global(ListActivity.this).writeBooleanInPreferences(Global.keyPreferencesForcedSync, false);

                    MyPagerAdapter pagerAdapter = (MyPagerAdapter) pager.getAdapter();
                    MyFragment fragment0 = (MyFragment) pagerAdapter.getRegisteredFragment(0);
                    MyFragment fragment1 = (MyFragment) pagerAdapter.getRegisteredFragment(1);
                    MyFragment fragment2 = (MyFragment) pagerAdapter.getRegisteredFragment(2);
                    MyFragment fragment3 = (MyFragment) pagerAdapter.getRegisteredFragment(3);

                    if (fragment0 != null) {
                        fragment0.refreshLocalList(0);
                    }
                    if (fragment1 != null) {
                        fragment1.refreshLocalList(1);
                    }
                    if (fragment2 != null) {
                        fragment2.refreshLocalList(2);
                    }
                    if (fragment3 != null) {
                        fragment3.refreshLocalList(3);
                    }

                    Toast.makeText(getApplicationContext(), R.string.finished_sync, Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    showErrorDialogGPS();
                    break;
                case 2:
                    showErrorDialogNetwork();
                    break;
            }
        }
    }

    private class SyncLocalData extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {

            if (Global.isNetworkAvailable()) {
                switch (params[0]) {
                    case SYNC_SEGNALATIONS:
                        Log.d("MapActivity", "Avviata sync segnalazioni locali");
                        new Global(ListActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, true);
                        new Segnalazione(ListActivity.this).syncingLocalChanges();
                        break;
                    case SYNC_USERS:
                        Log.d("MapActivity", "Avviata sync utenti locali");
                        new Global(ListActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, true);
                        new Utente(ListActivity.this).syncingLocalChanges();
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
                    new Global(ListActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncSegnalation, false);
                    break;
                case SYNC_USERS:
                    Log.d("MapActivity", "Conclusa sync utenti locali");
                    new Global(ListActivity.this).writeBooleanInPreferences(Global.getKeyPreferencesLocalSyncUser, false);
                    break;
            }
        }
    }
}
