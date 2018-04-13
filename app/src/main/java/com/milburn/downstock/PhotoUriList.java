package com.milburn.downstock;

import android.net.Uri;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class PhotoUriList {
    private List<String> uriList = new ArrayList<>();

    public void addUri(Uri uri) {
        uriList.add(uri.toString());
    }

    public Uri getUri(int position) {
        return Uri.parse(uriList.get(position));
    }

    public int size() {
        return uriList.size();
    }

    public void clear() {
        uriList.clear();
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this, PhotoUriList.class);
    }
}
