package com.infinitycode.repaircity.parse;

import android.content.Context;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Created by Infinity Code on 12/08/2015.
 */
public class Valutazione {

    public static final String className = "Valutazione";
    public static final String likeNotLike = "like_NotLike";
    private static final String user = "utenteCollegato";
    public static final String segnalation = "segnalazioneCollegata";
    private static final String objectId = "objectId";
    private static final String valutationNotSaved = "newValutation";

    private final Context context;

    public Valutazione(Context context) {
        this.context = context;
    }

    public void insert(final int likeNotLike, final ParseObject user, final ParseObject segnalation) {
        final ParseObject newValutation = new ParseObject(Valutazione.className);
        newValutation.put(Valutazione.likeNotLike, likeNotLike);
        newValutation.put(Valutazione.user, user);
        newValutation.put(Valutazione.segnalation, segnalation);

        try {
            newValutation.save();
            newValutation.pin();
        } catch (ParseException e) {
            e.printStackTrace();
            try {
                newValutation.pin(Valutazione.valutationNotSaved);
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void checkValutationExistOnline(final ParseObject user, final ParseObject segnalation) throws ParseException {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Valutazione.className);
        query.whereEqualTo(Valutazione.user, user);
        query.whereEqualTo(Valutazione.segnalation, segnalation);
        query.getFirst();
    }

    private void checkValutationExistLocal(final ParseObject user, final ParseObject segnalation) throws ParseException {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Valutazione.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Valutazione.user, user);
        query.whereEqualTo(Valutazione.segnalation, segnalation);
        query.getFirst();
    }

    @SuppressWarnings("unused")
    public ParseObject getValutationFromObjectID(final String valutationObjectId) throws ParseException {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Valutazione.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Valutazione.objectId, valutationObjectId);
        return query.getFirst();
    }

    public int getValutationType(final ParseObject user, final ParseObject segnalation) {
        try {
            this.checkValutationExistLocal(user, segnalation);
            ParseQuery<ParseObject> query = ParseQuery.getQuery(Valutazione.className);
            query.fromLocalDatastore();
            query.whereEqualTo(Valutazione.user, user);
            query.whereEqualTo(Valutazione.segnalation, segnalation);
            final ParseObject result = query.getFirst();
            return result.getInt(Valutazione.likeNotLike); //la valutazione esiste, torno il tipo di valutazione
        } catch (ParseException e) { //la valutazione non esiste, torno 0
            e.printStackTrace();
            return 0;
        }
    }

    public int getNumberOfLikesOrDislikes(final ParseObject segnalation, final int likeOrNotLike) {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Valutazione.className);
        query.fromLocalDatastore();
        if (segnalation != null) {
            query.whereEqualTo(Valutazione.segnalation, segnalation);
        }
        query.whereEqualTo(Valutazione.likeNotLike, likeOrNotLike);
        final int results;
        try {
            results = query.count();
            return results;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getNumberOfLikeOrDislikesOfAnUser(final ParseObject user, final int likeOrNotLike) {
        final List<ParseObject> segnalations = new Segnalazione(this.context).getSegnalationsOfAnUser(user, null);
        int counter = 0;
        for (ParseObject segnalation : segnalations) {
            counter += this.getNumberOfLikesOrDislikes(segnalation, likeOrNotLike);
        }
        return counter;
    }

    public void updateValutation(final ParseObject user, final ParseObject segnalation, final int likeOrDislike) {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Valutazione.className);
        query.fromLocalDatastore();
        query.whereEqualTo(Valutazione.user, user);
        query.whereEqualTo(Valutazione.segnalation, segnalation);
        try {
            ParseObject result = query.getFirst();
            result.put(Valutazione.likeNotLike, likeOrDislike);
            result.save();
            result.pin();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public List<ParseObject> updateWithOnline(List<ParseObject> segnalationsArray) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Valutazione.className);
        query.whereContainedIn(Valutazione.segnalation, segnalationsArray);
        try {
            List<ParseObject> results = query.find();
            for (ParseObject record : results) {
                record.pin();
            }
            return results;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
