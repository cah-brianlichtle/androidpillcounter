package com.cardinalhealth.pillcounter

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView

import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc.*

import java.util.ArrayList

class ColorBlobDetectionActivity : Activity(), View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private lateinit var mRgba: Mat
    private lateinit var mOpenCvCameraView: CameraBridgeViewBase
    private var minAreaThreshold = 100

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.color_blob_detection_surface_view)

        mOpenCvCameraView = findViewById(R.id.color_blob_detection_activity_surface_view)
        mOpenCvCameraView.setCvCameraViewListener(this)
    }

    public override fun onPause() {
        super.onPause()
        mOpenCvCameraView.disableView()
    }

    public override fun onResume() {
        super.onResume()

        if (OpenCVLoader.initDebug()) {
            mOpenCvCameraView.enableView()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        mOpenCvCameraView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        mRgba.release()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return false // don't need subsequent touch events
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat? {
        mRgba = inputFrame.rgba()

        val currentCount = processImage(mRgba)
        updateCountTextOnScreen(currentCount)

        return mRgba
    }

    private fun updateCountTextOnScreen(currentCount: Int) {
        val textView = findViewById<TextView>(R.id.countText)
        runOnUiThread { textView.text = getString(R.string.current_pill_count, currentCount.toString()) }
    }

    private fun processImage(imageBmp: Mat): Int {
        return try {
            val gray = getGrayScaleImage(imageBmp)
            val dist = getDistanceTransformationImage(gray)
            val thresh = getThresholdImage(dist)
            val dist8u = getDist8UImage(thresh)
            val contours = findContours(dist8u)

            drawBoundingBoxOnContours(contours, imageBmp)

            getContourCount(contours)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing pill image", e)
            0
        }
    }

    private fun getContourCount(contours: ArrayList<MatOfPoint>): Int {
        var count = 0

        contours.indices
                .asSequence()
                .filter { contourArea(contours[it]) > minAreaThreshold }
                .forEach { count = count.plus(1) }

        return count
    }

    private fun findContours(dist8u: Mat): ArrayList<MatOfPoint> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        findContours(dist8u, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)
        dist8u.release()
        return contours
    }

    private fun getDist8UImage(thresh: Mat): Mat {
        val dist8u = Mat()
        thresh.convertTo(dist8u, CvType.CV_8U)
        thresh.release()
        return dist8u
    }

    private fun getThresholdImage(dist: Mat): Mat {
        val thresh = Mat()
        threshold(dist, thresh, 0.5, 1.0, THRESH_BINARY)
        dist.release()
        preview(thresh, R.id.filter_image_view2)
        return thresh
    }

    private fun getDistanceTransformationImage(gray: Mat): Mat {
        val dist = Mat()
        distanceTransform(gray, dist, CV_DIST_L2, 3)
        gray.release()
        return dist
    }

    private fun getGrayScaleImage(imageBmp: Mat?): Mat {
        val gray = Mat()
        cvtColor(imageBmp!!, gray, COLOR_BGR2GRAY)
        preview(gray, R.id.filter_image_view1)
        return gray
    }

    private fun preview(mat: Mat, imageViewId: Int) {
        val matOfByte = MatOfByte()
        Imgcodecs.imencode(".jpg", mat, matOfByte)
        val data = matOfByte.toArray()
        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
        val mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)

        runOnUiThread {
            val imageView = findViewById<ImageView>(imageViewId)
            imageView.setImageBitmap(mutableBitmap)
        }
    }

    private fun drawBoundingBoxOnContours(contours: List<MatOfPoint>, imageBmp: Mat) {
        val approxCurve = MatOfPoint2f()

        for (i in contours.indices) {
            if (contourArea(contours[i]) > 100) {
                //Convert contours(i) from MatOfPoint to MatOfPoint2f
                val contour2f = MatOfPoint2f(*contours[i].toArray())
                //Processing on mMOP2f1 which is in type MatOfPoint2f
                val approxDistance = arcLength(contour2f, true) * 0.02
                approxPolyDP(contour2f, approxCurve, approxDistance, true)

                //Convert back to MatOfPoint
                val points = MatOfPoint(*approxCurve.toArray())

                // Get bounding rect of contour
                val rect = boundingRect(points)

                // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                rectangle(imageBmp,
                        Point(rect.x.toDouble(), rect.y.toDouble()),
                        Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
                        Scalar(255.0, 0.0, 0.0),
                        3)
            }
        }
    }

    companion object {
        private val TAG = "OCVSample::Activity"
    }
}
