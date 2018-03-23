package com.milburn.downstock;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    TextRecognizer textRecognizer;
    List<Uri> imageList = new ArrayList<>();
    CoordinatorLayout coordinatorLayout;
    MenuItem showDeltaItem;
    MenuItem showFoundItem;

    boolean isDeltaChecked = false;
    boolean isFoundChecked = false;

    RecyclerView recyclerView;
    RecyclerView.Adapter recyclerAdapter;
    int selectedPosition;

    List<ProductDetails> resultList = new ArrayList<>();
    HashMap<ProductDetails, Integer> swipedItems = new HashMap<>();
    ProductDetails lastItem;
    int lastPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        coordinatorLayout = findViewById(R.id.coordinator);

        MobileAds.initialize(this, this.getString(R.string.admob_key));
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        initialize();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
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
        recyclerView = findViewById(R.id.recycler);
        recyclerAdapter = new RecyclerAdapter(resultList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        recyclerView.setAdapter(recyclerAdapter);

        class UndoListener implements View.OnClickListener {

            @Override
            public void onClick(View view) {
                lastItem.deltabusted = false;
                lastItem.found = false;
                resultList.add(lastPosition, lastItem);
                swipedItems.remove(lastItem);
                recyclerAdapter.notifyItemInserted(lastPosition);
                recyclerAdapter.notifyItemRangeChanged(lastPosition, resultList.size());

                recyclerView.scrollToPosition(lastPosition);
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
                Snackbar snackbar = Snackbar.make(coordinatorLayout, "0", Snackbar.LENGTH_LONG);
                snackbar.setAction("Undo", new UndoListener());

                switch (direction) {
                    case ItemTouchHelper.LEFT:
                        lastItem.deltabusted = true;
                        lastItem.found = false;
                        swipedItems.put(lastItem, lastPosition);
                        snackbar.setText("Product delta busted");
                        break;

                    case ItemTouchHelper.RIGHT:
                        lastItem.deltabusted = false;
                        lastItem.found = true;
                        swipedItems.put(lastItem, lastPosition);
                        snackbar.setText("Product found");
                        break;
                }
                snackbar.show();

                resultList.remove(lastPosition);
                recyclerAdapter.notifyItemRemoved(lastPosition);
                recyclerAdapter.notifyItemRangeChanged(lastPosition, resultList.size());

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

        showDeltaItem = menu.findItem(R.id.show_delta);
        showFoundItem = menu.findItem(R.id.show_found);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem viewPage = menu.findItem(R.id.view_page);
        viewPage.getSubMenu().clear();

        for (int i = 0; i < imageList.size(); i++) {
            viewPage.getSubMenu().add(Menu.NONE, i, i, "Page " + (i+1));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_page:
                break;

            case R.id.show_swiped:
                break;

            case R.id.show_delta:
                isDeltaChecked = !isDeltaChecked;
                item.setChecked(!item.isChecked());
                updateShowSwipedItems();
                break;

            case R.id.show_found:
                isFoundChecked = !isFoundChecked;
                item.setChecked(!item.isChecked());
                updateShowSwipedItems();
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
        System.out.println("Delta : " + isDeltaChecked + " || Found : " + isFoundChecked);
        HashMap<ProductDetails, Integer> tempMap = new HashMap<>();
        tempMap.putAll(swipedItems);

        for (ProductDetails item : swipedItems.keySet()) {
            int pos = swipedItems.get(item);
            if (item.deltabusted && isDeltaChecked) {
                resultList.add(pos, item);
                recyclerAdapter.notifyItemInserted(pos);
                recyclerAdapter.notifyItemRangeChanged(pos, resultList.size());
            } else if (item.deltabusted && !isDeltaChecked && resultList.get(pos).sku.contains(item.sku)) {
                resultList.remove(pos);
                recyclerAdapter.notifyItemRemoved(pos);
                recyclerAdapter.notifyItemRangeChanged(pos, resultList.size());
            }

            if (item.found && isFoundChecked) {
                resultList.add(pos, item);
                recyclerAdapter.notifyItemInserted(pos);
                recyclerAdapter.notifyItemRangeChanged(pos, resultList.size());
            } else if (item.found && !isFoundChecked && resultList.get(pos).sku.contains(item.sku)) {
                System.out.println("Item Removed");
                resultList.remove(pos);
                recyclerAdapter.notifyItemRemoved(pos);
                recyclerAdapter.notifyItemRangeChanged(pos, resultList.size());
            }
        }
    }

    @Override
    public boolean onContextItemSelected (MenuItem item) {
        switch (item.getOrder()) {
            case 0:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(resultList.get(selectedPosition).url));
                startActivity(intent);
                break;

            case 1:
                openImage(resultList.get(selectedPosition).pageNum);
                break;

            case 2:
                swipedItems.remove(resultList.get(selectedPosition));
                if (resultList.get(selectedPosition).deltabusted) {
                    resultList.get(selectedPosition).deltabusted = false;
                } else if (resultList.get(selectedPosition).found) {
                    resultList.get(selectedPosition).found = false;
                }
                break;
        }
        return true;
    }
}