package com.infinitycode.repaircity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

import com.infinitycode.repaircity.parse.Utente;
import com.parse.ParseException;
import com.parse.ParseObject;

/**
 * Created by Infinity Code on 26/08/2015.
 */
public class InfoFragment extends PreferenceFragment {

    private ParseObject currentUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_info);

        try {
            this.currentUser = new Utente(getActivity()).getLocalUserObject(new Global(getActivity()).getStringFromPreferences(Global.keyPreferencesFB));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        final Preference contactUs = findPreference("contact_us");

        contactUs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "infinitycode.dev@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Messagio da " + currentUser.getString(Utente.name));
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Salve Infinity Code,\n\nsto utilizzando la vostra applicazione Repair City e...");
                startActivity(Intent.createChooser(emailIntent, "Manda email..."));
                return true;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideSoftKeyBoard();
            }
        }, 300);

    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setPadding(0, 0, 0, 0);
        }
        return v;
    }

    private void hideSoftKeyBoard() {

        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (getActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

}
