package com.milburn.downstock;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;

public class AsyncSaveImage extends AsyncTask<Object, Integer, Void> {

    private Context context;

    public AsyncSaveImage(Context con) {
        context = con;
    }

    @Override
    protected Void doInBackground(Object... objects) {
        Bitmap image = (Bitmap)objects[0];
        String pageId = (String)objects[1];

        File dirs = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/pages");
        dirs.mkdirs();
        File photo = new File(dirs.getAbsolutePath() + "/" + pageId + ".jpg");

        try {
            FileOutputStream fos = new FileOutputStream(photo);
            image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
    }
}
