package com.jacobobryant.scripturemastery;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.ReportingInteractionMode;

import android.app.Application;

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
public class SMHelperApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) ACRA.init(this);
    }
}
