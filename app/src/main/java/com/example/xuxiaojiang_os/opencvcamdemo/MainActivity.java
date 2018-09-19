package com.example.xuxiaojiang_os.opencvcamdemo;

import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private final static String Tag = "mainActivity";

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private android.hardware.Camera mCamera;
    private SurfaceView mOpenCVView;
    private SurfaceHolder mOpenCVHolder;

    private OpenCVTask mOpenCvTask;

    private final int ACTION_NONE       = 0;
    private final int ACTION_CONTRAST   = 1;
    private final int ACTION_BRIGHTNESS = 2;
    private final int ACTION_SATURATION = 3;
    private int mAction = ACTION_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView)findViewById(R.id.surCamera);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

        mOpenCVView = (SurfaceView)findViewById(R.id.surOpencv);
        mOpenCVHolder = mOpenCVView.getHolder();
        mOpenCVHolder.addCallback(new MySurCallback());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.optionmenu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.contrast:
                mAction = ACTION_CONTRAST;
                break;
            case R.id.brightness:
                mAction = ACTION_BRIGHTNESS;
                break;
            case R.id.saturation:
                mAction = ACTION_SATURATION;
                break;
            case R.id.off:
            default:
                mAction = ACTION_NONE;
                break;
        }

        getPreviewData(); // get preview data and start opencv process

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.setDisplayOrientation(90);
        Camera.Parameters mParameters = mCamera.getParameters();
        mParameters.setPreviewSize(1920, 1080 );
        mCamera.setParameters(mParameters);

        mCamera.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if(mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    private void getPreviewData(){
        if(ACTION_NONE == mAction){
            if(null != mOpenCvTask){
                mOpenCvTask.cancel(false);
            }

            Log.d(Tag, "Cancel opencv process!");
            return;
        }

        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                if(null != mOpenCvTask){
                    switch(mOpenCvTask.getStatus()){
                        case RUNNING:
                            Log.d(Tag, "opencv process is running!");
                            return;
                        case PENDING:
                            Log.d(Tag, "opencv process is pending!");
                            mOpenCvTask.cancel(false);
                            break;
                    }
                }

                mOpenCvTask = new OpenCVTask(bytes);
                mOpenCvTask.execute((Void)null);
            }
        });
    }

    private class MySurCallback implements SurfaceHolder.Callback{

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }

    private class OpenCVTask extends AsyncTask{
        private byte[] mData;

        OpenCVTask(byte[] data){
            this.mData = data;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d(Tag, "doInBackground enter!");

            doOpenCvProcess(mAction);

            return null;
        }

        private void doOpenCvProcess(int action){
            switch(action){
                case ACTION_CONTRAST:
                    contrastProc();
                    break;
                case ACTION_BRIGHTNESS:
                    brightnessProc();
                    break;
                case ACTION_SATURATION:
                    saturationProc();
                    break;
                case ACTION_NONE:
                default:
                    break;
            }
        }

        private void contrastProc(){
            Log.d(Tag, "contrast process enter!");
        }

        private void brightnessProc(){
            Log.d(Tag, "brightness process enter!");
        }

        private void saturationProc(){
            Log.d(Tag, "saturation process enter!");
        }
    }
}
