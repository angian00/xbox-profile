package com.ag.android.xboxprofile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

public class PictureDownloader extends AsyncTask<String, Void, Bitmap> {
    private static final String TAG = "PictureDownloader";

    private final Listener mListener;

    public interface Listener {
        void onPictureDownloaded(Bitmap picture);
    }


    public PictureDownloader(Listener listener) {
        super();
        mListener = listener;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        String targetUrl = params[0];

        try {
            byte[] bitmapBytes = NetworkUtils.getUrl(targetUrl).getData();
            return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading profile image", ioe);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            mListener.onPictureDownloaded(bitmap);
        }
    }
}
