package com.oney.WebRTCModule;

import android.content.Context;
import android.graphics.RectF;
import android.opengl.GLES20;

import org.webrtc.RendererCommon;

import com.asha.vrlib.MD360Director;
import com.asha.vrlib.MD360DirectorFactory;
import com.asha.vrlib.MDVRLibrary;
import com.asha.vrlib.common.MDGLHandler;
import com.asha.vrlib.common.MDMainHandler;
import com.asha.vrlib.model.MDMainPluginBuilder;
import com.asha.vrlib.plugins.MDAbsPlugin;
import com.asha.vrlib.strategy.projection.AbsProjectionStrategy;
import com.asha.vrlib.strategy.projection.IMDProjectionFactory;
import com.asha.vrlib.strategy.projection.ProjectionModeManager;

import java.util.Iterator;
import java.util.List;


/**
 * Created by jefry on 11/4/17.
 */

public class Gl360Drawer implements RendererCommon.GlDrawer {

    private MDGLHandler mGLHandler;
    private ProjectionModeManager mProjectionModeManager;
    private Context mContext;

    public Gl360Drawer(SurfaceViewRenderer svr) {
        mContext = svr.getContext();
        MDMainHandler.init();
        mGLHandler = new MDGLHandler();

        ProjectionModeManager.Params projectionManagerParams = new ProjectionModeManager.Params();
        projectionManagerParams.textureSize = new RectF(0,0,2048,1024);;
        projectionManagerParams.directorFactory =  new MD360DirectorFactory.DefaultImpl();
        projectionManagerParams.projectionFactory = new IMDProjectionFactory() {
            @Override
            public AbsProjectionStrategy createStrategy(int mode) {
                assert (mode == MDVRLibrary.PROJECTION_MODE_SPHERE);
                return new YUVSphereProjection();
            }
        };
        projectionManagerParams.mainPluginBuilder = new MDMainPluginBuilder();
        mProjectionModeManager = new ProjectionModeManager(MDVRLibrary.PROJECTION_MODE_SPHERE, mGLHandler, projectionManagerParams);
        mProjectionModeManager.prepare(null, null);
    }

    private class UpdateDirRunnable implements Runnable{
        private double[] dir;

        public void setDir(double[] dir) {
            this.dir = dir;
        }

        @Override
        public void run() {
            List<MD360Director> directors = mProjectionModeManager.getDirectors();
            for (MD360Director director : directors){
                director.setDeltaX((float)dir[0]);
                director.setDeltaY((float)dir[1]);
                if(dir[2]>0 && dir[2]<10) {
                    director.updateProjectionNearScale((float) dir[2]);
                }
            }
        }
    }

    final UpdateDirRunnable mUpdateDir = new UpdateDirRunnable();

    public boolean setDir360(double[] dir) {
        mUpdateDir.setDir(dir);
        mGLHandler.post(mUpdateDir);
        return true;
    }

    @Override
    public void drawOes(int oesTextureId, float[] texMatrix, int frameWidth, int frameHeight, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {

    }

    @Override
    public void drawRgb(int textureId, float[] texMatrix, int frameWidth, int frameHeight, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {

    }

    @Override
    public void drawYuv(int[] yuvTextures, float[] texMatrix, int frameWidth, int frameHeight, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        // Bind the textures.
        for (int i = 0; i < 3; ++i) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[i]);
        }

        mGLHandler.dealMessage();
        List<MD360Director> directors = mProjectionModeManager.getDirectors();

        MDAbsPlugin mainPlugin = mProjectionModeManager.getMainPlugin();
        if (mainPlugin != null){
            mainPlugin.setup(mContext);
            mainPlugin.beforeRenderer(frameWidth, frameHeight);
        }
        MD360Director director = directors.get(0);
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight);

        if (mainPlugin != null){
            mainPlugin.renderer(0, viewportWidth, viewportHeight, director);
        }

        // Unbind the textures as a precaution..
        for (int i = 0; i < 3; ++i) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    @Override
    public void release() {
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                fireDestroy();
            }
        });
        mGLHandler.destroy();
    }

    private void fireDestroy() {
        MDAbsPlugin mainPlugin = mProjectionModeManager.getMainPlugin();
        if (mainPlugin != null){
            mainPlugin.destroy();
        }
    }
}
