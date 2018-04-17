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
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public Toolbar toolbar;

    public MenuItem showSwipedItems;
    public Spinner listSelectSpinner;

    private FragmentManager fragmentManager = null;
    private FragmentTransaction fragmentTransaction = null;

    private SharedPreferences sharedPreferences;
    private FileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileManager = new FileManager(this);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MobileAds.initialize(this, this.getString(R.string.admob_key));
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        fragmentManager = getSupportFragmentManager();

        if (sharedPreferences.getString("store_id_pref", "0").contentEquals("0")) {
            startActivityForResult(new Intent(this, SettingsActivity.class), 0);
        } else if (fileManager.saveStateExists()) {
            showStartFragment(false);
        } else {
            showStartFragment(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (fileManager.saveStateExists()) {
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
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, show ? new SelectionFragment() : new RecyclerFragment(), show ? "Selection" : "Recycler").addToBackStack(show ? "Selection" : "Recycler").commit();
    }

    private RecyclerFragment getRecyclerFragment() {
        if (fragmentManager != null) {
            return (RecyclerFragment)fragmentManager.findFragmentByTag("Recycler");
        }
        return null;
    }

    private boolean isSelectionState() {
        if (getRecyclerFragment() != null) {
            return getRecyclerFragment().isSelectionState();
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        if (!isSelectionState()) {
            inflater.inflate(R.menu.toolbar_menu, menu);
            showSwipedItems = menu.findItem(R.id.show_swiped);

            MenuItem spinnerItem = menu.findItem(R.id.list_selector);
            listSelectSpinner = (Spinner)spinnerItem.getActionView();

            List<String> spinnerArray =  new ArrayList<>();
            spinnerArray.add("Main List");
            spinnerArray.add("Shared List");
            spinnerArray.add("Alex's Shared List");
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, spinnerArray);

            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            listSelectSpinner.setAdapter(spinnerAdapter);

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
        if (getRecyclerFragment() == null && fileManager.saveStateExists()) {
            showStartFragment(false);
        } else if (getRecyclerFragment() != null && !fileManager.saveStateExists()) {
            showStartFragment(true);
        }
    }
}