package com.milburn.downstock;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.content.FileProvider.getUriForFile;

public class FileManager {

    private Context context;
    private Gson gson;
    private File stateFile;
    private File pageDirFile;

    public FileManager(Context context) {
        this.context = context;
        gson = new Gson();
        stateFile = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/savedstate");
        pageDirFile = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/pages/");
        pageDirFile.mkdirs();
    }

    public Uri getPageUri(String pageId) {
        File page = new File(pageDirFile + "/" + pageId + ".jpg");
        if (page.exists()) {
            return getUriForFile(context, "com.milburn.fileprovider", page);
        }
        return null;
    }

    public boolean getPageExists(String pageId) {
        File page = new File(pageDirFile + "/" + pageId + ".jpg");
        return page.exists();
    }

    public void savePage(Bitmap bitmap, String pageId) {
        AsyncSavePage asyncSaveImage = new AsyncSavePage(context);
        asyncSaveImage.execute(bitmap, pageId);
    }

    public void cleanUp(ProductDetails productDetails) {
        List<String> idList = new ArrayList<>();
        for (ProductDetails.DetailedItem item : productDetails.getDetailedItems()) {
            idList.add(item.getPageId());
        }

        File[] files = pageDirFile.listFiles();
        for (File file : files) {
            String name = file.getName().replace(".jpg", "");
            if (!idList.contains(name)) {
                file.delete();
            }
        }
    }

    public boolean saveStateExists() {
        return stateFile.exists();
    }

    public void saveState(ProductDetails productDetails) {
        if (productDetails != null && productDetails.sizeDetailedItems() != 0) {
            try {
                FileOutputStream fos = new FileOutputStream(stateFile);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(productDetails.toJson());
                osw.close();
                fos.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        } else {
            deleteState();
        }
    }

    public ProductDetails retrieveState() {
        ProductDetails temp = null;
        StringBuffer stringBuffer = new StringBuffer();
        if (stateFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(stateFile);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(isr);

                String fileString = bufferedReader.readLine();
                while (fileString != null) {
                    stringBuffer.append(fileString);
                    fileString = bufferedReader.readLine();
                }
                isr.close();
                fis.close();

            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
            temp = gson.fromJson(stringBuffer.toString(), ProductDetails.class);
        }
        return temp != null ? temp : new ProductDetails();
    }

    private void deleteState() {
        stateFile.delete();
    }
}
