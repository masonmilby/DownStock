package com.milburn.downstock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.milburn.downstock.ProductDetails.DetailedItem;
import com.milburn.downstock.ProductDetails.BasicItem;

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

    @Override
    public void onAttach (Context context) {
        super.onAttach(context);
        this.activityContext = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        fileManager = new FileManager(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.activity_main_recycler, container, false);
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
        if (recyclerAdapter == null) {
            setupRecycler(productDetails);
        } else {
            recyclerAdapter.refreshData(productDetails);
        }
    }

    public boolean isSelectionState() {
        return selectionState;
    }

    public int setSelectedPosition(int position) {
        return selectedPosition = position;
    }

    public MenuInflater getActivityMenuInflater() {
        if (activityContext instanceof MainActivity) {
            return ((MainActivity) activityContext).getMenuInflater();
        } else if (activityContext instanceof CameraActivity) {
            return ((CameraActivity) activityContext).getMenuInflater();
        }
        return null;
    }

    public boolean supportsAdvancedOptions() {
        return activityContext instanceof MainActivity;
    }

    public MainActivity getMainActivity() {
        if (supportsAdvancedOptions()) {
            return (MainActivity)activityContext;
        }
        return null;
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
                        insertItems(returnedProducts.getDetailedItems());
                    }
                } else if (object instanceof DetailedItem) {
                    insertItem(0, (DetailedItem)object);
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

    class UndoSwipeListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            swipedItem.resetSwiped();
            swipedItem.setSelected(false);
            recyclerAdapter.refreshData();
            recyclerView.smoothScrollToPosition(swipedPosition);
        }
    }

    class UndoRemoveListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            insertItem(removedPosition, removedItem);
        }
    }

    class UndoRemoveAllListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            insertItems(removedItems);
        }
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
            case R.id.view_page:
                break;

            case R.id.show_swiped:
                setSwiped(!productDetails.isShowSwiped());
                updateSwiped();
                break;

            case android.R.id.home:
                selectAll(true);
                break;

            case R.id.delete_items:
                removeSelectedItems();
                break;

            case R.id.share_items:
                //
                break;

            case R.id.select_all_items:
                selectAll(false);
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
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(productDetails.getShownItem(selectedPosition).getUrl()));
                startActivity(intent);
                break;

            case R.id.view_page:
                String pageId = productDetails.getShownItem(selectedPosition).getPageId();
                if (!pageId.equals("-1")) {
                    openImage(pageId);
                }
                break;

            case R.id.remove_item:
                removeItem(selectedPosition, true);
                updateSwiped();
                break;

            case 0:
                //Remove from swiped
                productDetails.getShownItem(selectedPosition).resetSwiped();
                recyclerAdapter.refreshData();
                updateSwiped();
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
                swipedItem = productDetails.getShownItem(swipedPosition);

                if (supportsAdvancedOptions()) {
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
                    if (!productDetails.isShowSwiped()) {
                        snackbar.show();
                    } else {
                        recyclerView.removeAllViews();
                    }
                    swipedItem.setSelected(false);
                    recyclerAdapter.refreshData();
                    updateSelectionState();
                    updateSwiped();
                } else {
                    removeItem(swipedPosition, true);
                }
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

        if (productDetails.sizeDetailedItems() != 0) {
            updateStock();
        }
    }

    private void updateStock() {
        swipeRefresh.setRefreshing(true);
        queryApi(productDetails, true);
    }

    public void insertItem(int position, DetailedItem item) {
        item.setSelected(false);
        BasicItem basicItem = new ProductDetails.BasicItem(item.getSku(), item.getUpc(), item.getPageId());
        basicItem.setMultiPlano(item.isMultiPlano());
        productDetails.addBasicItem(basicItem);
        productDetails.addDetailedItem(position, item);
        recyclerAdapter.refreshData();
        updateSwiped();
    }

    public void insertItems(List<DetailedItem> items) {
        for (DetailedItem item : items) {
            insertItem(0, item);
        }
    }

    public void removeItem(int position, boolean showSnack) {
        removedPosition = position;
        removedItem = productDetails.getShownItem(position);

        productDetails.removeBasicItem(removedItem.getSku());
        productDetails.removeShownItem(position);
        recyclerAdapter.refreshData();
        updateSwiped();

        if (supportsAdvancedOptions() && productDetails.sizeDetailedItems() == 0) {
            getMainActivity().showStartFragment(true);
        } else {
            if (showSnack) {
                Snackbar.make(recyclerView, "Item removed", Snackbar.LENGTH_LONG).setAction("Undo", new UndoRemoveListener()).show();
            }
        }
    }

    public void removeSelectedItems() {
        removedItems.clear();
        for (DetailedItem item : productDetails.getDetailedItems(true, false, false)) {
            removedItems.add(item);
            removeItem(productDetails.getShownItemIndex(item), false);
        }
        updateSelectionState();
        updateSwiped();

        if (supportsAdvancedOptions() && productDetails.sizeDetailedItems() == 0) {
            getMainActivity().showStartFragment(true);
        } else {
            Snackbar.make(recyclerView, "Items removed", Snackbar.LENGTH_LONG).setAction("Undo", new UndoRemoveAllListener()).show();
        }
    }

    public void selectItem(int position) {
        if (supportsAdvancedOptions()) {
            productDetails.getShownItem(position).setSelected(!productDetails.getShownItem(position).isSelected());
            recyclerAdapter.refreshData();
            updateSelectionState();
        }
    }

    public void selectAll(boolean deselect) {
        for (int i = 0; i < productDetails.sizeShownItems(); i++) {
            if (deselect == productDetails.getShownItem(i).isSelected()) {
                selectItem(i);
            }
        }
    }

    public void setSwiped(boolean show) {
        productDetails.setShowSwiped(show);
        recyclerAdapter.refreshData();
    }

    public void updateSwiped() {
        if (supportsAdvancedOptions()) {
            if (productDetails != null) {
                if (productDetails.isShowSwiped() && productDetails.sizeSwipedItems() == 0) {
                    setSwiped(false);
                }
                getMainActivity().showSwipedItems.setVisible(productDetails.sizeSwipedItems() != 0);
                getMainActivity().showSwipedItems.setIcon(productDetails.isShowSwiped() ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            }
        }
    }

    public void updateSelectionState() {
        selectionState = productDetails.sizeSelectedItems() != 0;

        if (supportsAdvancedOptions()) {
            MainActivity mainActivity = getMainActivity();
            mainActivity.supportInvalidateOptionsMenu();

            if (selectionState) {
                mainActivity.toolbar.setTitle(productDetails.sizeSelectedItems() + " selected");
                mainActivity.toolbar.setTitleTextColor(getResources().getColor(R.color.colorBlack));
                mainActivity.toolbar.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                mainActivity.toolbar.getOverflowIcon().setColorFilter(getResources().getColor(R.color.colorBlack), PorterDuff.Mode.MULTIPLY);
                mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                mainActivity.toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.colorBlack), PorterDuff.Mode.MULTIPLY);
            } else {
                mainActivity.toolbar.setTitle(R.string.app_name);
                mainActivity.toolbar.setTitleTextColor(getResources().getColor(R.color.colorWhite));
                mainActivity.toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                mainActivity.toolbar.getOverflowIcon().setColorFilter(getResources().getColor(R.color.colorWhite), PorterDuff.Mode.MULTIPLY);
                mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        }
    }

    @Override
    public void onPause() {
        fileManager.saveState(productDetails);
        super.onPause();
    }

    @Override
    public void onStop() {
        fileManager.saveState(productDetails);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        fileManager.saveState(productDetails);
        fileManager.cleanUp(productDetails);
        super.onDestroy();
    }
}
