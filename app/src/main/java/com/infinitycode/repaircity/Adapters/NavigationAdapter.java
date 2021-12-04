package com.infinitycode.repaircity.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.infinitycode.repaircity.Global;
import com.infinitycode.repaircity.NavigationCell;
import com.infinitycode.repaircity.R;

import java.util.Collections;
import java.util.List;

/**
 * Created by Infinity Code on 14/08/15.
 */
public class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.NavigationViewHolder> {

    private final LayoutInflater inflater;
    private List<NavigationCell> data = Collections.emptyList();
    private final Context context;
    private ClickListener clickListener;

    public NavigationAdapter(Context context, List<NavigationCell> data) {
        inflater = LayoutInflater.from(context);
        this.data = data;
        this.context = context;
    }

    @Override
    public NavigationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.fragment_navigation_row, parent, false);
        return new NavigationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NavigationViewHolder holder, final int position) {
        NavigationCell current = data.get(position);
        holder.title.setText(current.title);
        Log.d("STAMPE", "posizione - > " + position + "; keypreferences -> " + new Global(this.context).getIntFromPreferences(Global.keyPreferencesCurrentActivity));
        if (position == new Global(this.context).getIntFromPreferences(Global.keyPreferencesCurrentActivity)) {
            Log.d("STAMPE", "posizione selezionata" + position);
            holder.parentView.setBackgroundColor(context.getResources().getColor(R.color.colorHighlight));
            holder.title.setTextColor(context.getResources().getColor(R.color.primary_dark));
            holder.icon.setImageResource(current.iconIdSelected);
        } else {
            Log.d("STAMPE", "posizione non selezionata" + position);
            holder.icon.setImageResource(current.iconIdUnselected);
            holder.title.setTextColor(context.getResources().getColor(R.color.secondary_text));
            holder.parentView.setBackgroundColor(context.getResources().getColor(R.color.transparent));
        }
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    class NavigationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView title;
        final ImageView icon;
        final View parentView;

        public NavigationViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            this.parentView = itemView;
            title = (TextView) itemView.findViewById(R.id.navListTitle);
            icon = (ImageView) itemView.findViewById(R.id.navListIcon);
        }

        @Override
        public void onClick(View view) {
            if (clickListener != null) {
                clickListener.itemClicked(view, getLayoutPosition());
            }
        }
    }

    public interface ClickListener {
        @SuppressWarnings("UnusedParameters")
        void itemClicked(View view, int position);
    }
}
