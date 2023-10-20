package com.pedro.encoder.input.video;

import android.content.Context;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import com.sigma.FullLog;

/**
 * Created by pedro on 17/12/18.
 */
public class CameraHelper {

    private static final float[] verticesData = {
            // X, Y, Z, U, V
            -1f, -1f, 0f, 0f, 0f,
            1f, -1f, 0f, 1f, 0f,
            -1f, 1f, 0f, 0f, 1f,
            1f, 1f, 0f, 1f, 1f,
    };

    private static int orientation = 0;

    public static float[] getVerticesData() {
        return verticesData;
    }

    public static int getCameraOrientation(Context context) {
//        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        if (windowManager != null) {
//            int orientation = windowManager.getDefaultDisplay().getRotation();
//            switch (orientation) {
//                case Surface.ROTATION_0: //portrait
//                    return 90;
//                case Surface.ROTATION_90: //landscape
//                    return 0;
//                case Surface.ROTATION_180: //reverse portrait
//                    return 270;
//                case Surface.ROTATION_270: //reverse landscape
//                    return 180;
//                default:
//                    return 0;
//            }
//        } else {
//            return 0;
//        }

        return orientation;
    }

    public static int setCameraOrientation(Context context, int rotation) {
        int r = rotation % 360;
        if (r < 0) {
            r = 360 + r;
        }
        if (r >= 315 || r <= 45) {
//            orientation = 0;
            orientation = 90;
            return 0;
        } else if (r <= 135) {
//            orientation = 90;
            orientation = 0;
            return 0;
        } else if (r <= 225) {
//            orientation = 180;
            orientation = 270;
            return 270;
        } else {
//            orientation = 270;
            orientation = 180;
            return 180;
        }
    }

    public static int getRotationGl(int r){
        if (r ==90) {
            return 0;
        } else if (r ==0) {
            return -90;
        } else if (r ==270) {
            return 0;
        } else {
            return 90;
        }
    }

    public static int chooseCameraOrientation(Context context, int orientation) {
        return orientation;
    }

    public static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    public enum Facing {
        BACK, FRONT
    }
}
