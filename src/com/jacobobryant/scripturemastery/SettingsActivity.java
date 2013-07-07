package com.jacobobryant.scripturemastery;

import android.os.Bundle;

import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
    public static final String KEYWORDS = "pref_keywords";
    public static final String REPORTING = "pref_reporting";

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
