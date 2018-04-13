package com.milburn.downstock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public Toolbar toolbar;

    public MenuItem showSwipedItems;
    private MenuItem viewPage;

    private FragmentManager fragmentManager = null;
    private FragmentTransaction fragmentTransaction = null;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MobileAds.initialize(this, this.getString(R.string.admob_key));
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        if (sharedPreferences.getString("store_id_pref", "0").contentEquals("0")) {
            startActivityForResult(new Intent(this, SettingsActivity.class), 0);
        } else if (canRetrieveState()) {
            showStartFragment(false);
        } else {
            showStartFragment(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (canRetrieveState()) {
                    showStartFragment(false);
                } else {
                    showStartFragment(true);
                }
                break;

                default:
                    break;
        }
    }

    public void showStartFragment(boolean show) {
        if (fragmentManager == null) {
            fragmentManager = getSupportFragmentManager();
        }
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, show ? new SelectionFragment() : new RecyclerFragment(), show ? "Selection" : "Recycler").addToBackStack(show ? "Selection" : "Recycler").commitAllowingStateLoss();
    }

    private RecyclerFragment getRecyclerFragment() {
        return (RecyclerFragment)fragmentManager.findFragmentByTag("Recycler");
    }

    private boolean isSelectionState() {
        if (getRecyclerFragment() != null) {
            return getRecyclerFragment().isSelectionState();
        }
        return false;
    }

    private boolean canRetrieveState() {
        String path = getExternalFilesDir(null).getAbsolutePath()+"/savedstate";
        File state = new File(path);
        return state.exists();
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

            case R.id.scan:
                startActivity(new Intent(this, CameraActivity.class));
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

    @Override
    public void onResume() {
        super.onResume();
        if (getRecyclerFragment() == null && canRetrieveState()) {
            showStartFragment(false);
        } else if (getRecyclerFragment() != null && !canRetrieveState()) {
            showStartFragment(true);
        }
    }
}