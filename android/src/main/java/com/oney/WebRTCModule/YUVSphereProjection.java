package com.oney.WebRTCModule;

import android.content.Context;
import android.opengl.GLES20;

import com.asha.vrlib.MD360Director;
import com.asha.vrlib.MD360Program;
import com.asha.vrlib.model.MDMainPluginBuilder;
import com.asha.vrlib.model.MDPosition;
import com.asha.vrlib.objects.MDAbsObject3D;
import com.asha.vrlib.plugins.MDAbsPlugin;
import com.asha.vrlib.strategy.projection.ProjectionModeManager;
import com.asha.vrlib.strategy.projection.SphereProjection;

import static com.asha.vrlib.common.GLUtil.compileShader;
import static com.asha.vrlib.common.GLUtil.createAndLinkProgram;
import static com.asha.vrlib.common.GLUtil.glCheck;

/**
 * Created by jefry on 19/4/17.
 */

public class YUVSphereProjection extends SphereProjection {
    @Override
    public MDAbsPlugin buildMainPlugin(MDMainPluginBuilder builder) {
        return new YUVMDPanoramaPlugin(builder);
    }

    private class YUVMD360Program extends  MD360Program {
        private int mMVPMatrixHandle;
        private int mMVMatrixHandle;

        private int mPositionHandle;
        private int mTextureCoordinateHandle;
        private int mProgramHandle;
        private int mSTMatrixHandle;
        private int mTextureUniformHandle[];

        public YUVMD360Program(int type) {
            super(type);
        }
        @Override
        public void build(Context context) {
            final String vertexShader = getVertexShader(context);
            final String fragmentShader = getFragmentShader(context);

            final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
            final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

            mProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                    new String[] {"a_Position", "a_TexCoordinate"});

            // Set program handles for cube drawing.
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
            mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
            mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
            mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
            mSTMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_STMatrix");
            mTextureUniformHandle = new int[] {
                    GLES20.glGetUniformLocation(mProgramHandle, "u_Texture"),
                    GLES20.glGetUniformLocation(mProgramHandle, "y_Texture"),
                    GLES20.glGetUniformLocation(mProgramHandle, "v_Texture")
            };
        }
        @Override
        protected String getFragmentShader(Context context) {
            return "precision mediump float; // Set the default precision to medium. We don't need as high of a\n" +
                    "                        // precision in the fragment shader.\n" +
                    "uniform sampler2D y_Texture;\n" +
                    "uniform sampler2D u_Texture;\n" +
                    "uniform sampler2D v_Texture;\n" +
                    "varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.\n" +
                    "// The entry point for our fragment shader.\n" +
                    "void main()\n" +
                    "{\n" +
                    // CSC according to http://www.fourcc.org/fccyvrgb.php
                    "   float y = texture2D(u_Texture, v_TexCoordinate).r;\n" +
                    "   float u = texture2D(y_Texture, v_TexCoordinate).r - 0.5;\n" +
                    "   float v = texture2D(v_Texture, v_TexCoordinate).r - 0.5;\n" +
                    "   gl_FragColor = vec4(y + 1.403 * v," +
                    "                       y - 0.344 * u - 0.714 * v," +
                    "                       y + 1.77 * u, 1);\n" +
                    "}";
        }

        @Override
        public void use() {
            GLES20.glUseProgram(mProgramHandle);
        }

        @Override
        public int getMVPMatrixHandle() {
            return mMVPMatrixHandle;
        }

        @Override
        public int getMVMatrixHandle() {
            return mMVMatrixHandle;
        }

        @Override
        public int getTextureUniformHandle() {
            return mTextureUniformHandle[0];
        }

        public int getTextureUniformHandle(int idx) {
            return mTextureUniformHandle[idx];
        }

        @Override
        public int getPositionHandle() {
            return mPositionHandle;
        }

        public int getSTMatrixHandle() {
            return mSTMatrixHandle;
        }

        @Override
        public int getTextureCoordinateHandle() {
            return mTextureCoordinateHandle;
        }

    }
    private class YUVMDPanoramaPlugin extends  MDAbsPlugin {
        private YUVMD360Program mProgram;
        private ProjectionModeManager mProjectionModeManager;

        public YUVMDPanoramaPlugin(MDMainPluginBuilder builder) {
            mProgram = new YUVMD360Program(builder.getContentType());
            mProjectionModeManager = builder.getProjectionModeManager();
        }

        @Override
        public void init(Context context) {
            mProgram.build(context);
        }

        @Override
        public void beforeRenderer(int totalWidth, int totalHeight) {

        }

        @Override
        public void renderer(int index, int width, int height, MD360Director director) {

            MDAbsObject3D object3D = mProjectionModeManager.getObject3D();
            // check obj3d
            if (object3D == null) return;

            // Update Projection
            director.updateViewport(width, height);

            // Set our per-vertex lighting program.
            mProgram.use();
            glCheck("MDPanoramaPlugin mProgram use");

            GLES20.glUniform1i(mProgram.getTextureUniformHandle(0), 0);
            GLES20.glUniform1i(mProgram.getTextureUniformHandle(1), 1);
            GLES20.glUniform1i(mProgram.getTextureUniformHandle(2), 2);

            object3D.uploadVerticesBufferIfNeed(mProgram, index);

            object3D.uploadTexCoordinateBufferIfNeed(mProgram, index);

            // Pass in the combined matrix.
            director.shot(mProgram, getModelPosition());
            object3D.draw();

        }

        @Override
        public void destroy() {
        }

        @Override
        protected MDPosition getModelPosition() {
            return mProjectionModeManager.getModelPosition();
        }

        @Override
        protected boolean removable() {
            return false;
        }

    }

}
