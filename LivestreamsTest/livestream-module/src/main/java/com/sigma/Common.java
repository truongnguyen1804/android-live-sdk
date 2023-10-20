package com.sigma;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class Common {
    private static Handler handler;

    private static Handler getHandler(Context context) {
        if (handler == null)
            handler = new Handler(Looper.getMainLooper());
        return handler;
    }

    public static void post(Context context, Runnable runnable) {
        getHandler(context).post(runnable);
    }

    public static enum TypePivot {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM
    }
}
