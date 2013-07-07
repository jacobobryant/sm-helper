package com.jacobobryant.scripturemastery;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.ReportingInteractionMode;

import android.app.Application;

import android.content.SharedPreferences;

import android.preference.PreferenceManager;

@ReportsCrashes(
    formKey="",
    mailTo = "tooke@gmx.com",
    mode = ReportingInteractionMode.DIALOG,
    resToastText = R.string.crash_toast_text,
    resDialogText = R.string.crash_dialog_text,
    resDialogTitle = R.string.crash_dialog_title,
    resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
    resDialogOkToast = R.string.crash_dialog_ok_toast
    )
public class SMApp extends Application {
    public static final String TAG = "scripturemastery";

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean reporting =
            prefs.getBoolean(SettingsActivity.REPORTING, true);
        if (reporting && ! BuildConfig.DEBUG) {
            ACRA.init(this);
        }
    }
}
