package com.jacobobryant.scripturemastery;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import android.os.Bundle;

public class SettingsActivity extends SherlockPreferenceActivity {
    public static final String KEYWORDS = "pref_keywords";

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
