package com.milburn.downstock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.milburn.downstock.ProductDetails.DetailedItem;

import java.util.ArrayList;
import java.util.List;

public class RecyclerFragment extends Fragment {

    private Context activityContext;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerAdapter recyclerAdapter;
    private ProductDetails productDetails = new ProductDetails();

    private int selectedPosition = 0;
    private int swipedPosition = 0;
    private boolean selectionState = false;
    private DetailedItem selectedItem;
    private DetailedItem swipedItem;

    private DetailedItem removedItem;
    private List<DetailedItem> removedItems = new ArrayList<>();
    private int removedPosition = 0;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    private FileManager fileManager;
    private FirebaseHelper firebaseHelper;

    private boolean isFirstLaunch = true;

    @Override
    public void onAttach (Context context) {
        super.onAttach(context);
        this.activityContext = context;
        recyclerAdapter = new RecyclerAdapter(productDetails,this);
        recyclerAdapter.refreshData();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        fileManager = new FileManager(context);
        firebaseHelper = new FirebaseHelper();
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
        productDetails = fileManager.retrieveState();
        setupRecycler(productDetails);
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
        return productDetails;
    }

    public void queryApi(Object object, final boolean updateStock) {
        BBYApi bbyApi = new BBYApi(activityContext, new BBYApi.AsyncResponse() {
            @Override
            public void processFinish(Object object) {
                swipeRefresh.setRefreshing(false);
                if (object instanceof ProductDetails) {
                    ProductDetails returnedProducts = (ProductDetails)object;
                    if (updateStock && getView() != null) {
                        Snackbar.make(recyclerView, "Stock updated", Snackbar.LENGTH_SHORT).show();
                    }
                    if (recyclerAdapter == null) {
                        setupRecycler(productDetails);
                    } else {
                        recyclerAdapter.insertItems(returnedProducts.getDetailedItems());
                    }
                } else if (object instanceof DetailedItem) {
                    recyclerAdapter.insertItem(0, (DetailedItem)object);
                }
            }
        });
        bbyApi.execute(object);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefresh.setRefreshing(true);
            }
        });
    }

    public void openImage(String pageId) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(fileManager.getPageUri(pageId), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_swiped:
                recyclerAdapter.setShowSwiped(!recyclerAdapter.isShowSwiped());
                updateSwiped();
                break;

            case android.R.id.home:
                recyclerAdapter.selectAll(true);
                break;

            case R.id.delete_items:
                removeSelectedItems();
                break;

            case R.id.share_items:
                //
                break;

            case R.id.select_all_items:
                recyclerAdapter.selectAll(false);
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
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(recyclerAdapter.getShownItem(selectedPosition).getUrl()));
                startActivity(intent);
                break;

            case R.id.view_page:
                String pageId = recyclerAdapter.getShownItem(selectedPosition).getPageId();
                if (!pageId.equals("-1")) {
                    openImage(pageId);
                }
                break;

            case R.id.remove_item:
                removeItem(selectedPosition, true);
                break;

            case 0:
                //Remove from swiped
                recyclerAdapter.getShownItem(selectedPosition).resetSwiped();
                recyclerAdapter.refreshData();
                break;

            default:
                break;
        }
        return true;
    }

    private void setupRecycler(final ProductDetails prods) {
        this.productDetails = prods;
        recyclerAdapter = new RecyclerAdapter(productDetails, this);

        recyclerView.setLayoutManager(new CustomLinearLayoutManager(activityContext));
        recyclerView.setAdapter(recyclerAdapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                swipedPosition = viewHolder.getLayoutPosition();
                swipedItem = recyclerAdapter.getShownItem(swipedPosition);

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
                    if (!recyclerAdapter.isShowSwiped()) {
                        snackbar.show();
                    } else {
                        recyclerView.removeAllViews();
                    }
                    recyclerAdapter.setSelected(false, swipedItem);
                }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (productDetails.sizeDetailedItems() > 0) {
                    updateStock();
                } else {
                    swipeRefresh.setRefreshing(false);
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
        if (isFirstLaunch && productDetails.sizeDetailedItems() != 0) {
            isFirstLaunch = false;
            updateStock();
        }
    }

    public RecyclerAdapter getRecyclerAdapter() {
        return recyclerAdapter;
    }

    private void updateStock() {
        swipeRefresh.setRefreshing(true);
        queryApi(productDetails, true);
    }

    public void removeItem(int position, boolean showSnack) {
        removedPosition = position;
        removedItem = recyclerAdapter.getShownItem(position);

        recyclerAdapter.removeItem(position);

        if (showSnack) {
            Snackbar.make(recyclerView, "Item removed", Snackbar.LENGTH_LONG).setAction("Undo", new UndoRemoveListener()).show();
        }

        if (productDetails.sizeDetailedItems() == 0 && getCameraActivity() != null) {
            getCameraActivity().bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    public void removeSelectedItems() {
        removedItems.clear();
        removedItems.addAll(recyclerAdapter.getSelectedSet());
        recyclerAdapter.removeSelectedItems();

        Snackbar.make(recyclerView, "Items removed", Snackbar.LENGTH_LONG).setAction("Undo", new UndoRemoveAllListener()).show();
    }

    public void selectItem(int position) {
        DetailedItem detailedItem = recyclerAdapter.getShownItem(position);
        recyclerAdapter.setSelected(!recyclerAdapter.isSelected(detailedItem), detailedItem);
    }

    public void setItemSelected(boolean selected, DetailedItem item) {
        recyclerAdapter.setSelected(selected, item);
    }

    public void updateSwiped() {
        CameraActivity cameraActivity = getCameraActivity();

        if (cameraActivity != null) {
            if (productDetails != null) {
                if (recyclerAdapter.isShowSwiped() && productDetails.sizeSwipedItems() == 0) {
                    recyclerAdapter.setShowSwiped(false);
                }
                if (cameraActivity.showSwipedItems != null) {
                    cameraActivity.showSwipedItems.setVisible(productDetails.sizeSwipedItems() != 0);
                    cameraActivity.showSwipedItems.setIcon(recyclerAdapter.isShowSwiped() ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
                }
            }
        }
    }

    public void updateSelectionState() {
        selectionState = recyclerAdapter.selectedSetSize() != 0;
        CameraActivity cameraActivity = getCameraActivity();

        if (cameraActivity != null) {
            cameraActivity.supportInvalidateOptionsMenu();

            if (selectionState) {
                cameraActivity.toolbar.setTitle(recyclerAdapter.selectedSetSize() + " selected");
            }
        }
    }

    @Override
    public void onPause() {
        if (recyclerAdapter != null) {
            fileManager.saveState(productDetails);
            firebaseHelper.saveToFire(productDetails);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (recyclerAdapter != null) {
            fileManager.saveState(productDetails);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (recyclerAdapter != null) {
            fileManager.saveState(productDetails);
        }
        fileManager.cleanUp(productDetails);
        super.onDestroy();
    }

    class UndoSwipeListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            swipedItem.resetSwiped();
            setItemSelected(false, swipedItem);
            recyclerView.smoothScrollToPosition(swipedPosition);
        }
    }

    class UndoRemoveListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            recyclerAdapter.insertItem(removedPosition, removedItem);
        }
    }

    class UndoRemoveAllListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            recyclerAdapter.insertItems(removedItems);
        }
    }
}
