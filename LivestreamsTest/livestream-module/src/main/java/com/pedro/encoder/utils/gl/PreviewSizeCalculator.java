package com.pedro.encoder.utils.gl;

import android.opengl.GLES20;

/**
 * Created by pedro on 22/03/19.
 */

public class PreviewSizeCalculator {

    public static void calculateViewPort(boolean keepAspectRatio, int previewWidth, int previewHeight,
                                         int streamWidth, int streamHeight) {
        if (keepAspectRatio) {
            float ratio = Math.max(previewWidth / (float) streamWidth, previewHeight / (float) streamHeight);
            int realWidth = (int) (streamWidth * ratio);
            int realHeight = (int) (streamHeight * ratio);
            GLES20.glViewport((previewWidth - realWidth) / 2, (previewHeight - realHeight) / 2, realWidth, realHeight);
        } else {
            GLES20.glViewport(0, 0, previewWidth, previewHeight);
        }
    }
}
