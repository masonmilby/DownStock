package com.milburn.downstock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {
    ProgressBar progressBar;
    Toolbar toolbar;
    ActionBar actionBar;

    MenuItem showSwipedItems;
    MenuItem viewPage;

    Button buttonSelect;
    Button buttonScan;

    View fragmentContainer;
    FragmentManager fragmentManager = null;
    FragmentTransaction fragmentTransaction = null;
    String shownFragmentTag;

    PhotoUriList uriList = new PhotoUriList();

    SharedPreferences sharedPreferences;
    OcrDetectorProcessor ocr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ocr = new OcrDetectorProcessor(this);
        setContentView(R.layout.activity_main);

        fragmentContainer = findViewById(R.id.fragment_container);
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
            showStartFragment(true, null);
        }
    }

    private void showStartFragment(boolean show, RecyclerFragment recyclerFragment) {
        if (fragmentManager == null) {
            fragmentManager = getSupportFragmentManager();

            fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentStarted(FragmentManager fm, Fragment f) {
                    super.onFragmentStarted(fm, f);
                    shownFragmentTag = f.getTag();

                    if (f.getTag().contentEquals("Selection")) {
                        buttonSelect = f.getView().findViewById(R.id.button_select);
                        buttonScan = f.getView().findViewById(R.id.button_scan);

                        if (buttonSelect != null && buttonScan != null) {
                            buttonSelect.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    setSelectionButtonsEnabled(false);
                                    pickPhotos();
                                }
                            });

                            buttonScan.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    setSelectionButtonsEnabled(false);
                                    takePhotos();
                                }
                            });
                        }
                    }
                }
            }, true);
        }
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, show ? new SelectionFragment() : recyclerFragment, show ? "Selection" : "Recycler").addToBackStack(show ? "Selection" : "Recycler").commitAllowingStateLoss();
    }

    private Fragment getShownFragment() {
        return fragmentManager.findFragmentByTag(shownFragmentTag);
    }

    private RecyclerFragment getRecyclerFragment() {
        return (RecyclerFragment)fragmentManager.findFragmentByTag("Recycler");
    }

    private boolean isSelectionState() {
        if (shownFragmentTag.contentEquals("Recycler")) {
            return ((RecyclerFragment)getShownFragment()).isSelectionState();
        }
        return false;
    }

    private void setSelectionButtonsEnabled(boolean enabled) {
        if (buttonSelect != null && buttonScan != null) {
            if (enabled) {
                buttonSelect.setEnabled(true);
                buttonSelect.setAlpha(1);
                buttonScan.setEnabled(true);
                buttonScan.setAlpha(1);
            } else {
                buttonSelect.setEnabled(false);
                buttonSelect.setAlpha((float)0.5);
                buttonScan.setEnabled(false);
                buttonScan.setAlpha((float)0.5);
            }
        }
    }

    private void pickPhotos() {
        //TODO: Fix api requirement
        //TODO: Move to selection fragment
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
                showStartFragment(true, null);
                break;

            case 1:
                if (data != null) {
                    if (data.getClipData() != null) {
                        for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                            uriList.addUri(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null){
                        uriList.addUri(data.getData());
                    }
                } else {
                    setSelectionButtonsEnabled(true);
                    Snackbar.make(fragmentContainer, "No image selected", Snackbar.LENGTH_LONG).show();
                    break;
                }

                if (ocr.isRecognizerReady()) {
                    ProductDetails productDetails = ocr.recognizeSkus(uriList);
                    if (productDetails.sizeBasicItems() <= 0) {
                        setSelectionButtonsEnabled(true);
                        Snackbar.make(fragmentContainer, "Cannot find SKUs in image", Snackbar.LENGTH_LONG).show();
                    } else {
                        RecyclerFragment recyclerFragment = new RecyclerFragment();
                        Bundle recyclerBundle = new Bundle();
                        recyclerBundle.putString("pd", productDetails.toJson());
                        recyclerBundle.putString("uri", uriList.toJson());
                        recyclerFragment.setArguments(recyclerBundle);
                        showStartFragment(false, recyclerFragment);
                    }
                }
                break;

            case 2:
                setSelectionButtonsEnabled(true);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        if (!isSelectionState()) {
            inflater.inflate(R.menu.toolbar_menu, menu);
            showSwipedItems = menu.findItem(R.id.show_swiped);
            viewPage = menu.findItem(R.id.view_page);
            //((RecyclerFragment)getShownFragment()).updateSwiped();
        } else {
            inflater.inflate(R.menu.toolbar_selected_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isSelectionState()) {
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
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (isSelectionState()) {
            getRecyclerFragment().selectAll(true);
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