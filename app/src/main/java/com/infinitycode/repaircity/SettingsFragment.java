package com.infinitycode.repaircity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.view.inputmethod.InputMethodManager;

import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.infinitycode.repaircity.activity.InfoActivity;
import com.infinitycode.repaircity.walkthrough.WalkthroughActivity;

import java.util.List;

/**
 * Created by Infinity Code on 24/08/2015.
 */
public class SettingsFragment extends PreferenceFragment {

    private static final int PICK_MAIL = 1;
    private static final int PICK_NUMBER = 2;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_settings);

        final Preference facebook = findPreference("facebook_share");
        final Preference twitter = findPreference("twitter_share");
        final Preference mail = findPreference("mail_share");
        final Preference sms = findPreference("sms_share");
        final Preference repeatTutorial = findPreference("repeat_tutorial");
        final Preference info = findPreference("info_settings");

        facebook.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String urlToShare = "http://infinitycode.altervista.org/IconaSkyline.png";
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, urlToShare);

                boolean facebookAppFound = false;
                final List<ResolveInfo> matches = getActivity().getPackageManager().queryIntentActivities(intent, 0);
                for (final ResolveInfo info : matches) {
                    if (info.activityInfo.packageName.toLowerCase().startsWith("com.facebook.katana")) {
                        intent.setPackage(info.activityInfo.packageName);
                        facebookAppFound = true;
                        break;
                    }
                }

                if (facebookAppFound) {
                    SharePhoto photo = new SharePhoto.Builder()
                            .setBitmap(BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.ic_default_icon))
                            .build();
                    SharePhotoContent content = new SharePhotoContent.Builder()
                            .addPhoto(photo)
                            .build();

                    ShareDialog.show(getActivity(), content);
                } else {
                    final String sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=" + urlToShare;
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl));
                    startActivity(intent);
                }
                return true;
            }
        });

        twitter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Uri imageUri = Uri.parse("android.resource://com.infinitycode.repaircity/drawable/" + R.drawable.ic_default_icon);
                Intent tweetIntent = new Intent(Intent.ACTION_SEND);
                final String tweetMessage = "Prova Repair City per il tuo smartphone!";
                tweetIntent.putExtra(Intent.EXTRA_TEXT, tweetMessage);
                tweetIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                tweetIntent.setType("image/*");
                tweetIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                final List<ResolveInfo> resolvedInfoList = getActivity().getPackageManager().queryIntentActivities(tweetIntent, 0);

                boolean resolved = false;
                for (final ResolveInfo resolveInfo : resolvedInfoList) {
                    if (resolveInfo.activityInfo.packageName.startsWith("com.twitter.android")) {
                        tweetIntent.setClassName(
                                resolveInfo.activityInfo.packageName,
                                resolveInfo.activityInfo.name);
                        resolved = true;
                        break;
                    }
                }
                if (resolved) {
                    startActivity(tweetIntent);
                } else {
                    tweetIntent = new Intent();
                    tweetIntent.putExtra(Intent.EXTRA_TEXT, "This is a Test.");
                    tweetIntent.setAction(Intent.ACTION_VIEW);
                    tweetIntent.setData(Uri.parse("https://twitter.com/intent/tweet?text=" + tweetMessage));
                    startActivity(tweetIntent);
                }
                return true;
            }
        });

        mail.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final Intent mailIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI);
                startActivityForResult(mailIntent, PICK_MAIL);
                return true;
            }
        });

        sms.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Intent smsIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(smsIntent, PICK_NUMBER);
                return true;
            }
        });

        repeatTutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Intent walkthroughIntent = new Intent(getActivity(), WalkthroughActivity.class);
                startActivity(walkthroughIntent);
                return true;
            }
        });

        info.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Intent infoIntent = new Intent(getActivity(), InfoActivity.class);
                startActivity(infoIntent);
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
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            final Uri contact = data.getData();
            final Cursor cursor = getActivity().getContentResolver().query(contact, null, null, null, null);
            switch (requestCode) {
                case PICK_MAIL:
                    if (cursor.moveToFirst()) {
                        final String mail = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                        final Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", mail, null));
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Repair City");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, "Ciao,\n\nho appena scaricato Repair City sul mio dispositivo Android.\nÈ un servizio" +
                                " per segnalare problemi nella tua città.\nDisponibile per iOS e Android.");
                        startActivity(Intent.createChooser(emailIntent, "Manda email..."));
                        cursor.close();
                    }
                    break;
                case PICK_NUMBER:
                    if (cursor.moveToFirst()) {
                        final String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        final Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                        smsIntent.setData(Uri.parse("sms:" + phoneNumber));
                        smsIntent.putExtra("sms_body", "Prova Repair City per il tuo smartphone. Disponibile per iOS e Android.");
                        startActivity(smsIntent);
                        cursor.close();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void hideSoftKeyBoard() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (getActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }
}
