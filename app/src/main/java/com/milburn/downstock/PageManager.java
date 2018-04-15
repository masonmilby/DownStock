package com.milburn.downstock;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.content.FileProvider.getUriForFile;

public class PageManager {

    public static Uri getUri(Context context, String pageId) {
        File dirs = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/pages/");
        File photo = new File(dirs + "/" + pageId + ".jpg");

        if (photo.exists()) {
            return getUriForFile(context, "com.milburn.fileprovider", photo);
        }
        return null;
    }

    public static void cleanUp(Context context, ProductDetails productDetails) {
        List<String> idList = new ArrayList<>();
        for (ProductDetails.DetailedItem item : productDetails.getDetailedItems()) {
            idList.add(item.getPageId());
        }

        File dirs = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/pages/");
        File[] files = dirs.listFiles();
        for (File file : files) {
            String name = file.getName().replace(".jpg", "");
            if (!idList.contains(name)) {
                file.delete();
            }
        }
    }
}
