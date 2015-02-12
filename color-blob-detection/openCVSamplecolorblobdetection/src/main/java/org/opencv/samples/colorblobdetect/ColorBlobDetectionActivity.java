package org.opencv.samples.colorblobdetect;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private Mat                  mRgba;
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    //mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {


        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Integer currentCount = processImage(mRgba);
        updateCountTextOnScreen(currentCount);

        return mRgba;
    }

    public void updateCountTextOnScreen(Integer currentCount){
        final String count = currentCount.toString();
        final TextView textView = (TextView) findViewById(R.id.countText);

        (this).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText("Current Count: " + count);
            }
        });
    }

    private int processImage(Mat imageBmp) {
        try {
            Mat threeChannel = new Mat();

            Imgproc.cvtColor(imageBmp, threeChannel, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(threeChannel, threeChannel, 220, 255, Imgproc.THRESH_BINARY);

            Mat dist = new Mat();
            Imgproc.distanceTransform(threeChannel, dist, Imgproc.CV_DIST_L2, 3);
            Imgproc.threshold(dist, dist, 0.5, 1.0, Imgproc.THRESH_BINARY);
            Mat dist8u = new Mat();
            dist.convertTo(dist8u, CvType.CV_8U);

            // Find total markers
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(dist8u, contours, hierarchy, Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE);

            final int currentCount = contours.size();
            final TextView textView = (TextView) findViewById(R.id.countText);

            (this).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText("Current Count: " + currentCount);
                }
            });

            return currentCount;
        } catch (Exception e) {
            Log.e(TAG, "Error processing pill image", e);
            return 0;
        }
    }
}
