package com.pedro.encoder.input.gl.render.filters;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.pedro.encoder.input.gl.render.BaseRenderOffScreen;
import com.pedro.encoder.input.gl.render.RenderHandler;
import com.pedro.encoder.utils.gl.GlUtil;

/**
 * Created by pedro on 29/01/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BaseFilterRender extends BaseRenderOffScreen {

    protected int previousTexId;
    private int width;
    private int height;
    private RenderHandler renderHandler = new RenderHandler();

    public void initGl(int width, int height, Context context) {
        this.width = width;
        this.height = height;
        GlUtil.checkGlError("initGl start");
        initGlFilter(context);
        GlUtil.checkGlError("initGl end");
    }

    public void initFBOLink() {
        initFBO(width, height, renderHandler.getFboId(), renderHandler.getRboId(),
                renderHandler.getTexId());
    }

    protected abstract void initGlFilter(Context context);

    public void draw() {
        GlUtil.checkGlError("drawFilter start");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderHandler.getFboId()[0]);
        GLES20.glViewport(0, 0, width, height);
        drawFilter();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.checkGlError("drawFilter end");
    }

    protected abstract void drawFilter();

    @Override
    public int getTexId() {
        return renderHandler.getTexId()[0];
    }

    protected int getWidth() {
        return width;
    }

    protected int getHeight() {
        return height;
    }

    public int getPreviousTexId() {
        return previousTexId;
    }

    public void setPreviousTexId(int texId) {
        this.previousTexId = texId;
    }

    public RenderHandler getRenderHandler() {
        return renderHandler;
    }

    public void setRenderHandler(RenderHandler renderHandler) {
        this.renderHandler = renderHandler;
    }
}
