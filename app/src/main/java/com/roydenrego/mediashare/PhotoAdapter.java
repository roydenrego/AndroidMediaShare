package com.roydenrego.mediashare;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class PhotoAdapter extends BaseAdapter {

    private Context ctx;
    private ArrayList<Uri> images;

    public PhotoAdapter(Context ctx, ArrayList<Uri> images) {
        this.images = images;
        this.ctx = ctx;
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public Object getItem(int i) {
        return images.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {

        View view = convertView;
        if (view == null) {
            LayoutInflater li = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.list_photo_item, null);
        }

        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;

            view.setLayoutParams(new RelativeLayout.LayoutParams(width / 3 - 5, width / 3 - 5));
        }

        ImageView imageView = view.findViewById(R.id.albumImage);
        imageView.setImageURI(images.get(i));

        return view;

    }
}
