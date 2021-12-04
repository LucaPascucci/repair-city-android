package com.infinitycode.repaircity.adapters;

import android.content.Context;
import android.location.Location;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.infinitycode.repaircity.activity.ListActivity;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.R;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Infinity Code on 14/08/2015.
 */
public class SegnalationAdapter extends RecyclerView.Adapter<SegnalationAdapter.SegnalationViewHolder> {

    private List<ParseObject> segnalationList;
    private final LayoutInflater layoutInflater;
    private final Location location;
    //listener per gli item della lista
    private static ListActivity.MyFragment.ClickItemListener itemListener;

    public SegnalationAdapter(final Context context, final Location location, final ListActivity.MyFragment.ClickItemListener listener) {
        itemListener = listener;
        this.segnalationList = new ArrayList<>();
        this.layoutInflater = LayoutInflater.from(context);
        this.location = location;
    }

    public void swap(final List<ParseObject> segnalationList) {
        this.segnalationList.clear();
        this.segnalationList = segnalationList;
        this.notifyDataSetChanged();
    }

    @Override
    public SegnalationViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = this.layoutInflater.inflate(R.layout.segnalation_list_cell, parent, false);
        view.setOnClickListener(itemListener);
        return new SegnalationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SegnalationViewHolder holder, int position) {
        final ParseObject currentSegnalation = this.segnalationList.get(position);
        holder.title.setText(currentSegnalation.getString(Segnalazione.title));
        holder.description.setText(currentSegnalation.getString(Segnalazione.description));

        holder.segnalationId = currentSegnalation.getObjectId();
        ParseObject user = (ParseObject) currentSegnalation.get(Segnalazione.user);
        holder.userId = user.getObjectId();

        switch (currentSegnalation.getInt(Segnalazione.priority)) {
            case 1:
                holder.priorityImage.setImageResource(R.drawable.ic_yellowcircle);
                break;
            case 2:
                holder.priorityImage.setImageResource(R.drawable.ic_orangecircle);
                break;
            case 3:
                holder.priorityImage.setImageResource(R.drawable.ic_redcircle);
                break;
            default:
                break;
        }

        if (this.location != null) {
            final ParseGeoPoint segnalationPosition = currentSegnalation.getParseGeoPoint(Segnalazione.position);
            final ParseGeoPoint myLocationGeoPoint = new ParseGeoPoint(this.location.getLatitude(), this.location.getLongitude());
            final double distanceInKilometers = myLocationGeoPoint.distanceInKilometersTo(segnalationPosition);
            if (distanceInKilometers >= 0 && distanceInKilometers < 1) {
                holder.distance.setText(String.format("%.0f m", distanceInKilometers * 1000));
            } else {
                holder.distance.setText(String.format("%.1f km", distanceInKilometers));
            }
        }

    }

    @Override
    public int getItemCount() {
        return this.segnalationList.size();
    }

    public ParseObject getListItem(int itemPosition) {
        return segnalationList.get(itemPosition);
    }

    @SuppressWarnings("unused")
    class SegnalationViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView description;
        private final TextView distance;
        private final ImageView priorityImage;
        private String segnalationId;
        private String userId;

        public SegnalationViewHolder(final View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.title_text);
            this.description = (TextView) itemView.findViewById(R.id.description_text);
            this.distance = (TextView) itemView.findViewById(R.id.distance_text);
            this.priorityImage = (ImageView) itemView.findViewById(R.id.priority_image);
        }

    }
}
