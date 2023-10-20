package com.sigma;

import android.util.Log;

public class FullLog {
    private static boolean IS_DEBUG_D = true;
    private static boolean IS_DEBUG_W = true;
    private static boolean IS_DEBUG_E = true;
    private static String TAG = "SIGMA_LIVE ===>";

    public static void LogD(String s) {
        if (IS_DEBUG_D) {
            Log.d(TAG, s);
        }
    }

    public static void LogW(String s) {
        if (IS_DEBUG_W) {
            Log.w(TAG, s);
        }
    }

    public static void LogE(String s) {
        if (IS_DEBUG_E) {
            Log.e(TAG, s);
        }
    }
}
