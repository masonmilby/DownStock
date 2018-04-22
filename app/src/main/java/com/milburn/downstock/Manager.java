package com.milburn.downstock;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.support.v4.content.FileProvider.getUriForFile;

public class Manager {

    private Context context;
    private Gson gson;
    private SharedPreferences sharedPreferences;

    private File stateFile;
    private File pageDirFile;

    private FirebaseAuth fireAuth;
    private FirebaseUser fireUser;

    private FirebaseFirestore fireStore;

    private FirebaseStorage fireStorage;
    private StorageReference firePagesRef;

    public interface OnCreateListCompleted {
        void finished();
    }

    public Manager(Context context) {
        this.context = context;
        gson = new GsonBuilder().enableComplexMapKeySerialization()
                .setPrettyPrinting().create();
        PreferenceManager.setDefaultValues(context.getApplicationContext(), R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        stateFile = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/savedstate");
        pageDirFile = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/pages/");
        pageDirFile.mkdirs();

        fireAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        fireStorage = FirebaseStorage.getInstance();

        getCurrentUser();
    }

    public String getStoreId() {
        return sharedPreferences.getString("store_id_pref", "0");
    }

    public List<String[]> getReferences() {
        Type typeOfMap = new TypeToken<List<String[]>>(){ }.getType();
        List<String[]> refs = gson.fromJson(sharedPreferences.getString("referencepair", ""), typeOfMap);
        return refs != null ? refs : new ArrayList<String[]>();
    }

    public void setReferences(List<String[]> references) {
        sharedPreferences.edit()
                .putString("referencepair", gson.toJson(references))
                .apply();
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

    public FirebaseUser getCurrentUser() {
        fireUser = fireAuth.getCurrentUser();
        if (fireUser == null) {
            fireAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        fireUser = fireAuth.getCurrentUser();
                    } else {
                        Log.e("Firebase Auth Error", task.getException().getMessage());
                    }
                }
            });
        }
        return fireUser;
    }

    public void createList(final String listName, ProductDetails productDetails, final OnCreateListCompleted onCreateListCompleted) {
        Map<String, String> listMap = new HashMap<>();
        listMap.put("productdetails", productDetails.toJson());
        fireStore.collection("lists").document(getCurrentUser().getUid()).collection("lists").document(listName).set(listMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                List<String[]> refs = getReferences();
                refs.add(new String[]{listName, getCurrentUser().getUid()});
                setReferences(refs);
                onCreateListCompleted.finished();
            }
        });
    }

    public void saveList(ProductDetails productDetails, String[] reference) {
        Map<String, String> listMap = new HashMap<>();
        listMap.put("productdetails", productDetails.toJson());
        fireStore.collection("lists").document(reference[1]).collection("lists").document(reference[0]).set(listMap);
    }

    public DocumentReference getDocReference(String[] reference) {
        return fireStore.collection("lists").document(reference[1]).collection("lists").document(reference[0]);
    }

    public void initStorage() {
        firePagesRef = fireStorage.getReference().child("pages");
    }
}
