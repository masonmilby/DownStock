package com.milburn.downstock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

import com.milburn.downstock.ProductDetails.DetailedItem;

public class MainActivity extends AppCompatActivity {
    List<Uri> uriList = new ArrayList<>();
    ProgressBar progressBar;
    Toolbar toolbar;
    ActionBar actionBar;

    MenuItem showSwipedItems;

    RecyclerView recyclerView;
    RecyclerAdapter recyclerAdapter;

    int selectedPosition = 0;
    int swipedPosition = 0;
    boolean selectionState = false;
    DetailedItem selectedItem;
    DetailedItem swipedItem;

    DetailedItem removedItem;
    List<DetailedItem> removedItems = new ArrayList<>();
    int removedPosition = 0;

    ProductDetails productDetails;

    SharedPreferences sharedPreferences;
    OcrDetectorProcessor ocr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ocr = new OcrDetectorProcessor(this);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        progressBar = findViewById(R.id.progressBar);
        showProgress(false, false, 0);

        MobileAds.initialize(this, this.getString(R.string.admob_key));
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        if (sharedPreferences.getString("store_id_pref", "0").contentEquals("0")) {
            startActivityForResult(new Intent(this, SettingsActivity.class), 0);
        } else {
            showStartFragment(true);
        }
    }

    private void showStartFragment(boolean show) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        SelectionFragment selectionFragment = new SelectionFragment();
        RecyclerFragment recyclerFragment = new RecyclerFragment();

        fragmentTransaction.replace(R.id.fragment_container, show ? selectionFragment : recyclerFragment, show ? "Selection" : "Recycler").addToBackStack(show ? "Selection" : "Recycler").commit();

        fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(FragmentManager fm, Fragment f) {
                super.onFragmentStarted(fm, f);

                if (f.getTag().contentEquals("Selection")) {
                    Button buttonSelect = f.getView().findViewById(R.id.button_select);
                    Button buttonScan = f.getView().findViewById(R.id.button_scan);

                    if (buttonSelect != null && buttonScan != null) {
                        buttonSelect.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                pickPhotos();
                            }
                        });

                        buttonScan.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                takePhotos();
                            }
                        });
                    }
                } else {
                    recyclerView = f.getView().findViewById(R.id.recycler);
                    setupRecycler();
                }
            }
        }, true);
    }

    private void pickPhotos() {
        //TODO: Fix api requirement
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        startActivityForResult(intent, 1);
    }

    private void takePhotos() {
        startActivityForResult(new Intent(this, CameraActivity.class), 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                showStartFragment(true);
                break;

            case 1:
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        uriList.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else {
                    uriList.add(data.getData());
                }

                if (ocr.isRecognizerReady()) {
                    productDetails = ocr.recognizeSkus(uriList);
                    if (productDetails.sizeBasicItems() <= 0) {
                        Snackbar.make(recyclerView, "Cannot find SKUs in image", Snackbar.LENGTH_LONG).show();
                    } else {
                        queryItems(productDetails);
                    }
                }
                break;

            case 2:
                break;
        }
    }

    private void queryItems(ProductDetails basic) {
        BBYApi bbyApi = new BBYApi(this, new BBYApi.AsyncResponse() {
            @Override
            public void processFinish(ProductDetails detailed) {
                productDetails.setDetailedItems(detailed.getDetailedItems());
                showStartFragment(false);
            }
        });
        bbyApi.execute(basic);
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

    private void setupRecycler() {
        recyclerAdapter = new RecyclerAdapter(productDetails, this);

        recyclerView.setLayoutManager(new LinearLayout(this));
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

                Snackbar snackbar = Snackbar.make(recyclerView, "0", Snackbar.LENGTH_LONG);
                snackbar.setAction("Undo", new UndoSwipeListener());

                switch (direction) {
                    case ItemTouchHelper.LEFT:
                        swipedItem.setDeltabusted(true);
                        snackbar.setText("Product delta busted");
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
                recyclerAdapter.refreshData();
                updateSelectionState();
                updateSwiped();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        if (!selectionState) {
            inflater.inflate(R.menu.toolbar_menu, menu);
            showSwipedItems = menu.findItem(R.id.show_swiped);
            updateSwiped();
        } else {
            inflater.inflate(R.menu.toolbar_selected_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!selectionState) {
            MenuItem viewPage = menu.findItem(R.id.view_page);
            if (uriList.size() == 0) {
                viewPage.setVisible(false);
            } else {
                viewPage.getSubMenu().clear();

                for (int i = 0; i < uriList.size(); i++) {
                    viewPage.getSubMenu().add(Menu.NONE, i, i, "Page " + (i+1));
                }
                viewPage.setVisible(true);
            }
        }
        return true;
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

            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case android.R.id.home:
                selectAll(true);
                break;

            case R.id.delete_items:
                removeSelectedItems();
                break;

            case R.id.share_items:
                break;

            case R.id.select_all_items:
                selectAll(false);
                break;

            default:
                openImage(item.getItemId());
                break;
        }

        return true;
    }

    public void openImage(int imageId) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uriList.get(imageId), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public boolean onContextItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_url:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(productDetails.getShownItem(selectedPosition).getUrl()));
                startActivity(intent);
                break;

            case R.id.view_page:
                openImage(productDetails.getShownItem(selectedPosition).getPageNum());
                break;

            case R.id.remove_item:
                removeItem(selectedPosition, true);
                updateSwiped();
                break;

            default:
                productDetails.getShownItem(selectedPosition).resetSwiped();
                recyclerAdapter.refreshData();
                updateSwiped();
                break;
        }
        return true;
    }

    public void insertItem(int position, DetailedItem item) {
        item.setSelected(false);
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

        productDetails.removeShownItem(position);
        recyclerAdapter.refreshData();

        if (showSnack) {
            Snackbar.make(recyclerView, "Item removed", Snackbar.LENGTH_LONG).setAction("Undo", new UndoRemoveListener()).show();
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

        Snackbar.make(recyclerView, "Items removed", Snackbar.LENGTH_LONG).setAction("Undo", new UndoRemoveAllListener()).show();
    }

    public void selectItem(int position) {
        productDetails.getShownItem(position).setSelected(!productDetails.getShownItem(position).isSelected());
        recyclerAdapter.refreshData();
        updateSelectionState();
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
        if (productDetails != null) {
            if (productDetails.isShowSwiped() && productDetails.sizeSwipedItems() == 0) {
                setSwiped(false);
            }
            showSwipedItems.setVisible(productDetails.sizeSwipedItems() != 0);
            showSwipedItems.setIcon(productDetails.isShowSwiped() ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
        }
    }

    public void updateSelectionState() {
        selectionState = productDetails.sizeSelectedItems() != 0;

        supportInvalidateOptionsMenu();

        if (selectionState) {
            toolbar.setTitle(productDetails.sizeSelectedItems() + " selected");
            toolbar.setTitleTextColor(getResources().getColor(R.color.colorToolbarSelectedInverse));
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarSelected));
            toolbar.getOverflowIcon().setColorFilter(getResources().getColor(R.color.colorToolbarSelectedInverse), PorterDuff.Mode.MULTIPLY);
            actionBar.setDisplayHomeAsUpEnabled(true);
            toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.colorToolbarSelectedInverse), PorterDuff.Mode.MULTIPLY);
        } else {
            toolbar.setTitle(R.string.app_name);
            toolbar.setTitleTextColor(getResources().getColor(R.color.colorToolbarSelected));
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            toolbar.getOverflowIcon().setColorFilter(getResources().getColor(R.color.colorToolbarSelected), PorterDuff.Mode.MULTIPLY);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (selectionState) {
            selectAll(true);
        } else {
            super.onBackPressed();
            finish();
        }
    }

    public void showProgress(final boolean showBar, final boolean increment, final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (increment) {
                    progressBar.incrementProgressBy(progress);
                } else {
                    progressBar.setProgress(progress);
                }
                progressBar.setVisibility(showBar ? View.VISIBLE : View.GONE);
            }
        });
    }
}