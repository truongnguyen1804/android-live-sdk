package com.sigma.live;


import com.pedro.encoder.input.video.CameraHelper;

public enum CameraFace {
    Back, Front, None;

    CameraHelper.Facing getValue() {
        return this == Back ? CameraHelper.Facing.BACK : CameraHelper.Facing.FRONT;
    }
}