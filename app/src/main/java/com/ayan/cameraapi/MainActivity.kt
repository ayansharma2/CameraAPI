package com.ayan.cameraapi

import android.Manifest
import android.animation.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import java.io.File

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.*
import android.os.*
import android.util.DisplayMetrics
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.*
import java.util.*
import kotlin.concurrent.thread
import android.graphics.drawable.Drawable

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.animation.addListener
import androidx.core.graphics.get
import com.google.mlkit.vision.pose.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    val textureView: AutoFitTextureView by lazy {
        findViewById<AutoFitTextureView>(R.id.texture_view)
    }

    companion object {
        const val CAMERA_REQUEST = 2
    }

    private lateinit var previewSurface: Surface
    lateinit var cameraId: String
    lateinit var cameraDevice: CameraDevice
    lateinit var captureCameraSession: CameraCaptureSession
    lateinit var captureRequest: CaptureRequest
    lateinit var captureRequestBuilder: CaptureRequest.Builder
    lateinit var leftEye: TextView
    lateinit var rightEye: TextView
    lateinit var parent_layout: ConstraintLayout
    private lateinit var imageDimensions: Size

    //    lateinit var imageReader: ImageReader
    lateinit var file: File
    var mBackgroundHandler: Handler? = null
    var mBackgroundThread: HandlerThread? = null
    lateinit var options: AccuratePoseDetectorOptions
    private lateinit var button: Button
    private val ORIENTATIONS = SparseIntArray()
    lateinit var eye: TextView
    lateinit var fab: FloatingActionButton
    lateinit var canvas: Canvas
    lateinit var tempImageView: ImageView
    val executorService: ExecutorService = Executors.newFixedThreadPool(4)


    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
    }

    lateinit var box: View
    lateinit var backEnd: Box
    lateinit var timerLayout: LinearLayout
    lateinit var timer: TextView
    lateinit var detecting: TextView

    private lateinit var poseDetector: PoseDetector
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tempImageView=findViewById(R.id.image_view)
        options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        fab = findViewById(R.id.goToFallingBoxes)
        parent_layout = findViewById(R.id.parent_layout)
        box = findViewById(R.id.box)
        timerLayout = findViewById(R.id.timer_layout)
        timer = findViewById(R.id.timer)
        poseDetector = PoseDetection.getClient(options)
        detecting = findViewById(R.id.detecting)
        button = findViewById(R.id.click)
        fab.setOnClickListener {
            startActivity(Intent(this, FallingBoxes::class.java))
        }
        button.setOnClickListener {
            takePicture()
        }
        //updateUI()
        canvas = Canvas()


    }

    var Viewheight = 0
    var Viewwidth = 0
    private fun takePicture() {
        TODO("Not yet implemented")
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false;
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

        }

    };
    lateinit var manager: CameraManager

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = manager.cameraIdList[1]
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        imageDimensions = map!!.getOutputSizes(SurfaceTexture::class.java)[0]
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST
            )
            return
        }
        manager.openCamera(cameraId, stateCallback, mBackgroundHandler)
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
//            captureRequestBuilder.removeTarget(Surface(textureView.surfaceTexture))
//            captureCameraSession.stopRepeating()
            //textureView.surfaceTextureListener= textureListener

        }

        override fun onError(camera: CameraDevice, error: Int) {
            TODO("Not yet implemented")
        }

    }
    var margin = 0

    @SuppressLint("NewApi")
    private fun createCameraPreview() {
        val texture = textureView.surfaceTexture
        val displayMetrics = DisplayMetrics()

        texture!!.setDefaultBufferSize(imageDimensions.width, imageDimensions.height)
        runOnUiThread {
            textureView.setAspectRatio(9, 16)
            val constraintSet = ConstraintSet()
            constraintSet.clone(parent_layout)
            margin = Resources.getSystem().displayMetrics.heightPixels - textureView.height
            constraintSet.connect(
                box.id, ConstraintSet.TOP,
                parent_layout.id, ConstraintSet.TOP, margin / 2
            )
            constraintSet.connect(
                box.id, ConstraintSet.BOTTOM,
                parent_layout.id, ConstraintSet.BOTTOM, margin / 2
            )
            constraintSet.applyTo(parent_layout)
        }
        Log.e("ImageDimensions", "${Resources.getSystem().displayMetrics.heightPixels}")

        backEnd = Box(textureView.width, textureView.height)
        val surface = Surface(texture)
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        //captureRequestBuilder.addTarget(imageReader.surface)
        cameraDevice.createCaptureSession(
            mutableListOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureCameraSession = session
                    updatePreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {

                }

            },
            null
        )
    }

    private fun updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        captureCameraSession.setRepeatingRequest(
            captureRequestBuilder.build(),
            null,
            mBackgroundHandler
        )
        detectPerson()
        //startImages()
    }

    private fun detectPerson() {
        //Log.e("ScreenWidth","${textureView.width}  ${textureView.height}")
        thread {
            var detected = false
            while (true) {
                Thread.sleep(100)
                val image = InputImage.fromBitmap(textureView.bitmap!!, 0)
                //Log.e("ImageWidth",image.width.toString())
                poseDetector.process(image)
                    .addOnCompleteListener { pose ->
                        Log.e(
                            "POSE",
                            Gson().toJson(pose.result!!.getPoseLandmark(PoseLandmark.LEFT_KNEE))
                        )
                        if (pose.result!!.allPoseLandmarks.size > 0 &&
                            pose.result!!.getPoseLandmark(PoseLandmark.NOSE).position.x > 0 &&
                            pose.result!!.getPoseLandmark(PoseLandmark.NOSE).position.x < image.width
                            && pose.result!!.getPoseLandmark(PoseLandmark.NOSE).position.y > 0
                            && pose.result!!.getPoseLandmark(PoseLandmark.NOSE).position.y < image.height &&
                            pose.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.x > 0 &&
                            pose.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.x < image.width &&
                            pose.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.y > 0 &&
                            pose.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.y < image.height &&
                            pose.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.x > 0 &&
                            pose.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.x < image.width &&
                            pose.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.y > 0 &&
                            pose.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.y < image.height
                        ) {
                            detected = true
                        }
                    }
                if (detected)
                    break
            }
            runOnUiThread {
                detecting.apply {
                    animate()
                        .alpha(0f)
                        .setDuration(500)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                detecting.visibility = View.GONE
                                timerLayout.visibility = View.VISIBLE
                                var countDown = object : CountDownTimer(5000, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        timer.text = "${millisUntilFinished / 1000}"
                                    }

                                    @SuppressLint("ObjectAnimatorBinding")
                                    override fun onFinish() {

                                        ObjectAnimator.ofFloat(timerLayout, "translationY", -300f)
                                            .apply {
                                                duration = 1000
                                                start()
                                            }.doOnEnd {
                                                timerLayout.visibility = View.GONE
                                                startImages()
                                            }

                                    }
                                }
                                countDown.start()
                            }
                        })
                }
            }
        }
    }

    private fun updateUI() {
        if (imageView != null) {
            parent_layout.removeView(imageView)
            imageView = null
        }
        Log.e("CurrentUI",i.toString())
        imageView = CustomRectangle(
            this@MainActivity,
            textureView.height,
            textureView.width,
            i
        )
        var set = ConstraintSet()
        imageView!!.id = View.generateViewId()
        parent_layout.addView(imageView)

        set.clone(parent_layout)
        set.connect(
            imageView!!.id,
            ConstraintSet.START,
            textureView.id,
            ConstraintSet.START
        )
        set.connect(
            imageView!!.id,
            ConstraintSet.TOP,
            textureView.id,
            ConstraintSet.TOP
        )
        set.connect(
            imageView!!.id,
            ConstraintSet.BOTTOM,
            textureView.id,
            ConstraintSet.BOTTOM
        )
        set.connect(
            imageView!!.id,
            ConstraintSet.END,
            textureView.id,
            ConstraintSet.END
        )
        set.applyTo(parent_layout)
    }

    fun increment(){
        Log.e("RequestREcevie",i.toString())
        synchronized(this){
            i++
        }
    }

    var i = 0
    var imageView: CustomRectangle? = null

    @SuppressLint("InlinedApi")
    private fun startImages() {
        var isDetecting=false
        updateUI()
        thread {
            while (i<5) {
                if(!isDetecting){
                    isDetecting=true
                    var completeBitmap = textureView.bitmap
                    var cropped = when (i) {
                        0 -> {
                            Bitmap.createBitmap(
                                completeBitmap!!,
                                (textureView.width / 2),
                                0,
                                (textureView.width / 2),
                                textureView.height
                            )
                        }
                        1 -> {
                            Bitmap.createBitmap(
                                completeBitmap!!,
                                (textureView.width / 2),
                                0,
                                (textureView.width / 2),
                                textureView.height / 2
                            )
                        }
                        2 -> {
                            Bitmap.createBitmap(
                                completeBitmap!!,
                                (completeBitmap.width / 2),
                                completeBitmap.height / 2,
                                (completeBitmap.width / 2),
                                completeBitmap.height / 2
                            )
                        }
                        3 -> {
                            Bitmap.createBitmap(
                                completeBitmap!!,
                                0,
                                0,
                                (textureView.width / 2),
                                textureView.height / 2
                            )
                        }
                        4 -> {
                            Bitmap.createBitmap(
                                completeBitmap!!,
                                0,
                                0,
                                (textureView.width),
                                textureView.height / 2
                            )
                        }

                        else -> {
                            Bitmap.createBitmap(completeBitmap!!)
                        }
                    }

                    poseDetector.process(InputImage.fromBitmap(cropped, 0))
                        .continueWith(executorService, { poses ->
                            if (poses.result!!.allPoseLandmarks.size > 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.x >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.x <= cropped.width &&
                                poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.y >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.y <= cropped.height &&
                                poses.result!!.getPoseLandmark(PoseLandmark.LEFT_SHOULDER).position.x >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.LEFT_SHOULDER).position.x <= cropped.width &&
                                poses.result!!.getPoseLandmark(PoseLandmark.LEFT_SHOULDER).position.y >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.LEFT_SHOULDER).position.y <= cropped.height &&
                                poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER).position.x >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER).position.x <= cropped.width &&
                                poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER).position.y >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER).position.y <= cropped.height &&
                                poses.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.x >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.x <= cropped.width &&
                                poses.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.y >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.y <= cropped.height &&
                                poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.x >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.x <= cropped.width &&
                                poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.y >= 0 &&
                                poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.y <= cropped.height
                            ) {
                                //detecting.setText(poses.result.getPoseLandmark(PoseLandmark.NOSE).position.toString())
                                runOnUiThread {
                                    tempImageView.setImageBitmap(cropped)
                                }
                                runOnUiThread {

                                    var animator = ValueAnimator.ofInt(100, 255)
                                    animator.duration = 2000
                                    animator.addUpdateListener {
                                        imageView!!.changeAlpha(it.animatedValue as Int)
                                    }
                                    animator.start()
                                    increment()
                                }
                                Thread.sleep(2500)
                                runOnUiThread {
                                    updateUI()
                                }
                                Thread.sleep(1000)
                            }
                            isDetecting=false
                        })
                }

                Thread.sleep(100)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                    manager.openCamera(cameraId, stateCallback, mBackgroundHandler)
                    //startCamera()
                } else {
                    Toast.makeText(this@MainActivity, "Permission Not granted", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackGroundThread()
        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = textureListener
        }
    }

    private fun startBackGroundThread() {
        mBackgroundThread = HandlerThread("Camera Thread")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    override fun onPause() {
        stopBackGroundThread()
        super.onPause()

    }

    private fun stopBackGroundThread() {

        mBackgroundThread!!.quitSafely()
        mBackgroundThread!!.join()
        mBackgroundThread = null
        mBackgroundHandler = null
    }
}
