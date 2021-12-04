package com.infinitycode.repaircity.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.infinitycode.repaircity.activity.ProfileActivity;
import com.infinitycode.repaircity.parse.Segnalazione;
import com.infinitycode.repaircity.R;
import com.malinskiy.superrecyclerview.swipe.BaseSwipeAdapter;
import com.malinskiy.superrecyclerview.swipe.SwipeLayout;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Infinity Code on 20/08/2015.
 */
public class ProfileAdapter extends BaseSwipeAdapter<ProfileAdapter.ProfileViewHolder> {

    private List<ParseObject> segnalationList;
    private final LayoutInflater layoutInflater;
    private SwipeLayout swipeLayout;
    private final Context context;

    private static ProfileActivity.MyProfileFragment.ClickItemListener itemListener;

    private int positionMarker;

    public ProfileAdapter(final Context context, final ProfileActivity.MyProfileFragment.ClickItemListener listener) {
        itemListener = listener;
        this.context = context;
        this.setMode(Mode.Single);
        this.segnalationList = new ArrayList<>();
        this.layoutInflater = LayoutInflater.from(context);
    }

    public void swap(final List<ParseObject> segnalationList, int positionMarker) {
        this.positionMarker = positionMarker;
        this.segnalationList.clear();
        this.segnalationList = segnalationList;
        this.notifyDataSetChanged();
    }

    @Override
    public ProfileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = this.layoutInflater.inflate(R.layout.segnalation_profile_cell, parent, false);
        view.setOnClickListener(itemListener);
        final ProfileViewHolder holder = new ProfileViewHolder(view);
        this.swipeLayout = holder.swipeLayout;
        this.swipeLayout.setDragEdge(SwipeLayout.DragEdge.Right);
        this.swipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ProfileViewHolder holder, final int position) {
        final ParseObject currentSegnalation = this.segnalationList.get(position);
        holder.title.setText(currentSegnalation.getString(Segnalazione.title));
        holder.description.setText(currentSegnalation.getString(Segnalazione.description));

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

        switch (this.positionMarker) {
            case 0:
                this.swipeLayout.setSwipeEnabled(false);
                break;
            case 1:
                holder.layout.setBackgroundResource(R.color.red);
                holder.confirmText.setText("Non risolta?");
                holder.confirmButton.setTextColor(this.context.getResources().getColor(R.color.red));
                holder.confirmButton.setOnClickListener(itemListener);
                break;
            case 2:
                holder.layout.setBackgroundResource(R.color.primary);
                holder.confirmText.setText("Risolta?");
                holder.confirmButton.setTextColor(this.context.getResources().getColor(R.color.primary));
                holder.confirmButton.setOnClickListener(itemListener);
                break;
            default:
                break;
        }

    }

    @Override
    public int getItemCount() {
        return this.segnalationList.size();
    }

    public ParseObject getListItem(int itemPosition) {
        return this.segnalationList.get(itemPosition);
    }

    class ProfileViewHolder extends BaseSwipeAdapter.BaseSwipeableViewHolder {

        private final TextView title;
        private final TextView description;
        private final ImageView priorityImage;

        private final LinearLayout layout;
        private final TextView confirmText;
        private final Button confirmButton;

        public ProfileViewHolder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.title_text_profile);
            this.description = (TextView) itemView.findViewById(R.id.description_text_profile);
            this.priorityImage = (ImageView) itemView.findViewById(R.id.priority_image_profile);

            this.layout = (LinearLayout) itemView.findViewById(R.id.layout_dismiss);
            this.confirmText = (TextView) itemView.findViewById(R.id.confirm_text_view);
            this.confirmButton = (Button) itemView.findViewById(R.id.confirm_button);
        }
    }



}
