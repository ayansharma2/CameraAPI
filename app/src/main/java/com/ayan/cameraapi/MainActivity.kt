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
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import java.io.File

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.*
import android.opengl.Visibility
import android.os.*
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.Animation
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.*
import java.util.*
import kotlin.concurrent.thread
import android.graphics.drawable.Drawable

import androidx.appcompat.content.res.AppCompatResources





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
    lateinit var imageView: ImageView
    lateinit var eye: TextView
    lateinit var fab: FloatingActionButton
    lateinit var canvas: Canvas
    val shapes by lazy {
        arrayOf(
            getDrawable(R.drawable.tiangle_1), getDrawable(R.drawable.ic_polygon),
            getDrawable(R.drawable.ic_rectangle)
        )
    }

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
        imageView = findViewById(R.id.image_view)
        options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        fab = findViewById(R.id.goToFallingBoxes)
        parent_layout = findViewById(R.id.parent_layout)
        box = findViewById(R.id.box)
        timerLayout = findViewById(R.id.timer_layout)
        timer = findViewById(R.id.timer)
        detecting = findViewById(R.id.detecting)
        button = findViewById(R.id.click)
        fab.setOnClickListener {
            startActivity(Intent(this, FallingBoxes::class.java))
        }
        button.setOnClickListener {
            takePicture()
        }

        canvas = Canvas()
        poseDetector = PoseDetection.getClient(options)

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
    var margin=0
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
//        runOnUiThread {
//            val txForm=Matrix()
//            textureView.getTransform(txForm)
//            val textViewParams=textureView.layoutParams as ConstraintLayout.LayoutParams
//            textViewParams.width=Resources.getSystem().displayMetrics.heightPixels*(9/16)
//            textViewParams.height=Resources.getSystem().displayMetrics.heightPixels
//            textureView.layoutParams=textViewParams
//        }
//        imageReader= ImageReader.newInstance(imageDimensions.width,imageDimensions.height,
//        ImageFormat.RGB_565,50)
//        imageReader.setOnImageAvailableListener({reader->
//            val img=reader.acquireLatestImage()
//            val image=InputImage.fromMediaImage(img,getRotationCompensation(cameraId,this,false))
//            poseDetector.process(image)
//                .addOnCompleteListener {
//                    Log.e("Result",it.toString())
//                }
//            imageReader.discardFreeBuffers()
//            //reader.close()
//
//        },mBackgroundHandler)


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
        //detectPerson()
        startImages()
    }

    private fun detectPerson() {
        //Log.e("ScreenWidth","${textureView.width}  ${textureView.height}")
        thread {
            var detected = true
            while (true) {
                Thread.sleep(100)
                val image = InputImage.fromBitmap(textureView.bitmap, 0)
                //Log.e("ImageWidth",image.width.toString())
                poseDetector.process(image)
                    .addOnCompleteListener { pose ->
                        Log.e(
                            "POSE",
                            Gson().toJson(pose.result.getPoseLandmark(PoseLandmark.LEFT_KNEE))
                        )
                        if (pose.result.allPoseLandmarks.size > 0 && pose.result.getPoseLandmark(
                                PoseLandmark.NOSE
                            ).inFrameLikelihood > 0.5 &&
                            pose.result.getPoseLandmark(PoseLandmark.NOSE).position.x > 0 &&
                            pose.result.getPoseLandmark(PoseLandmark.NOSE).position.x < image.width
                            && pose.result.getPoseLandmark(PoseLandmark.NOSE).position.y > 0
                            && pose.result.getPoseLandmark(PoseLandmark.NOSE).position.y < image.height &&
                            pose.result.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.x > 0 &&
                            pose.result.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.x < image.width &&
                            pose.result.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.y > 0 &&
                            pose.result.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.y < image.height &&
                            pose.result.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.x > 0 &&
                            pose.result.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.x < image.width &&
                            pose.result.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.y > 0 &&
                            pose.result.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.y < image.height
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

    @SuppressLint("InlinedApi")
    private fun startImages() {
        var completed = true
        var i = 0
        thread {
            while (true) {
                if (completed) {

                    when (i) {
                        0 -> {
                           runOnUiThread {
                               Log.e("Codition1", "Yes")
                               val imageView = CustomRectangle(this@MainActivity,textureView.height,textureView.width,margin/2,0,)
                               val set = ConstraintSet()
                               parent_layout.addView(imageView)
                               imageView.id = View.generateViewId()
                               set.clone(parent_layout)
                               set.connect(
                                   imageView.id,
                                   ConstraintSet.START,
                                   textureView.id,
                                   ConstraintSet.START
                               )
                               set.connect(
                                   imageView.id,
                                   ConstraintSet.TOP,
                                   textureView.id,
                                   ConstraintSet.TOP
                               )
                               set.connect(
                                   imageView.id,
                                   ConstraintSet.BOTTOM,
                                   textureView.id,
                                   ConstraintSet.BOTTOM
                               )
                               set.applyTo(parent_layout)
                           }
                        }
                        1 -> {
                            runOnUiThread {
                                val imageView = ImageView(this@MainActivity)
                                val drawable = getDrawable(R.drawable.ic_polygon_transparent)
                                DrawableCompat.setTint(drawable!!, 0xFFFC0CB)
                                imageView.setImageDrawable(drawable)
                                imageView.id = View.generateViewId()
                                imageView.alpha = 0.5f
                                val set = ConstraintSet()
                                parent_layout.addView(imageView)
                                set.clone(parent_layout)
                                imageView.id = View.generateViewId()
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.START,
                                    parent_layout.id,
                                    ConstraintSet.START
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.TOP,
                                    textureView.id,
                                    ConstraintSet.TOP
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.BOTTOM,
                                    textureView.id,
                                    ConstraintSet.BOTTOM
                                )
                                set.applyTo(parent_layout)
                                var lp = imageView.layoutParams as ConstraintLayout.LayoutParams
                                lp.width = textureView.width / 2
                                lp.height = textureView.height
                            }
                        }
                        2 -> {
                            runOnUiThread {
                                val imageView = ImageView(this@MainActivity)
                                val drawable = getDrawable(R.drawable.ic_polygon_transparent)
                                DrawableCompat.setTint(drawable!!, 0xFFFC0CB)
                                imageView.setImageDrawable(drawable)
                                imageView.alpha = 0.5f
                                imageView.id = View.generateViewId()
                                val set = ConstraintSet()
                                parent_layout.addView(imageView)
                                set.clone(parent_layout)
                                imageView.id = View.generateViewId()
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.START,
                                    parent_layout.id,
                                    ConstraintSet.START
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.TOP,
                                    textureView.id,
                                    ConstraintSet.TOP
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.BOTTOM,
                                    textureView.id,
                                    ConstraintSet.BOTTOM
                                )
                                set.applyTo(parent_layout)
                                var lp = imageView.layoutParams as ConstraintLayout.LayoutParams
                                lp.width = textureView.width / 2
                                lp.height = textureView.height
                                var matrix = Matrix()
                                imageView.scaleType = ImageView.ScaleType.MATRIX
                                matrix.postRotate(90f)
                                imageView.imageMatrix = matrix
                            }
                        }
                        3 -> {
                            runOnUiThread {
                                val imageView = ImageView(this@MainActivity)
                                val drawable = getDrawable(R.drawable.ic_polygon_transparent)
                                var bitmap=(drawable as VectorDrawable).toBitmap(textureView.width,textureView.height,Bitmap.Config.ARGB_8888)
//                                var newDrawable=VectorDrawable(resources,Bitmap.createScaledBitmap(bitmap,textureView.width,textureView.height,false))
                                //DrawableCompat.setTint(drawable!!, 0xFFFC0CB)
                                imageView.setImageBitmap(bitmap)
                                //imageView.scaleType=ImageView.ScaleType.FIT_XY
                                //imageView.alpha = 0.5f
                                imageView.id = View.generateViewId()
                                val set = ConstraintSet()
                                parent_layout.addView(imageView)
                                set.clone(parent_layout)
                                //imageView.id = View.generateViewId()
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.START,
                                    parent_layout.id,
                                    ConstraintSet.START
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.TOP,
                                    textureView.id,
                                    ConstraintSet.TOP
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.BOTTOM,
                                    textureView.id,
                                    ConstraintSet.BOTTOM
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.END,
                                    textureView.id,
                                    ConstraintSet.END
                                )
                                set.applyTo(parent_layout)
                                var matrix = Matrix()
                                var lp=imageView.layoutParams as ConstraintLayout.LayoutParams
                                lp.height=textureView.height
                                lp.width=textureView.width
                                imageView.layoutParams=lp
                                imageView.scaleType = ImageView.ScaleType.MATRIX
                                matrix.postRotate(-90f)
                                //imageView.imageMatrix = matrix

                            }
                        }
                        4 -> {
                            runOnUiThread {
                                val imageView = ImageView(this@MainActivity)
                                imageView.setImageDrawable(getDrawable(R.drawable.ic_transparent_rectangle))
                                imageView.setBackgroundColor(0xFFFC0CB)
                                imageView.alpha = 0.5f
                                imageView.id = View.generateViewId()
                                val set = ConstraintSet()
                                parent_layout.addView(imageView)
                                set.clone(parent_layout)
                                imageView.id = View.generateViewId()
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.START,
                                    parent_layout.id,
                                    ConstraintSet.START
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.END,
                                    textureView.id,
                                    ConstraintSet.END
                                )
                                set.connect(
                                    imageView.id,
                                    ConstraintSet.BOTTOM,
                                    textureView.id,
                                    ConstraintSet.BOTTOM
                                )
                                set.applyTo(parent_layout)
                                var lp = imageView.layoutParams as ConstraintLayout.LayoutParams
                                lp.width = textureView.width
                                lp.height = textureView.height / 2
                            }
                        }

                    }
                }
                //i++
                completed = false
                Thread.sleep(1000)
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


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(
        cameraId: String,
        activity: Activity,
        isFrontFacing: Boolean
    ): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // Get the device's sensor orientation.
        val cameraManager = activity.getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360
        }
        return rotationCompensation
    }

    fun getRectangleShape(scaleBitmapImage: Bitmap?, margin: Margins, height: Int): Bitmap? {
        val targetWidth = scaleBitmapImage!!.width
        val targetHeight = scaleBitmapImage!!.height
        val targetBitmap = Bitmap.createBitmap(
            targetWidth,
            targetHeight, Bitmap.Config.ARGB_8888
        )
        var canvas = Canvas(targetBitmap)
        val path = Path()

        path.addRect(
            0f,
            0f,
            (textureView.width / 2).toFloat(),
            0f,
            Path.Direction.CCW
        )
        canvas.clipPath(path)
        canvas.drawBitmap(
            scaleBitmapImage,
            Rect(
                0, 0, scaleBitmapImage.width,
                scaleBitmapImage.height
            ),
            Rect(
                0, 0, targetWidth,
                targetHeight
            ), null
        )
        return targetBitmap
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