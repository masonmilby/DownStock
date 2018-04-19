package com.milburn.downstock;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private FirebaseAuth fireAuth;
    private FirebaseUser fireUser;

    private FirebaseFirestore fireStore;

    private FirebaseStorage fireStorage;
    private StorageReference firePagesRef;

    public FirebaseHelper() {
        fireAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        fireStorage = FirebaseStorage.getInstance();

        getCurrentUser();
        initStorage();
        initDatabase();
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

    public void initDatabase() {
        final Map<String, Object> docMap = new HashMap<>();
        docMap.put("isAnon", getCurrentUser().isAnonymous());

        fireStore.collection("lists").document(getCurrentUser().getUid()).set(docMap);
    }

}
