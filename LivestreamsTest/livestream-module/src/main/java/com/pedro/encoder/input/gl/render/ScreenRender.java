package com.pedro.encoder.input.gl.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.RequiresApi;

import com.github.faucamp.simplertmp.Util;
import com.pedro.encoder.utils.gl.GlUtil;
import com.pedro.encoder.utils.gl.PreviewSizeCalculator;
import com.sigma.FullLog;
import com.sigma.live.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by pedro on 29/01/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ScreenRender {

    //rotation matrix
    private final float[] squareVertexData = {
            // X, Y, Z, U, V
            -1f, -1f, 0f, 0f, 0f, //bottom left
            1f, -1f, 0f, 1f, 0f, //bottom right
            -1f, 1f, 0f, 0f, 1f, //top left
            1f, 1f, 0f, 1f, 1f, //top right
    };

    private FloatBuffer squareVertex;

    private float[] MVPMatrix = new float[16];
    private float[] STMatrix = new float[16];
    private boolean AAEnabled = false;  //FXAA enable/disable

    private int texId;

    private int program = -1;
    private int uMVPMatrixHandle = -1;
    private int uSTMatrixHandle = -1;
    private int aPositionHandle = -1;
    private int aTextureHandle = -1;
    private int uSamplerHandle = -1;
    private int uResolutionHandle = -1;
    private int uAAEnabledHandle = -1;

    private int streamWidth;
    private int streamHeight;


//    private float[] mModelMatrix;


    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    public ScreenRender() {
        squareVertex =
                ByteBuffer.allocateDirect(squareVertexData.length * BaseRenderOffScreen.FLOAT_SIZE_BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();
        squareVertex.put(squareVertexData).position(0);

//        mModelMatrix = new float[16];
//        Matrix.setIdentityM(mModelMatrix, 0);
//        Matrix.rotateM(mModelMatrix, 0, 180, 0.0f, 1.0f, 0.0f);
//
//        // Đảo ngược ma trận Model Matrix (M) - xoay ngược
//
//        System.arraycopy(mModelMatrix, 0, MVPMatrix, 0, mModelMatrix.length);
//        Matrix.invertM(mModelMatrix, 0, MVPMatrix, 0);


//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        int height = displayMetrics.heightPixels;
//        int width = displayMetrics.widthPixels;

        // Khởi tạo ma trận Projection Matrix (P)
      /*  float aspectRatio = 1f;
        float fov = 15.0f; // Góc mở rộng của camera (Field of View)
        float near = 1.0f;
        float far = 100.0f;
        Matrix.perspectiveM(mProjectionMatrix, 0, fov, aspectRatio, near, far);

        // Khởi tạo ma trận View Matrix (V)
        float eyeX = 0.0f;
        float eyeY = 0.0f;
        float eyeZ = -5.0f;
        float centerX = 0.0f;
        float centerY = 0.0f;
        float centerZ = 0.0f;
        float upX = 0.0f;
        float upY = 1.0f;
        float upZ = 0.0f;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);

        Matrix.rotateM(mViewMatrix, 0, 90f, 0.0f, 0.0f, 1.0f);

        // Khởi tạo ma trận Model Matrix (M) đơn vị
        Matrix.setIdentityM(mModelMatrix, 0);


        Matrix.multiplyMM(MVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(MVPMatrix, 0, mProjectionMatrix, 0, MVPMatrix, 0);*/

        Matrix.setIdentityM(MVPMatrix, 0);
//
//        for (int i = 0; i < 4; i++) {
//            MVPMatrix[i * 4 + i] = 1.0f - MVPMatrix[i * 4 + i];
//        }
        Matrix.setIdentityM(STMatrix, 0);
    }

    public void initGl(Context context) {
        GlUtil.checkGlError("initGl start");
        String vertexShader = GlUtil.getStringFromRaw(context, R.raw.simple_vertex);
        String fragmentShader = GlUtil.getStringFromRaw(context, R.raw.fxaa);

        program = GlUtil.createProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        aTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix");
        uSamplerHandle = GLES20.glGetUniformLocation(program, "uSampler");
        uResolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
        uAAEnabledHandle = GLES20.glGetUniformLocation(program, "uAAEnabled");
        GlUtil.checkGlError("initGl end");
    }


    public void setImageThumbnail(Bitmap bitmap) {
        FullLog.LogD("bitmap3");
        this.b = bitmap;
    }

    private static final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec2 vTexCoord;" +
                    "varying vec2 texCoord;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "  texCoord = vTexCoord;" +
                    "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D sTexture;" +
                    "varying vec2 texCoord;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(sTexture, texCoord);" +
                    "}";

    public float[] bitmapToSquareVertexData(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float left = -1.0f;
        float right = 1.0f;
        float top = (float) height / (float) width;
        float bottom = -top;

        float[] squareVertexData = {
                left, top, 0.0f, // Top-left
                right, top, 0.0f, // Top-right
                left, bottom, 0.0f, // Bottom-left
                right, bottom, 0.0f // Bottom-right
        };

        return squareVertexData;
    }

    private Bitmap b = createBitmap();


    public Bitmap createBitmap() {
        FullLog.LogD("bitmap1");
        if (b == null) {
            FullLog.LogE("bitmap2");
            b = Bitmap.createBitmap(720, 1080, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            canvas.drawColor(Color.YELLOW);
        }
        return b;
    }

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private int positionHandle;
    private int texCoordHandle;
    private int samplerHandle;

    private int[] textureIds = new int[1];

    private static FloatBuffer createFloatBuffer(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    private float[] stm = new float[16];

    private float[] mvp = new float[16];

    public void draw(boolean enable, int width, int height, boolean keepAspectRatio) {
        GlUtil.checkGlError("drawScreen start");
        FullLog.LogD("drawScreen start");



        PreviewSizeCalculator.calculateViewPort(keepAspectRatio, width, height, streamWidth,
                streamHeight);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        if (!enable) {
            // truong edit
/*            float[] triangleCoords = {
                    0.0f, 1.0f, 0.0f, // Đỉnh trên
                    -1.0f, -1.0f, 0.0f, // Đỉnh trái dưới
                    1.0f, -1.0f, 0.0f // Đỉnh phải dưới
            };

            float[] bitmapFloat = bitmapToSquareVertexData(createBitmap());


            int vertexShader = GlUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = GlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            GLES20.glUseProgram(program);


            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


            FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(bitmapFloat.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            vertexBuffer.put(bitmapFloat);
            vertexBuffer.position(0);

            int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

            GLES20.glDisableVertexAttribArray(positionHandle);

            GlUtil.checkGlError("drawScreen end");*/
            // Tạo vertex buffer
            float[] vertices = {
                    -1.0f, 1.0f, 0.0f,  // Top-left
                    -1.0f, -1.0f, 0.0f, // Bottom-left
                    1.0f, -1.0f, 0.0f,  // Bottom-right
                    1.0f, 1.0f, 0.0f    // Top-right
            };
            vertexBuffer = createFloatBuffer(vertices);

            // Tạo texture coordinate buffer
            float[] texCoords = {
                    0.0f, 0.0f, // Top-left
                    0.0f, 1.0f, // Bottom-left
                    1.0f, 1.0f, // Bottom-right
                    1.0f, 0.0f  // Top-right
            };
            texCoordBuffer = createFloatBuffer(texCoords);

            // Tạo chương trình shader
            int vertexShader = GlUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = GlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            // Lấy vị trí thuộc tính trong vertex shader
            positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            texCoordHandle = GLES20.glGetAttribLocation(program, "vTexCoord");

            // Lấy vị trí uniform sampler trong fragment shader
            samplerHandle = GLES20.glGetUniformLocation(program, "sTexture");

            // Tạo texture object
            GLES20.glGenTextures(1, textureIds, 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Kích hoạt texture unit 0 và liên kết texture object
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

            // Sử dụng chương trình shader
            GLES20.glUseProgram(program);

            // Liên kết vertex buffer và thuộc tính vPosition
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

            // Liên kết texture coordinate buffer và thuộc tính vTexCoord
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer);

            // Thiết lập giá trị sampler
            GLES20.glUniform1i(samplerHandle, 0);

            // Vẽ hình vuông
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

            // Vô hiệu hóa thuộc tính và giải phóng tài nguyên
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
            GLES20.glDeleteTextures(1, textureIds, 0);
            GLES20.glDeleteProgram(program);
            GlUtil.checkGlError("drawScreen end");
            return;
        }
   /*     else {
            GLES20.glUseProgram(program);
//
//            float angle = 90.0f; // Góc xoay (đơn vị: độ)
//
//            Matrix.rotateM(MVPMatrix, 0, angle, 0.0f, 1.0f, 0.0f);
//
//            Matrix.multiplyMM(mvp, 0, mvp, 0, MVPMatrix, 0);
//
//            Matrix.rotateM(STMatrix, 0, angle, 0.0f, 1.0f, 0.0f);
//
//            Matrix.multiplyMM(stm, 0, stm, 0, STMatrix, 0);

//            int vertexShader = GlUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
//            int fragmentShader = GlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
////            int program = GLES20.glCreateProgram();
//            GLES20.glAttachShader(program, vertexShader);
//            GLES20.glAttachShader(program, fragmentShader);
//            GLES20.glLinkProgram(program);
//
//            GLES20.glUseProgram(program);
//
//            int projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix");
//            GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0);
//
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 4);
//            GlUtil.checkGlError("drawScreen end");
            squareVertex.position(BaseRenderOffScreen.SQUARE_VERTEX_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                    BaseRenderOffScreen.SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
            GLES20.glEnableVertexAttribArray(aPositionHandle);
//
            squareVertex.position(BaseRenderOffScreen.SQUARE_VERTEX_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
                    BaseRenderOffScreen.SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
            GLES20.glEnableVertexAttribArray(aTextureHandle);

            GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, MVPMatrix, 0);
            GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, STMatrix, 0);
//            GLES20.glUniform2f(uResolutionHandle, width, height);
            GLES20.glUniform2f(uResolutionHandle, height, width);
            GLES20.glUniform1f(uAAEnabledHandle, AAEnabled ? 1f : 0f);

            GLES20.glUniform1i(uSamplerHandle, 5);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
            //draw
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GlUtil.checkGlError("drawScreen end");
        }*/

        /*source*/
        GLES20.glUseProgram(program);

        squareVertex.position(BaseRenderOffScreen.SQUARE_VERTEX_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                BaseRenderOffScreen.SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
        GLES20.glEnableVertexAttribArray(aPositionHandle);

        squareVertex.position(BaseRenderOffScreen.SQUARE_VERTEX_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
                BaseRenderOffScreen.SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
        GLES20.glEnableVertexAttribArray(aTextureHandle);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, MVPMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, STMatrix, 0);
        GLES20.glUniform2f(uResolutionHandle, width, height);
        GLES20.glUniform1f(uAAEnabledHandle, AAEnabled ? 1f : 0f);

        GLES20.glUniform1i(uSamplerHandle, 5);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        //draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GlUtil.checkGlError("drawScreen end");
    }

    public void release() {
        GLES20.glDeleteProgram(program);
    }

    public void setTexId(int texId) {
        this.texId = texId;
    }

    public boolean isAAEnabled() {
        return AAEnabled;
    }

    public void setAAEnabled(boolean AAEnabled) {
        this.AAEnabled = AAEnabled;
    }

    public void setStreamSize(int streamWidth, int streamHeight) {
        this.streamWidth = streamWidth;
        this.streamHeight = streamHeight;
    }
}
