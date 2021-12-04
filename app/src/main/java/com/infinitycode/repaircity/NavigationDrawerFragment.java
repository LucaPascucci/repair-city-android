package com.infinitycode.repaircity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.infinitycode.repaircity.activity.AddActivity;
import com.infinitycode.repaircity.activity.ListActivity;
import com.infinitycode.repaircity.activity.LoginActivity;
import com.infinitycode.repaircity.activity.ProfileActivity;
import com.infinitycode.repaircity.activity.SettingsActivity;
import com.infinitycode.repaircity.adapters.NavigationAdapter;
import com.infinitycode.repaircity.parse.Utente;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.pkmmte.view.CircularImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Infinity Code on 24/08/2015.
 */
public class NavigationDrawerFragment extends Fragment implements NavigationAdapter.ClickListener {

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private View containerView;
    private CircularImageView profilePhoto;
    private TextView userName;

    private boolean viewChanging = false;
    private boolean mDrawerOpened = false;

    public NavigationDrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //RecyclerView recyclerView;
        //NavigationAdapter adapter;
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        RecyclerView recyclerView = (RecyclerView) layout.findViewById(R.id.drawerList);
        NavigationAdapter adapter = new NavigationAdapter(getActivity(), getData());
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.profilePhoto = (CircularImageView) layout.findViewById(R.id.profilePhotoNavigation);
        this.userName = (TextView) layout.findViewById(R.id.userNameNavigation);

        new PrepareNavigatioDrawer().execute();

        return layout;
    }

    private static List<NavigationCell> getData() {
        List<NavigationCell> data = new ArrayList<>();
        int[] selected_icons = {R.drawable.ic_map, R.drawable.ic_list, R.drawable.ic_plus, R.drawable.ic_user, R.drawable.ic_settings, R.drawable.ic_exit};
        int[] unselected_icons = {R.drawable.ic_map_gray, R.drawable.ic_list_gray, R.drawable.ic_plus_gray, R.drawable.ic_user_gray, R.drawable.ic_settings_gray, R.drawable.ic_exit_gray};
        String[] titles = {"Mappa", "Lista segnalazioni", "Nuova segnalazione", "Profilo", "Impostazioni", "Esci"};

        for (int i = 0; i < titles.length && i < selected_icons.length; i++) {
            NavigationCell current = new NavigationCell();
            current.iconIdSelected = selected_icons[i];
            current.iconIdUnselected = unselected_icons[i];
            current.title = titles[i];
            data.add(current);
        }

        return data;
    }


    public void setUp(int fragmentId, DrawerLayout drawerLayout, final Toolbar toolbar) {
        containerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                mDrawerOpened = true;
                super.onDrawerOpened(drawerView);
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                mDrawerOpened = false;
                super.onDrawerClosed(drawerView);
                getActivity().invalidateOptionsMenu();
                if (viewChanging) {
                    viewChanging = false;
                    int currentActivity = new Global(getActivity()).getIntFromPreferences(Global.keyPreferencesCurrentActivity);
                    int selectedActivity = new Global(getActivity()).getIntFromPreferences(Global.keyPreferencesSelectedNavigation);
                    switch (selectedActivity) {
                        case 0:
                            getActivity().onBackPressed();
                            break;
                        case 1:
                            startActivity(new Intent(getActivity(), ListActivity.class));
                            if (currentActivity != 0) {
                                getActivity().finish();
                            }
                            break;
                        case 2:
                            startActivity(new Intent(getActivity(), AddActivity.class));
                            if (currentActivity != 0) {
                                getActivity().finish();
                            }
                            break;
                        case 3:
                            startActivity(new Intent(getActivity(), ProfileActivity.class));
                            if (currentActivity != 0) {
                                getActivity().finish();
                            }
                            break;
                        case 4:
                            startActivity(new Intent(getActivity(), SettingsActivity.class));
                            if (currentActivity != 0) {
                                getActivity().finish();
                            }
                            break;
                        case 5:
                            new Global(getActivity()).removeFromSharedPreferences(Global.keyPreferencesFB);
                            LoginManager.getInstance().logOut();
                            startActivity(new Intent(getActivity(), LoginActivity.class));
                            getActivity().finish();
                            break;
                    }
                }
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (imm.isAcceptingText() && getActivity().getCurrentFocus() != null) { // verify if the soft keyboard is open
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }

                if (slideOffset < 0.6) {
                    toolbar.setAlpha(1 - slideOffset);
                }
            }
        };

        if (!mDrawerOpened) {
            mDrawerLayout.closeDrawer(containerView);
        }

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
    }

    @Override
    public void itemClicked(View view, int position) {
        int currentActivity = new Global(getActivity()).getIntFromPreferences(Global.keyPreferencesCurrentActivity);

        viewChanging = currentActivity != position;
        new Global(getActivity()).writeIntInPreferences(Global.keyPreferencesSelectedNavigation, position);
        mDrawerLayout.closeDrawer(containerView);
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(containerView);
    }

    private class PrepareNavigatioDrawer extends AsyncTask<Void, Void, Boolean>{

        private ParseObject user;
        private Bitmap userPhoto;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                user = new Utente(getActivity()).getLocalUserObject((new Global(getActivity())).getStringFromPreferences(Global.keyPreferencesFB));
                userPhoto = new Utente(getActivity()).getUserProfilePhoto(user);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                profilePhoto.setImageBitmap(userPhoto);
                userName.setText(user.getString(Utente.name));
            }

        }
    }
}