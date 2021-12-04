package com.infinitycode.repaircity.parse;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.infinitycode.repaircity.Global;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by Infinity Code on 12/08/15.
 */

public class Segnalazione {

    private static final String className = "Segnalazione";
    public static final String title = "titolo";
    public static final String description = "descrizione";
    public static final String priority = "gravita";
    public static final String position = "posizione";
    public static final String solved = "risolto";
    private static final String objectId = "objectId";
    private static final String updatedDate = "updatedAt";
    public static final String segnalationPhoto = "fotoSegnalazione";
    public static final String user = "utenteCollegato";
    private static final String localDataUUID = "codiceFotoLocale";
    private static final String segnalationNotSaved = "newSegnalation";

    private final Context context;

    public Segnalazione(Context context) {
        this.context = context;
    }

    public boolean insert(String title, String description, int priority, Double latitude, Double longitude, byte[] segnalationPhoto, ParseObject user) {
        String uuid = UUID.randomUUID().toString();

        ParseObject newSegnalation = new ParseObject(Segnalazione.className);
        newSegnalation.put(Segnalazione.title, title);
        newSegnalation.put(Segnalazione.description, description);
        newSegnalation.put(Segnalazione.priority, priority);
        newSegnalation.put(Segnalazione.position, new ParseGeoPoint(latitude, longitude));
        newSegnalation.put(Segnalazione.solved, false);
        newSegnalation.put(Segnalazione.user, user);

        ParseFile imageFile = new ParseFile(uuid + ".png", segnalationPhoto);

        try {
            imageFile.save();
            newSegnalation.put(Segnalazione.segnalationPhoto, imageFile);
            try {
                newSegnalation.save();
                newSegnalation.pin();
                return true;
            }catch (ParseException e){
                newSegnalation.put(Segnalazione.localDataUUID, uuid);
                try {
                    newSegnalation.pin(Segnalazione.segnalationNotSaved);
                    new Global(this.context).writeStringInPreferences(uuid, Base64.encodeToString(segnalationPhoto, Base64.DEFAULT));
                    new Global(this.context).writeBooleanInPreferences(Global.keyPreferencesUsyncSegnalation,true);
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (ParseException e) {
            newSegnalation.put(Segnalazione.localDataUUID, uuid);
            try {
                newSegnalation.pin(Segnalazione.segnalationNotSaved);
                new Global(this.context).writeStringInPreferences(uuid, Base64.encodeToString(segnalationPhoto, Base64.DEFAULT));
                new Global(this.context).writeBooleanInPreferences(Global.keyPreferencesUsyncSegnalation,true);
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    public List<ParseObject> getSegnalationsInRange(ParseGeoPoint userLocation) {
        int range = new Global(this.context).getIntFromPreferences(Global.keyPreferencesSegnalationRange);
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Segnalazione.className);
        query.fromLocalDatastore();
        query.whereWithinKilometers(Segnalazione.position, userLocation, range);
        query.whereEqualTo(Segnalazione.solved, false);

        try {
            return query.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ParseObject> getSegnalationsOrderedByPriority() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Segnalazione.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Segnalazione.solved, false);
        query.orderByDescending(Segnalazione.priority);

        try {
            return query.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ParseObject> getSegnalationsOrderedByDate() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Segnalazione.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Segnalazione.solved, false);
        query.orderByDescending(Segnalazione.updatedDate);

        try {
            return query.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public List<ParseObject> getSegnalationOrderedByPopularity() {
        ParseQuery<ParseObject> querySegnalation = ParseQuery.getQuery(Segnalazione.className);
        querySegnalation.fromLocalDatastore();
        querySegnalation.whereEqualTo(Segnalazione.solved, false);
        List<ParseObject> results = null;
        try {
            results = querySegnalation.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // creo le strutture dati per poter salvare e ordinare i record
        HashMap<String, Integer> map = new HashMap<>();
        ValueComparator bvc = new ValueComparator(map);
        TreeMap<String, Integer> sorted_map = new TreeMap<>(bvc);

        ParseQuery<ParseObject> queryPopulatiry = ParseQuery.getQuery(Valutazione.className);
        queryPopulatiry.fromLocalDatastore();
        if (results != null) { //aggiunto perchè lint aveva le turbe
            for (ParseObject record : results) {
                queryPopulatiry.whereEqualTo(Valutazione.segnalation, record);
                queryPopulatiry.whereNotEqualTo(Valutazione.likeNotLike, 0);
                try {
                    int value = queryPopulatiry.count();
                    map.put(record.getObjectId(), value);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            sorted_map.putAll(map);
            List<ParseObject> list = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sorted_map.entrySet()) {
                ParseObject obj = getSegnalationFromObjectID(entry.getKey());
                list.add(obj);
            }

            return list;
        }
        return null;
    }

    public ParseObject getSegnalationFromObjectID(String segnalationObjectId) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Segnalazione.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Segnalazione.objectId, segnalationObjectId);
        try {
            return query.getFirst();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ParseObject> getSegnalationsOfAnUser(ParseObject user, Boolean solved) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Segnalazione.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Segnalazione.user, user);
        if (solved != null) {
            query.whereEqualTo(Segnalazione.solved, solved);
        }
        try {
            return query.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateSolved(String segnalationId, Boolean solved) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Segnalazione.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Segnalazione.objectId, segnalationId);
        try {
            ParseObject result = query.getFirst();
            result.put(Segnalazione.solved, solved);
            result.save();
            result.pin();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public List<ParseObject> updateWithOnline(ParseGeoPoint userLocation) throws ParseException {
        int range = new Global(this.context).getIntFromPreferences(Global.keyPreferencesSegnalationRange);
        ParseQuery<ParseObject> firstQuery = ParseQuery.getQuery(Segnalazione.className);
        firstQuery.whereWithinKilometers(Segnalazione.position, userLocation, range);
        List<ParseObject> firstResult = firstQuery.find();

        ParseQuery<ParseObject> secondQuery = ParseQuery.getQuery(Segnalazione.className);
        secondQuery.fromLocalDatastore();
        secondQuery.whereDoesNotMatchQuery(Segnalazione.objectId, firstQuery);

        List<ParseObject> secondResult = secondQuery.find();

        for (ParseObject obj : secondResult) {
            obj.unpin();
        }

        for (ParseObject obj : firstResult) {
            obj.pin();
        }
        return firstResult;

    }

    public void syncingLocalChanges() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Segnalazione.className);
        query.fromLocalDatastore();
        query.fromPin(Segnalazione.segnalationNotSaved);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                boolean uploadIssues = false;
                for (final ParseObject segnalation : list) {
                    //noinspection UnnecessaryLocalVariable
                    final ParseObject objectSegnalation = segnalation;
                    String stringData = new Global(context).getStringFromPreferences(segnalation.getString(Segnalazione.localDataUUID));
                    byte[] data = Base64.decode(stringData, Base64.DEFAULT);
                    ParseFile imageFile = new ParseFile(segnalation.getString(Segnalazione.localDataUUID) + ".png", data);

                    try {
                        imageFile.save();
                        objectSegnalation.put(Segnalazione.segnalationPhoto, imageFile);
                        segnalation.unpin(Segnalazione.segnalationNotSaved);
                        objectSegnalation.pin();
                        objectSegnalation.saveInBackground(new SaveCallback() { // TODO: non so se andrà bene
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    try {
                                        objectSegnalation.unpin();
                                        objectSegnalation.pin(Segnalazione.segnalationNotSaved);
                                    } catch (ParseException e1) {
                                        e1.printStackTrace();
                                    }
                                } else {
                                    new Global(context).removeFromSharedPreferences(segnalation.getString(Segnalazione.localDataUUID));
                                }
                            }
                        });

                    } catch (ParseException e1) {
                        e1.printStackTrace();
                        uploadIssues = true;
                    }
                }
                Log.d("MapActivity", "Segnalazioni uploadIssues: " + uploadIssues);
                new Global(context).writeBooleanInPreferences(Global.keyPreferencesUsyncSegnalation,uploadIssues);
            }
        });
    }

    //Classe per creare la mappa ordinata
    private class ValueComparator implements Comparator<String> {

        final Map<String, Integer> base;

        public ValueComparator(Map<String, Integer> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }

}

