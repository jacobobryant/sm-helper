package com.jacobobryant.scripturemastery;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import android.os.Bundle;

public class SettingsActivity extends SherlockPreferenceActivity {
    public static final String KEYWORDS = "pref_keywords";
    public static final String REPORTING = "pref_reporting";

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
