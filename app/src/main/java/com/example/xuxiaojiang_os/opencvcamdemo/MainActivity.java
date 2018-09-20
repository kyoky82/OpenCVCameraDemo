package com.example.xuxiaojiang_os.opencvcamdemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

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
    protected void onResume() {
        super.onResume();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mOpenCVCallBack);
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

        //mCamera.setDisplayOrientation(90);
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

            long start = System.currentTimeMillis();

            // convert NV21 to BGR
            Camera.Size size = mCamera.getParameters().getPreviewSize();
            final int w = size.width;
            final int h = size.height;
            Mat mat = new Mat((int)(h*1.5), w, CvType.CV_8UC1);
            mat.put(0, 0, mData);
            Mat bgr_i420 = new Mat();
            Imgproc.cvtColor(mat, bgr_i420, Imgproc.COLOR_YUV2BGR_NV12);

            long cvtend = System.currentTimeMillis();
            Log.d(Tag, "convert yuv to bgr time: " + (cvtend - start));

            // Do opencv process
            doOpenCvProcess(bgr_i420, mAction);

            long procend = System.currentTimeMillis();
            Log.d(Tag, "opencv process time: " + (procend - cvtend));

            showOpenCvView(bgr_i420, w, h);
            long showView = System.currentTimeMillis();
            Log.d(Tag, "opencv showView time: " + (showView - procend));

            // convert bgr to nv21
            /*
            Mat result = new Mat();
            Imgproc.cvtColor(bgr_i420, result, Imgproc.COLOR_BGR2YUV_I420);
            byte[] data_result = new byte[(int)(w*h*1.5)];
            result.get(0, 0, data_result);

            long cvtagain = System.currentTimeMillis();
            Log.d(Tag, "convert bgr to yuv time: " + (cvtagain - procend));
            */

            getPreviewData();

            return null;
        }

        private void doOpenCvProcess(Mat mat, int action){
            switch(action){
                case ACTION_CONTRAST:
                    contrastProc(mat);
                    break;
                case ACTION_BRIGHTNESS:
                    brightnessProc(mat);
                    break;
                case ACTION_SATURATION:
                    saturationProc(mat);
                    break;
                case ACTION_NONE:
                default:
                    break;
            }
        }

        private void contrastProc(Mat mat){
            Log.d(Tag, "contrast process enter!");
        }

        private void brightnessProc(Mat mat){
            Log.d(Tag, "brightness process enter!");
        }

        private void saturationProc(Mat mat){
            Log.d(Tag, "saturation process enter!");
        }
    }

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS:
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void showOpenCvView(Mat mat, int width, int height){
        Bitmap bitMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        if(null == mat){
            return;
        }

        Utils.matToBitmap(mat,bitMap);
        Canvas canvas = mOpenCVHolder.lockCanvas();
        if(null != canvas){
            canvas.drawColor(0, PorterDuff.Mode.CLEAR );

            // 修改预览旋转90度问题
            /*
            canvas.rotate(90, 0, 0);
            float scale = canvas.getWidth() / (float)bitMap.getHeight();
            float scale2 = canvas.getHeight() / (float)bitMap.getWidth();
            if(scale2 > scale){
                scale = scale2;
            }
            if(0 != scale){
                canvas.scale(scale, scale, 0, 0 );
            }
            */
            // end

            canvas.drawBitmap(bitMap, new Rect(0, 0, bitMap.getWidth(), bitMap.getHeight()),
                    new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), null);
            mOpenCVHolder.unlockCanvasAndPost(canvas);
        }
    }

}
