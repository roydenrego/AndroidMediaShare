package com.roydenrego.imageshare;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent data = getIntent();

        if(data != null && data.getExtras() != null) {

            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setTitle("Progress");
            dialog.setMessage("Downloading Image....");
            dialog.show();

            String url = data.getExtras().getString("android.intent.extra.TEXT");

            Picasso.get().load(url).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    dialog.hide();

                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("image/*");

                    Uri photoURI = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".com.roydenrego.imageshare.provider", getLocalFile(bitmap));

                    i.putExtra(Intent.EXTRA_STREAM, photoURI);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(i, "Share Image"));

                    finish();
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
        }
    }

    public File getLocalFile(Bitmap bmp) {
        File file = null;
        try {
            file =  new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
