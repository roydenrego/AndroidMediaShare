package com.roydenrego.mediashare;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.GridView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static com.roydenrego.mediashare.Const.*;

public class MainActivity extends AppCompatActivity {


    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent data = getIntent();

        if (data != null && data.getExtras() != null) {
            // Initialize and show progress dialog if the app was opened via an url intent
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage("Downloading Media");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);

            //Start the download task
            final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
            downloadTask.execute(data.getExtras().getString("android.intent.extra.TEXT"));


            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //Stop the task if the dialog was cancelled
                    downloadTask.cancel(true); //cancel the task
                }
            });
        }

        GridView gridView = findViewById(R.id.gridView);

        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (directory != null) {
            File[] files = directory.listFiles();
            Log.d(TAG, "Size: " + files.length);

            ArrayList<Uri> images = new ArrayList<>();
            for (File file : files) {
                images.add(FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + "." + PACKAGE_NAME + ".provider", file));
            }

            PhotoAdapter adapter = new PhotoAdapter(MainActivity.this, images);
            gridView.setAdapter(adapter);
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;
        private File saveFile;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                Log.i(TAG, "Header Content-Type: " + connection.getHeaderField("Content-Type"));
                String ext = "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(connection.getHeaderField("Content-Type"));
                Log.i(TAG, "File Extension: " + ext);

                saveFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ext);
                output = new FileOutputStream(saveFile);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            else {
                Intent i = new Intent(Intent.ACTION_SEND).setType("image/*");


                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + "." + PACKAGE_NAME + ".provider", saveFile);

                i.putExtra(Intent.EXTRA_STREAM, photoURI);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, "Share Image"));

                ((Activity) context).finish();
            }
        }

    }
}
