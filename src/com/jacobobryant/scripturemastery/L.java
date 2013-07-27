package com.jacobobryant.scripturemastery;

import android.util.Log;

public class L {
    public static void log(String msg) {
        d(msg);
    }

    public static void d(String msg) {
        Log.d(SMApp.TAG, msg);
    }

    public static void w(String msg) {
        Log.w(SMApp.TAG, msg);
    }
}
