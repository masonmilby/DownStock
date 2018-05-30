package com.milburn.downstock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseUser;
import com.milburn.downstock.ProductDetails.DetailedItem;

import java.io.File;
import java.util.Set;

import static android.support.v4.content.FileProvider.getUriForFile;

public class RecyclerFragment extends Fragment {

    private Context activityContext;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerAdapter recyclerAdapter;

    private int selectedPosition = 0;
    private int swipedPosition = 0;
    private boolean selectionState = false;
    private DetailedItem selectedItem;
    private DetailedItem swipedItem;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    private Manager manager;

    @Override
    public void onAttach (Context context) {
        super.onAttach(context);
        this.activityContext = context;
        recyclerAdapter = new RecyclerAdapter(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        manager = new Manager(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_recycler, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        recyclerView = getView().findViewById(R.id.recycler);
        swipeRefresh = getView().findViewById(R.id.swipeRefresh);
    }

    @Override
    public void onStart() {
        super.onStart();
        setupRecycler();
    }

    private CameraActivity getCameraActivity() {
        if (activityContext instanceof CameraActivity) {
            return (CameraActivity)activityContext;
        }
        return null;
    }

    public boolean isSelectionState() {
        return selectionState;
    }

    public int setSelectedPosition(int position) {
        return selectedPosition = position;
    }

    public ProductDetails getProductDetails() {
        return getRecyclerAdapter().getProductDetails();
    }

    public void queryApi(ProductDetails sendingProducts, final boolean updateStock) {
        BBYApi bbyApi = new BBYApi(activityContext, new BBYApi.AsyncResponse() {
            @Override
            public void processFinish(ProductDetails result) {
                setRefreshing(false);
                if (updateStock && getView() != null) {
                    Snackbar.make(recyclerView, "Stock updated", Snackbar.LENGTH_SHORT).show();
                }
                getRecyclerAdapter().insertItems(result.getDetailedItems());
            }
        });
        bbyApi.execute(sendingProducts);
        setRefreshing(true);
    }

    public void openImage(String pageId) {
        setRefreshing(true);
        manager.getPageUri(pageId, new Manager.OnGetPageUri() {
            @Override
            public void finished(Uri uri) {
                if (uri != null) {
                    Glide.with(activityContext).asFile().load(uri).into(new SimpleTarget<File>() {
                        @Override
                        public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            Uri finalUri = getUriForFile(activityContext, "com.milburn.fileprovider", resource);
                            intent.setDataAndType(finalUri, "image/*");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                            setRefreshing(false);
                        }
                    });
                } else {
                    setRefreshing(false);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_swiped:
                getRecyclerAdapter().setShowSwiped(!getRecyclerAdapter().isShowSwiped());
                updateSwiped();
                break;

            case android.R.id.home:
                getRecyclerAdapter().selectAll(true);
                break;

            case R.id.delete_items:
                removeSelectedItems();
                break;

            case R.id.share_list:
                shareCurrentList(getRecyclerAdapter().getSelectedReference());
                break;

            case R.id.new_list_selected:
                selectedToNewList(getRecyclerAdapter().getSelectedSet());
                break;

            case R.id.select_all_items:
                getRecyclerAdapter().selectAll(false);
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onContextItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_url:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getRecyclerAdapter().getShownItem(selectedPosition).getUrl()));
                startActivity(intent);
                break;

            case R.id.view_page:
                String pageId = getRecyclerAdapter().getShownItem(selectedPosition).getPageId();
                if (!pageId.equals("-1")) {
                    openImage(pageId);
                }
                break;

            case R.id.remove_item:
                removeItem(selectedPosition, true);
                break;

            case 0:
                //Remove from swiped
                getRecyclerAdapter().removeFromSwiped(selectedPosition);
                break;

            default:
                break;
        }
        return true;
    }

    public void shareCurrentList(final ListReference listReference) {
        if (listReference != null) {
            manager.getCurrentUser(new Manager.OnSignedIn() {
                @Override
                public void finished(FirebaseUser user) {
                    QRDialogFragment qrDialogFragment = new QRDialogFragment();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("is_new", false);
                    bundle.putString("list_reference", listReference.createString());
                    qrDialogFragment.setArguments(bundle);
                    qrDialogFragment.show(getFragmentManager(), "qrDialog");
                }
            });
        }
    }

    public void selectedToNewList(final Set<DetailedItem> selectedSet) {
        QRDialogFragment qrDialogFragment = new QRDialogFragment();
        qrDialogFragment.setListener(new QRDialogFragment.OnSaveListName() {
            @Override
            public void saveName(final String name) {
                final ProductDetails tempSelected = new ProductDetails();
                for (DetailedItem item : selectedSet) {
                    tempSelected.addDetailedItem(0, item);
                }
                manager.getCurrentUser(new Manager.OnSignedIn() {
                    @Override
                    public void finished(FirebaseUser user) {
                        final ListReference newReference = new ListReference(name, user.getUid());
                        manager.saveList(tempSelected, newReference, new Manager.OnSaveListCompleted() {
                            @Override
                            public void finished() {
                                getRecyclerAdapter().refreshData(newReference);
                            }
                        });
                    }
                });
            }
        });
        Bundle bundle = new Bundle();
        bundle.putBoolean("is_new", true);
        qrDialogFragment.setArguments(bundle);
        qrDialogFragment.show(getFragmentManager(), "qrDialog");
    }

    public void showSnack(String message, int length) {
        Snackbar.make(recyclerView, message, length).show();
    }

    private void setupRecycler() {
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(activityContext));
        recyclerView.setAdapter(getRecyclerAdapter());

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                swipedPosition = viewHolder.getLayoutPosition();
                swipedItem = getRecyclerAdapter().getShownItem(swipedPosition);

                    Snackbar snackbar = Snackbar.make(recyclerView, "0", Snackbar.LENGTH_LONG);
                    snackbar.setAction("Undo", new UndoSwipeListener());

                    switch (direction) {
                        case ItemTouchHelper.LEFT:
                            swipedItem.setDeltabusted(true);
                            snackbar.setText("Product deltabusted");
                            break;

                        case ItemTouchHelper.RIGHT:
                            swipedItem.setFound(true);
                            snackbar.setText("Product found");
                            break;
                    }
                    if (!getRecyclerAdapter().isShowSwiped()) {
                        snackbar.show();
                    } else {
                        recyclerView.removeAllViews();
                    }
                    getRecyclerAdapter().setSelected(false, swipedItem);
                    updateSwiped();
                }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (getRecyclerAdapter().getProductDetails().sizeDetailedItems() > 0) {
                    queryApi(getRecyclerAdapter().getProductDetails(), true);
                } else {
                    setRefreshing(false);
                }
            }
        });

        sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("store_id_pref")) {
                    updateStock();
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        updateSelectionState();
        updateSwiped();
        updateStock();
    }

    public RecyclerAdapter getRecyclerAdapter() {
        return recyclerAdapter;
    }

    public void setRefreshing(final boolean refreshing) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    swipeRefresh.setRefreshing(refreshing);
                }
            });
        }
    }

    private void updateStock() {
        if (getRecyclerAdapter().getProductDetails().sizeDetailedItems() > 0) {
            queryApi(getRecyclerAdapter().getProductDetails(), true);
        }
    }

    public void removeItem(int position, boolean showSnack) {
        getRecyclerAdapter().removeItem(position);

        if (showSnack) {
            Snackbar.make(recyclerView, "Item removed", Snackbar.LENGTH_LONG).setAction("Undo", new UndoRemoveListener()).show();
        }

        if (getRecyclerAdapter().getProductDetails().sizeDetailedItems() == 0 && getCameraActivity() != null) {
            getCameraActivity().bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    public void removeSelectedItems() {
        getRecyclerAdapter().removeSelectedItems();
        Snackbar.make(recyclerView, "Items removed", Snackbar.LENGTH_LONG).setAction("Undo", new UndoRemoveAllListener()).show();
    }

    public void updateSwiped() {
        CameraActivity cameraActivity = getCameraActivity();

        if (cameraActivity != null) {
            if (cameraActivity.showSwipedItems != null) {
                cameraActivity.showSwipedItems.setVisible(getRecyclerAdapter().getProductDetails().sizeSwipedItems() > 0);
                cameraActivity.showSwipedItems.setIcon(getRecyclerAdapter().isShowSwiped() ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            }
        }
    }

    public void updateSelectionState() {
        selectionState = getRecyclerAdapter().selectedSetSize() != 0;
        CameraActivity cameraActivity = getCameraActivity();

        if (cameraActivity != null) {
            cameraActivity.supportInvalidateOptionsMenu();

            if (selectionState) {
                cameraActivity.toolbar.setTitle(getRecyclerAdapter().selectedSetSize() + " selected");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    class UndoSwipeListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            swipedItem.resetSwiped();
            getRecyclerAdapter().setSelected(false, swipedItem);
            recyclerView.smoothScrollToPosition(swipedPosition);
        }
    }

    class UndoRemoveListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            getRecyclerAdapter().undoRemove();
        }
    }

    class UndoRemoveAllListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            getRecyclerAdapter().undoRemoveAll();
        }
    }
}
