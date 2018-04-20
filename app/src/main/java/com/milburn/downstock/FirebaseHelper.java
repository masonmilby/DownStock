package com.milburn.downstock;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private FirebaseAuth fireAuth;
    private FirebaseUser fireUser;

    private FirebaseFirestore fireStore;

    private FirebaseStorage fireStorage;
    private StorageReference firePagesRef;

    public interface taskFinished {
        void finished(Map<String, ProductDetails> productDetailsMap);
    }

    public FirebaseHelper() {
        fireAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        fireStorage = FirebaseStorage.getInstance();

        getCurrentUser();
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

    public void initStorage() {
        firePagesRef = fireStorage.getReference().child("pages");
    }

    public Map<String, ProductDetails> getSavedLists(final taskFinished taskFinished) {
        final Gson gson = new Gson();
        final Map<String, ProductDetails> productDetailsMap = new HashMap<>();

        DocumentReference userDocRef = fireStore.collection("lists").document(getCurrentUser().getUid());

        userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Map<String, Object> returnedObject = task.getResult().getData();
                    for (String listName : returnedObject.keySet()) {
                        ProductDetails productDetails = gson.fromJson((String)returnedObject.get(listName), ProductDetails.class);
                        productDetailsMap.put(listName, productDetails);
                    }
                    taskFinished.finished(productDetailsMap);
                }
            }
        });
        return productDetailsMap;
    }

    public void saveToFire(ProductDetails productDetails) {
        fireStore.collection("lists").document(getCurrentUser().getUid()).update("Main List", productDetails.toJson());
    }
}
