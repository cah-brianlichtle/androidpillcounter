package com.cardinalhealth.pillcounter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ColorBlobDetectionActivity extends Activity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private Mat mRgba;
    private CameraBridgeViewBase mOpenCvCameraView;

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            mOpenCvCameraView.enableView();
        }
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

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Integer currentCount = processImage(mRgba);
        updateCountTextOnScreen(currentCount);

        return mRgba;
    }

    public void updateCountTextOnScreen(Integer currentCount) {
        final String count = currentCount.toString();
        final TextView textView = findViewById(R.id.countText);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText("Current Count: " + count);
            }
        });
    }

    private int processImage(Mat imageBmp) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(imageBmp, gray, Imgproc.COLOR_BGR2GRAY);
            preview(gray, R.id.filter_image_view1);

            Mat thresh = new Mat();
            Imgproc.threshold(gray, thresh, 220, 255, Imgproc.THRESH_BINARY);
            preview(thresh, R.id.filter_image_view2);

            Mat dist = new Mat();
            Imgproc.distanceTransform(gray, dist, Imgproc.CV_DIST_L2, 3);
            Imgproc.threshold(dist, dist, 0.5, 1.0, Imgproc.THRESH_BINARY);

            Mat dist8u = new Mat();
            dist.convertTo(dist8u, CvType.CV_8U);

            // Find total markers
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(dist8u, contours, hierarchy, Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

            drawBoundingBoxOnContours(contours, imageBmp);

            return contours.size();

        } catch (Exception e) {
            Log.e(TAG, "Error processing pill image", e);
            return 0;
        }
    }

    private void preview(Mat mat, final int imageViewId) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, matOfByte);
        byte[] data = matOfByte.toArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        final Bitmap mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = findViewById(imageViewId);
                imageView.setImageBitmap(mutableBitmap);
            }
        });
    }

    private void drawBoundingBoxOnContours(List<MatOfPoint> contours, Mat imageBmp) {
        MatOfPoint2f         approxCurve = new MatOfPoint2f();

        for (int i=0; i<contours.size(); i++)
        {
            //Convert contours(i) from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);

            // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
            Imgproc.rectangle(imageBmp,
                new Point(rect.x,rect.y),
                new Point(rect.x+rect.width,rect.y+rect.height),
                new Scalar(255, 0, 0),
                3);


        }
    }
}
