package com.milburn.downstock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        if (sharedPreferences.getString("store_id_pref", "0").contentEquals("0")) {
            Snackbar.make(findViewById(android.R.id.content), "Please set your store number", Snackbar.LENGTH_INDEFINITE).show();
        }
    }
}
