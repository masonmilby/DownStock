package com.milburn.downstock;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    TextRecognizer textRecognizer;
    List<Uri> imageList = new ArrayList<>();
    ProgressBar progressBar;
    Toolbar toolbar;

    MenuItem showSwipedItems;
    boolean isShowSwipedChecked = false;

    RecyclerView recyclerView;
    RecyclerView.Adapter recyclerAdapter;

    int selectedPosition;
    boolean selectionState = false;
    List<ProductDetails> selectedItems = new ArrayList<>();

    List<ProductDetails> resultList = new ArrayList<>();
    List<ProductDetails> swipedItems = new ArrayList<>();
    ProductDetails lastItem;
    int lastPosition = 0;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recyclerView = findViewById(R.id.recycler);
        progressBar = findViewById(R.id.progressBar);
        showProgress(false, false, 0);

        MobileAds.initialize(this, this.getString(R.string.admob_key));
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        if (sharedPreferences.getString("store_id_pref", "0").contentEquals("0")) {
            startActivityForResult(new Intent(this, SettingsActivity.class), 0);
        } else {
            initialize();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            initialize();
        } else if (requestCode == 1 && resultCode == RESULT_OK) {
            List<Uri> uriList = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    uriList.add(data.getClipData().getItemAt(i).getUri());
                }
            } else {
                uriList.add(data.getData());
            }

            getItems(uriList);
        }
    }

    private void initialize() {
        textRecognizer = new TextRecognizer.Builder(getBaseContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.i("Info", "Detector dependencies are not yet available.");

            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "Storage too low", Toast.LENGTH_LONG).show();
                Log.i("Error", "Low storage");
            }
        } else {
            pickPhotos();
        }
    }

    private void pickPhotos() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        startActivityForResult(intent, 1);
    }

    private void getItems(List<Uri> uriList) {
        imageList = uriList;
        List<String[]> itemList = new ArrayList<>();
        int pageNum = -1;
        while (pageNum < uriList.size()-1) {
            pageNum++;
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriList.get(pageNum));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Frame frame = new Frame.Builder()
                    .setBitmap(bitmap)
                    .build();

            SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

            Pattern p = Pattern.compile("\\d{7}");

            String temp = "";
            for (int ii = 0; ii < textBlocks.size(); ii++) {
                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(ii));
                temp = temp + " " + textBlock.getValue();
            }

            Matcher m = p.matcher(temp);
            while (m.find()) {
                itemList.add(new String[]{m.group(), String.valueOf(pageNum)});
            }
        }

        BBYApi bbyApi = new BBYApi(this, new BBYApi.AsyncResponse() {
            @Override
            public void processFinish(List<ProductDetails> result) {
                setupRecycler(result);
            }
        });
        bbyApi.execute(itemList);
    }

    private void setupRecycler(final List<ProductDetails> result) {
        resultList = result;
        recyclerAdapter = new RecyclerAdapter(resultList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        recyclerView.setAdapter(recyclerAdapter);

        class UndoListener implements View.OnClickListener {

            @Override
            public void onClick(View view) {
                swipedItems.remove(lastItem);
                lastItem.deltabusted = false;
                lastItem.found = false;
                resultList.add(lastPosition, lastItem);
                recyclerAdapter.notifyItemInserted(lastPosition);
                recyclerAdapter.notifyItemRangeChanged(lastPosition, resultList.size());

                recyclerView.smoothScrollToPosition(lastPosition);
            }
        }

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                lastPosition = viewHolder.getAdapterPosition();
                lastItem = resultList.get(lastPosition);

                if (selectedItems.contains(lastItem)) {
                    selectedItems.remove(lastItem);
                }
                lastItem.selected = false;

                Snackbar snackbar = Snackbar.make(recyclerView, "0", Snackbar.LENGTH_LONG);
                snackbar.setAction("Undo", new UndoListener());

                switch (direction) {
                    case ItemTouchHelper.LEFT:
                        lastItem.deltabusted = true;
                        lastItem.found = false;
                        swipedItems.add(lastItem);
                        snackbar.setText("Product delta busted");
                        break;

                    case ItemTouchHelper.RIGHT:
                        lastItem.deltabusted = false;
                        lastItem.found = true;
                        swipedItems.add(lastItem);
                        snackbar.setText("Product found");
                        break;
                }
                snackbar.show();

                removeItem(lastPosition);
                updateShowSwipedItems();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);

        showSwipedItems = menu.findItem(R.id.show_swiped);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        showSwipedItems.setVisible(swipedItems.size() != 0);

        MenuItem viewPage = menu.findItem(R.id.view_page);

        if (imageList.size() == 0) {
            viewPage.setVisible(false);
        } else {
            viewPage.getSubMenu().clear();

            for (int i = 0; i < imageList.size(); i++) {
                viewPage.getSubMenu().add(Menu.NONE, i, i, "Page " + (i+1));
            }
            viewPage.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_page:
                break;

            case R.id.show_swiped:
                isShowSwipedChecked = !isShowSwipedChecked;
                item.setChecked(!item.isChecked());
                updateShowSwipedItems();
                break;

            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
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
        intent.setDataAndType(imageList.get(imageId), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    public void updateShowSwipedItems() {
        for (ProductDetails item : swipedItems) {
            if (!resultList.contains(item) & isShowSwipedChecked) {
                insertItem(0, item);
            } else if (resultList.contains(item) & !isShowSwipedChecked) {
                int pos = resultList.indexOf(item);
                removeItem(pos);
            }
        }

        if (isShowSwipedChecked & swipedItems.size() >= 1) {
            recyclerView.smoothScrollToPosition(0);
        }
    }

    @Override
    public boolean onContextItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_url:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(resultList.get(selectedPosition).url));
                startActivity(intent);
                break;

            case R.id.view_page:
                openImage(resultList.get(selectedPosition).pageNum);
                break;

            case R.id.remove_item:
                deleteItem(selectedPosition);
                break;

            default:
                swipedItems.remove(resultList.get(selectedPosition));
                resultList.get(selectedPosition).deltabusted = false;
                resultList.get(selectedPosition).found = false;
                recyclerAdapter.notifyItemChanged(selectedPosition);
                break;
        }
        return true;
    }

    public void insertItem(int position, ProductDetails item) {
        resultList.add(position, item);
        recyclerAdapter.notifyItemInserted(position);
        recyclerAdapter.notifyItemRangeChanged(position, resultList.size());
    }

    public void removeItem(int position) {
        resultList.remove(position);
        recyclerAdapter.notifyItemRemoved(position);
        recyclerAdapter.notifyItemRangeChanged(position, resultList.size());
    }

    public void deleteItem(int position) {
        if (swipedItems.contains(resultList.get(position))) {
            swipedItems.remove(resultList.get(position));
        }
        removeItem(position);
    }

    public void selectItem(int position) {
        resultList.get(position).selected = !resultList.get(position).selected;
        if (resultList.get(position).selected) {
            selectedItems.add(resultList.get(position));
        } else {
            resultList.get(position).selected = true;
            selectedItems.remove(resultList.get(position));
            resultList.get(position).selected = false;
        }
        recyclerAdapter.notifyItemChanged(position);

        selectionState = selectedItems.size() != 0;

        if (selectionState) {
            toolbar.setTitle(selectedItems.size() + " selected");
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        } else {
            toolbar.setTitle(R.string.app_name);
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    public void showProgress(final boolean showBar, final boolean increment, final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (showBar) {
                    if (increment) {
                        progressBar.incrementProgressBy(progress);
                    } else {
                        progressBar.setProgress(progress);
                    }
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}