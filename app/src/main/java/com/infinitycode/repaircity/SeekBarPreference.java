package com.infinitycode.repaircity;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Infinity Code on 25/08/2015.
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private static final int MAX = 40;
    private static final int MIN = 10;

    private TextView distanceText;

    private int distanceSet;

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBarPreference(Context context) {
        super(context);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);
        this.distanceSet = new Global(view.getContext()).getIntFromPreferences(Global.keyPreferencesSegnalationRange);
        SeekBar distanceBar = (SeekBar) view.findViewById(R.id.distance_seekbar);
        this.distanceText = (TextView) view.findViewById(R.id.distance_text_bar);
        distanceBar.setMax(MAX);
        distanceBar.setProgress(this.distanceSet - MIN);
        this.distanceText.setText(this.distanceSet + " km");
        distanceBar.setOnSeekBarChangeListener(this);
        return view;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newProgress = progress + MIN;
        this.distanceText.setText(newProgress + " km");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress() + MIN;
        new Global(getContext()).writeIntInPreferences(Global.keyPreferencesSegnalationRange, progress);
        if (this.distanceSet < progress) {
            new Global(this.getContext()).writeBooleanInPreferences(Global.keyPreferencesForcedSync, true);
        } else {
            new Global(this.getContext()).writeBooleanInPreferences(Global.keyPreferencesForcedSync, false);
        }
    }
}
