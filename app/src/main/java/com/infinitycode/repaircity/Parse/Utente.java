package com.infinitycode.repaircity.parse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.infinitycode.repaircity.Global;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Created by Infinity Code on 12/08/2015.
 */
public class Utente {

    public static final String className = "Utente";
    public static final String facebookID = "facebookID";
    public static final String name = "nome";
    public static final String mail = "mail";
    private static final String profilePhoto = "fotoProfilo";
    private static final String objectID = "objectId";

    /**
     * Variabile per il pin e unpin degli oggetti non salvati online per mancanza di connessione.
     */
    private static final String userNotSaved = "newUser";

    private final Context context;

    public Utente(Context context) {
        this.context = context;
    }

    public boolean login(final ParseObject user) {
        boolean checkSavingLocal = false;
        boolean checkSavingOnline = false;
        boolean checkExistOnline = false;
        try {
            final ParseObject onlineUser = this.getUserOnline(user.getString(Utente.facebookID));
            onlineUser.pin();
            checkExistOnline = true;
        } catch (final ParseException e) {
            final byte[] data = this.getProfilePhotoOnline(user.getString(Utente.facebookID));
            if (data != null) {
                final ParseFile imageFile = new ParseFile(user.getString(Utente.facebookID) + ".png", data);
                try {
                    imageFile.save();
                    user.put(Utente.profilePhoto, imageFile);
                    try {
                        user.save();
                        checkSavingOnline = true;
                        user.pin();
                        checkSavingLocal = true;
                    } catch (final ParseException e1) {
                        e1.printStackTrace();
                        try {
                            user.pin(Utente.userNotSaved);
                            new Global(context).writeStringInPreferences(user.getString(Utente.facebookID), Base64.encodeToString(data, Base64.DEFAULT));
                            checkSavingLocal = true;
                            new Global(context).writeBooleanInPreferences(Global.keyPreferencesUsyncUser,true);
                        } catch (final ParseException e2) {
                            e2.printStackTrace();
                        }
                    }
                } catch (ParseException e1) {
                    Log.d("LoginActivity", "foto non salvata");
                    return false;
                }
            } else {
                Log.d("LoginActivity", "data == null");
                return false;
            }
        }

        if (checkExistOnline || checkSavingLocal || checkSavingOnline) {
            new Global(this.context).writeStringInPreferences(Global.keyPreferencesFB, user.getString(Utente.facebookID));
        }
        return checkExistOnline || checkSavingLocal || checkSavingOnline;
    }

    private ParseObject getUserOnline(final String facebookID) throws ParseException {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Utente.className);
        query.whereEqualTo(Utente.facebookID, facebookID);
        return query.getFirst();
    }

    public ParseObject getLocalUserObject(final String facebookID) throws ParseException {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Utente.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Utente.facebookID, facebookID);
        return query.getFirst();
    }

    public Bitmap getUserProfilePhoto(final ParseObject user) {
        final ParseFile userImageFile = user.getParseFile(Utente.profilePhoto);
        byte[] imageData;
        try {
            imageData = userImageFile.getData();
        } catch (ParseException e) {
            imageData = this.getProfilePhotoOnline(user.getString(Utente.facebookID));
            if (imageData == null) {
                return null;
            }
        }
        return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
    }

    public ParseObject getUserFromObjectID(final String userObjectId) throws ParseException {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Utente.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Utente.objectID, userObjectId);
        return query.getFirst();
    }

    @SuppressWarnings("UnusedReturnValue")
    public List<ParseObject> updateWithOnline(final Set<String> objectIDSet) {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Utente.className);
        query.whereContainedIn(Utente.objectID, objectIDSet);
        try {
            final List<ParseObject> results = query.find();
            for (final ParseObject result : results) {
                result.pin();
            }
            return results;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] getProfilePhotoOnline(String facebookID) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            final URL imageURL = new URL("https://graph.facebook.com/" + facebookID + "/picture?width=200&height=200");
            final Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void syncingLocalChanges() {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Utente.className);
        query.fromLocalDatastore();
        query.fromPin(Utente.userNotSaved);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                boolean uploadIssues = false;
                for (final ParseObject user : list) {
                    //noinspection UnnecessaryLocalVariable
                    final ParseObject objectUser = user;
                    String stringData = new Global(context).getStringFromPreferences(user.getString(Utente.facebookID));
                    byte[] data = Base64.decode(stringData, Base64.DEFAULT);
                    final ParseFile imageFile = new ParseFile(user.getString(Utente.facebookID) + ".png", data);

                    try {
                        imageFile.save();
                        user.unpin(Utente.userNotSaved);
                        objectUser.put(Utente.profilePhoto, imageFile);
                        objectUser.pin();
                        objectUser.saveInBackground(new SaveCallback() {
                            //TODO NON SO SE ANDRA BENE
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    try {
                                        objectUser.unpin();
                                        objectUser.pin(Utente.userNotSaved);
                                    } catch (ParseException e1) {
                                        e1.printStackTrace();
                                    }
                                } else {
                                    new Global(context).removeFromSharedPreferences(user.getString(Utente.facebookID));
                                }
                            }
                        });
                    } catch (ParseException e1) {
                        Log.d("LoginActivity", "foto non salvata");
                        uploadIssues = true;
                    }
                }
                Log.d("MapActivity", "Utenti uploadIssues: " + uploadIssues);
                new Global(context).writeBooleanInPreferences(Global.keyPreferencesUsyncUser, uploadIssues);
            }
        });
    }

}
