package com.infinitycode.repaircity.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.infinitycode.repaircity.R;

/**
 * Created by InfinityCode on 26/08/15.
 */

public class ListDialogAdapter extends BaseAdapter {

    private final LayoutInflater layoutInflater;
    private final boolean segnalationResolved;

    public ListDialogAdapter(Context context,boolean value) {
        this.segnalationResolved = value;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        if (segnalationResolved){
            return 2;
        }
        return 3;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        View view = convertView;

        if (view == null) {
            view = this.layoutInflater.inflate(R.layout.dialog_list_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) view.findViewById(R.id.text_list_item);
            viewHolder.imageView = (ImageView) view.findViewById(R.id.image_list_item);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        Context context = parent.getContext();
        switch (position) {
            case 0:
                if (this.segnalationResolved) {
                    viewHolder.textView.setText(context.getString(R.string.facebook));
                    viewHolder.imageView.setImageResource(R.drawable.ic_facebook_blue_big);
                }else {
                    viewHolder.textView.setText(context.getString(R.string.resolve));
                    viewHolder.imageView.setImageResource(R.drawable.ic_confirm_green);
                }
                break;
            case 1:
                if (this.segnalationResolved){
                    viewHolder.textView.setText(context.getString(R.string.twitter));
                    viewHolder.imageView.setImageResource(R.drawable.ic_twitter_big);
                }else {
                    viewHolder.textView.setText(context.getString(R.string.facebook));
                    viewHolder.imageView.setImageResource(R.drawable.ic_facebook_blue_big);
                }
                break;
            case 2:
                viewHolder.textView.setText(context.getString(R.string.twitter));
                viewHolder.imageView.setImageResource(R.drawable.ic_twitter_big);
                break;
        }
        return view;
    }

    static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }
}
