package com.milburn.downstock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
    private ProgressBar progressBar;
    public Toolbar toolbar;
    public ActionBar actionBar;

    public MenuItem showSwipedItems;
    private MenuItem viewPage;

    private Button buttonScan;

    private View fragmentContainer;
    private FragmentManager fragmentManager = null;
    private FragmentTransaction fragmentTransaction = null;
    private String shownFragmentTag;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

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

    public void showStartFragment(boolean show, RecyclerFragment recyclerFragment) {
        if (fragmentManager == null) {
            fragmentManager = getSupportFragmentManager();

            fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentStarted(FragmentManager fm, Fragment f) {
                    super.onFragmentStarted(fm, f);
                    shownFragmentTag = f.getTag();

                    if (f.getTag().contentEquals("Selection")) {
                        buttonScan = f.getView().findViewById(R.id.button_scan);

                        if (buttonScan != null) {
                            buttonScan.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //setSelectionButtonsEnabled(false);
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
        if (buttonScan != null) {
            if (enabled) {
                buttonScan.setEnabled(true);
                buttonScan.setAlpha(1);
            } else {
                buttonScan.setEnabled(false);
                buttonScan.setAlpha((float)0.5);
            }
        }
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

                default:
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
            if (getRecyclerFragment() == null) {
                showSwipedItems.setVisible(false);
            } else {
                getRecyclerFragment().updateSwiped();
            }
        } else {
            inflater.inflate(R.menu.toolbar_selected_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isSelectionState() && getRecyclerFragment() != null) {
            int uriSize = getRecyclerFragment().getProductDetails().getUriList().size();
            if (uriSize == 0) {
                viewPage.setVisible(false);
            } else {
                viewPage.getSubMenu().clear();

                for (int i = 0; i < uriSize; i++) {
                    viewPage.getSubMenu().add(Menu.NONE, i, i, "Page " + (i+1));
                }
                viewPage.setVisible(true);
            }
        } else if (getRecyclerFragment() == null) {
            viewPage.setVisible(false);
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