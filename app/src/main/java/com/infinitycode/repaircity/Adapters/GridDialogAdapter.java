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
 * Created by Infinity Code on 21/08/15.
 */

public class GridDialogAdapter extends BaseAdapter {

    private final LayoutInflater layoutInflater;

    public GridDialogAdapter(Context context) {
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return 2;
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
            view = this.layoutInflater.inflate(R.layout.dialog_grid_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) view.findViewById(R.id.text_grid_item);
            viewHolder.imageView = (ImageView) view.findViewById(R.id.image_grid_item);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        Context context = parent.getContext();
        switch (position) {
            case 0:
                viewHolder.textView.setText(context.getString(R.string.camera));
                viewHolder.imageView.setImageResource(R.drawable.ic_camera);
                break;
            case 1:
                viewHolder.textView.setText(context.getString(R.string.photos));
                viewHolder.imageView.setImageResource(R.drawable.ic_photos);
                break;
        }
        return view;
    }

    static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }

}
