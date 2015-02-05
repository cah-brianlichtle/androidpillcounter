package cardinalhealth.pillcountertestlowerversion;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageHandling extends ActionBarActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    final String TAG = "Image Handling";
    Mat mImg;
    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private Mat                  mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.i(TAG, "OPENCV MANAGER CONNECTED");
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // Create and set View
                    setContentView(R.layout.activity_image_handling);
                    mImg = new Mat();
                }
                break;
                case LoaderCallbackInterface.INIT_FAILED: {
                    Log.i(TAG, "OpenCV init failed");
                }
                break;
                default: {
                    Log.i(TAG, "OpenCV default");
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_image_handling);

//        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mOpenCVCallBack))
//        {
//            Log.e(TAG, "Cannot connect to OpenCV Manager");
//        }

        ImageView thePhoto = (ImageView) findViewById(R.id.imageDisplay);
        Intent intent = getIntent();
        Bitmap bitmap = intent.getParcelableExtra("image");
        //thePhoto.setImageBitmap(bitmap);
        //int count = processImage(bitmap);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_image_handling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int processImage(Mat imageBmp) {
        try {
//            Bitmap imageBmp;
//
//            File imgFile = new File(imageThumbPath);
//            imageBmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//;
            //mImg = new Mat();
         //   Utils.bitmapToMat(imageBmp, mImg);
            Mat threeChannel = new Mat();
            Imgproc.cvtColor(mImg, threeChannel, Imgproc.COLOR_BGR2GRAY);

            Imgproc.threshold(threeChannel, threeChannel, 220, 255,
                    Imgproc.THRESH_BINARY);

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

            int ncomp = contours.size();

            Log.i("Count", "" + ncomp);

            return ncomp;
        } catch (Exception e) {
            //Log.e(TAG, "Error processing pill image", e);
            return 0;
        }
    }



    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mOpenCVCallBack);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
//            mDetector.process(mRgba);
//            List<MatOfPoint> contours = mDetector.getContours();
//            Log.e(TAG, "Contours count: " + contours.size());
//            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
//
//            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
//            colorLabel.setTo(mBlobColorRgba);
//
//            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
//            mSpectrum.copyTo(spectrumLabel);
            Integer count = processImage(mRgba);
            Log.i(TAG, count.toString());
        }

        return mRgba;
    }
}
