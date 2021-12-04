package com.infinitycode.repaircity.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.parse.Utente;
import com.infinitycode.repaircity.R;
import com.infinitycode.repaircity.walkthrough.WalkthroughActivity;
import com.parse.ParseException;
import com.parse.ParseObject;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Infinity Code on 14/08/15.
 */

public class LoginActivity extends AppCompatActivity {

    private CallbackManager callbackManager;
    private ParseObject user;
    private ProfileTracker profileTracker;
    private ProgressDialog loading;
    private AlertDialog GPSdialog;
    private AlertDialog networkDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.hideStatusBar();
        setContentView(R.layout.activity_login);

        if (new Global(LoginActivity.this).getStringFromPreferences(Global.keyPreferencesFB) != null) {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
            finish();
        } else {
            if (!new Global(LoginActivity.this).checkGeolocalization()){
               this.showErrorDialogGPS();
            }
        }

        this.loading = new ProgressDialog(LoginActivity.this);

        this.callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton) this.findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");

        this.profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile newProfile) {
                Log.d("LoginActivity","onCurrentProfileChanged");
                if (newProfile != null) {
                    try {
                        new Utente(LoginActivity.this).getLocalUserObject(newProfile.getId());
                        new Global(LoginActivity.this).writeBooleanInPreferences(Global.keyPreferencesFirstLaunch, false);
                    } catch (ParseException e) {
                        new Global(LoginActivity.this).writeBooleanInPreferences(Global.keyPreferencesFirstLaunch, true);
                    }

                    user = new ParseObject(Utente.className);
                    user.put(Utente.facebookID, newProfile.getId());
                    user.put(Utente.name, newProfile.getName());

                    GraphRequest request = GraphRequest.newMeRequest(
                            AccessToken.getCurrentAccessToken(),
                            new GraphRequest.GraphJSONObjectCallback() {
                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {
                                    Log.d("LoginActivity","onCompleted");
                                    try {
                                        user.put(Utente.mail, object.get("email"));

                                    } catch (JSONException e) {
                                        Log.e("JSONException", e.getMessage());
                                    }
                                    new AsyncLogin().execute(user);
                                    profileTracker.stopTracking();
                                }
                            });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "email");
                    request.setParameters(parameters);
                    request.executeAsync();
                }
            }
        };

        loginButton.registerCallback(this.callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("LoginActivity", "onSuccess");
                profileTracker.startTracking();
                AccessToken.setCurrentAccessToken(loginResult.getAccessToken());
                loading = ProgressDialog.show(LoginActivity.this, null, getString(R.string.waiting_message), true, false);
            }

            @Override
            public void onCancel() {
                Log.d("LoginActivity", "onCancel");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d("LoginActivity", "OnError");
                showErrorDialogNetwork();
                profileTracker.stopTracking();
            }
        });
        this.profileTracker.startTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.hideStatusBar();
        this.profileTracker.startTracking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.profileTracker.stopTracking();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }


    private void hideStatusBar() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void showErrorDialogGPS(){
        if (this.GPSdialog == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
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
        if (!this.GPSdialog.isShowing()){
            this.GPSdialog.show();
        }
    }

    private void showErrorDialogNetwork(){
        if (this.networkDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
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
        if (!this.networkDialog.isShowing()){
            this.networkDialog.show();
        }
    }

    private class AsyncLogin extends AsyncTask<ParseObject, Void, Boolean> {

        @Override
        protected Boolean doInBackground(ParseObject... params) {
            if (Global.isNetworkAvailable()){
                Log.d("LoginActivity", "Avvio il login tramite parse");
                return new Utente(LoginActivity.this).login(params[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (loading != null && loading.isShowing()) {
                loading.dismiss();
            }
            if (result == null){
                showErrorDialogNetwork();
            }else {
                if (result) {
                    if (new Global(LoginActivity.this).getBooleanFromPreferences(Global.keyPreferencesFirstLaunch)) {
                        Intent intent = new Intent(LoginActivity.this, WalkthroughActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }else{
                    LoginManager.getInstance().logOut();
                    profileTracker.stopTracking();
                    showErrorDialogNetwork();
                    Log.d("LoginActivity", "profileTracker: " + profileTracker.isTracking());
                }
            }

        }
    }
}

