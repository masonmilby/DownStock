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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

public class Manager {

    private SharedPreferences sharedPreferences;

    private File pageDirFile;

    private FirebaseAuth fireAuth;
    private FirebaseFirestore fireStore;
    private FirebaseStorage fireStorage;

    private CollectionReference listsReference;
    private CollectionReference userInfoReference;
    private StorageReference pagesReference;

    public interface OnSaveListCompleted {
        void finished();
    }

    public interface OnSignedIn {
        void finished(FirebaseUser user);
    }

    public interface OnGetReferences {
        void finished(MapListReferences references);
    }

    public interface OnAddedReference {
        void finished();
    }

    public interface OnDeletedReference {
        void finished();
    }

    public interface OnGetPageExists {
        void finished(boolean exists);
    }

    public interface OnGetListExists {
        void finished(boolean exists, String name);
    }

    public interface OnGetPageUri {
        void finished(Uri uri);
    }

    public interface OnDeletedList {
        void finished();
    }

    public interface OnFinishedAction {
        void finished(boolean added);
    }

    public Manager(Context context) {
        PreferenceManager.setDefaultValues(context.getApplicationContext(), R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        pageDirFile = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/pages/");
        pageDirFile.mkdirs();

        fireAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        fireStorage = FirebaseStorage.getInstance();

        listsReference = fireStore.collection("lists");
        userInfoReference = fireStore.collection("userinfo");
        pagesReference = fireStorage.getReference().child("pages");
    }

    public String getStoreId() {
        return sharedPreferences.getString("store_id_pref", "0");
    }

    public void getPageUri(String pageId, final OnGetPageUri onGetPageUri) {
        StorageReference newImage = pagesReference.child(pageId + ".jpg");
        newImage.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    onGetPageUri.finished(task.getResult());
                } else {
                    onGetPageUri.finished(null);
                }
            }
        });
    }

    public void savePage(Bitmap bitmap, String pageId) {
        StorageReference newImage = pagesReference.child(pageId + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        newImage.putBytes(baos.toByteArray());
    }

    private void deletePage(String pageId) {
        StorageReference newImage = pagesReference.child(pageId + ".jpg");
        newImage.delete();
    }

    public void deleteList(final ListReference reference, final ProductDetails productDetails, final OnDeletedList onDeletedList) {
        getCurrentUser(new OnSignedIn() {
            @Override
            public void finished(final FirebaseUser user) {
                for (String id : productDetails.getAllPageIds()) {
                    deletePage(id);
                }
                deleteReference(reference, new OnDeletedReference() {
                    @Override
                    public void finished() {
                        if (user.getUid().contentEquals(reference.getUserId())) {
                            listsReference.document(reference.getRefCode()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    onDeletedList.finished();
                                }
                            });
                        } else {
                            onDeletedList.finished();
                        }
                    }
                });
            }
        });
    }

    public void getPageExists(String pageId, final OnGetPageExists onGetPageExists) {
        StorageReference newImage = pagesReference.child(pageId + ".jpg");
        newImage.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                onGetPageExists.finished(task.isSuccessful());
            }
        });
    }

    public void getReferences(final OnGetReferences onGetReferences) {
        getCurrentUser(new OnSignedIn() {
            @Override
            public void finished(FirebaseUser user) {
                if (user != null) {
                    userInfoReference.document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult().get("References") != null) {

                                List<String> returnedRefs = (List<String>)task.getResult().get("References");
                                MapListReferences mapListReferences = new MapListReferences(returnedRefs);
                                onGetReferences.finished(mapListReferences);
                            } else {
                                onGetReferences.finished(new MapListReferences(null));
                            }
                        }
                    });
                } else {
                    onGetReferences.finished(new MapListReferences(null));
                }
            }
        });
    }

    private void addReference(final ListReference reference, final OnAddedReference onAddedReference) {
        getCurrentUser(new OnSignedIn() {
            @Override
            public void finished(final FirebaseUser user) {
                getReferences(new OnGetReferences() {
                    @Override
                    public void finished(MapListReferences returnedReferences) {
                        returnedReferences.addListReference(reference);
                        userInfoReference.document(user.getUid()).set(returnedReferences.getFinalMapReferences()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                onAddedReference.finished();
                            }
                        });
                    }
                });
            }
        });
    }

    public void deleteReference(final ListReference reference, final OnDeletedReference onDeletedReference) {
        getCurrentUser(new OnSignedIn() {
            @Override
            public void finished(final FirebaseUser user) {
                getReferences(new OnGetReferences() {
                    @Override
                    public void finished(MapListReferences returnedReferences) {
                        returnedReferences.removeListReference(reference);
                        userInfoReference.document(user.getUid()).set(returnedReferences.getFinalMapReferences()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (onDeletedReference != null) {
                                    onDeletedReference.finished();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    public void getCurrentUser(final OnSignedIn onSignedIn) {
        final FirebaseUser fireUser = fireAuth.getCurrentUser();
        if (fireUser == null) {
            fireAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        onSignedIn.finished(fireAuth.getCurrentUser());
                    } else {
                        Log.e("Firebase Auth Error", task.getException().getMessage());
                        onSignedIn.finished(null);
                    }
                }
            });
        } else {
            onSignedIn.finished(fireUser);
        }
    }

    public void saveList(ProductDetails productDetails, final ListReference reference, final OnSaveListCompleted onSaveListCompleted) {
        listsReference.document(reference.getRefCode()).set(reference.createMap(productDetails))
        .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    addReference(reference, new OnAddedReference() {
                        @Override
                        public void finished() {
                            if (onSaveListCompleted != null) {
                                onSaveListCompleted.finished();
                            }
                        }
                    });
                } else {
                    if (onSaveListCompleted != null) {
                        onSaveListCompleted.finished();
                    }
                }
            }
        });
    }

    public void getListExists(final String listName, final OnGetListExists onGetListExists) {
        getCurrentUser(new OnSignedIn() {
            @Override
            public void finished(final FirebaseUser user) {
                getListDocReference(new ListReference(listName, user.getUid())).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        onGetListExists.finished(task.getResult().exists(), listName);
                    }
                });
            }
        });
    }

    public void checkExistsAndAdd(final ListReference listReference, final OnFinishedAction onFinishedAction) {
        getListDocReference(listReference).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    addReference(listReference, new OnAddedReference() {
                        @Override
                        public void finished() {
                            onFinishedAction.finished(true);
                        }
                    });
                } else {
                    onFinishedAction.finished(false);
                }
            }
        });
    }

    public DocumentReference getListDocReference(ListReference listReference) {
        return listsReference.document(listReference.getRefCode());
    }
}
