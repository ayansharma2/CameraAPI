package com.ayan.cameraapi

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.io.File
import kotlin.concurrent.thread

class FallingBoxes : AppCompatActivity() {
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
    var mBackgroundHandler: Handler?=null
    var mBackgroundThread: HandlerThread?=null
    lateinit var options: AccuratePoseDetectorOptions
    private lateinit var button: Button
    private val ORIENTATIONS = SparseIntArray()
    lateinit var imageView: ImageView
    lateinit var eye: TextView
    lateinit var canvas: Canvas
    var condition=true
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
        setContentView(R.layout.activity_falling_boxes)
        imageView = findViewById(R.id.image_view)
        options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()

        parent_layout = findViewById(R.id.parent_layout)
        box = findViewById(R.id.box)
        timerLayout = findViewById(R.id.timer_layout)
        timer = findViewById(R.id.timer)
        detecting = findViewById(R.id.detecting)

        button = findViewById(R.id.click)
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
            condition=false
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
                MainActivity.CAMERA_REQUEST
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
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
        }

    }

    @SuppressLint("NewApi")
    private fun createCameraPreview() {
        val texture = textureView.surfaceTexture
        val displayMetrics = DisplayMetrics()

        texture!!.setDefaultBufferSize(imageDimensions.width, imageDimensions.height)
        runOnUiThread {
            textureView.setAspectRatio(9, 16)
            val constraintSet = ConstraintSet()
            constraintSet.clone(parent_layout)
            val margin = Resources.getSystem().displayMetrics.heightPixels - textureView.height
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
                    .addOnCompleteListener { poses ->
                        Log.e(
                            "POSE",
                            Gson().toJson(poses.result!!.getPoseLandmark(PoseLandmark.LEFT_KNEE))
                        )
                        if (poses.result!!.allPoseLandmarks.size > 0 && poses.result!!.getPoseLandmark(
                                PoseLandmark.NOSE
                            ).inFrameLikelihood > 0.5 &&
                            poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.x > 0 &&
                            poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.x < image.width
                            && poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.y > 0
                            && poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.y < image.height &&
                            poses.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.x > 0 &&
                            poses.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.x < image.width &&
                            poses.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.y > 0 &&
                            poses.result!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE).position.y < image.height &&
                            poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.x > 0 &&
                            poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.x < image.width &&
                            poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.y > 0 &&
                            poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE).position.y < image.height
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
    var count = 0
    var marginStart = 0
    var index = 0
    var marginTop=0
    var bitmap: Bitmap? =null
    lateinit var view1: ImageView
    private fun startImages() {
        val background = box.background as GradientDrawable
        thread {
            while (condition) {
                val width = backEnd.getBoxSize()
                val margin = backEnd.getConstraints()

                runOnUiThread {
                    if (count % 10 == 0) {
                        marginStart =
                            textureView.width / 2 - 100//(0..textureView.width-200).random()
                        view1 = ImageView(this@FallingBoxes)
                        index = (shapes.indices).random()
                        view1.setImageDrawable(shapes[index])
                        val set = ConstraintSet()
                        view1.id = View.generateViewId()
                        parent_layout.addView(view1)
                        var lp = view1.layoutParams
                        lp.height = 200
                        lp.width = 200
                        view1.layoutParams = lp
                        set.clone(parent_layout)
                        set.connect(
                            view1.id,
                            ConstraintSet.START,
                            textureView.id,
                            ConstraintSet.START,
                            marginStart
                        )
                        set.connect(view1.id, ConstraintSet.TOP, textureView.id, ConstraintSet.TOP)
                        set.applyTo(parent_layout)
                        ObjectAnimator.ofFloat(
                            view1,
                            "translationY",
                            (textureView.height - 200).toFloat()
                        )
                            .apply {
                                duration = 1000
                                start()
                            }.doOnEnd {
                                parent_layout.removeView(view1)

                            }

                    }
                    marginTop+=(textureView.height)
                    count++;
//                    //background.setStroke(15,Color.YELLOW)
//                    //imageView.setImageBitmap(textureView.bitmap)
                    val lp = box.layoutParams
                    lp.height = textureView.height
                    lp.width = width
                    box.layoutParams = lp
//                    val constraintSet=ConstraintSet()
//                    constraintSet.clone(parent_layout)
//                    Log.e("MarginsAre","${margin.marginStart}  ${margin.marginTop}")
//                    constraintSet.connect(box.id,ConstraintSet.START,
//                    parent_layout.id,ConstraintSet.START,margin.marginStart)
//                    constraintSet.applyTo(parent_layout)
                }
                var bitmap = textureView.bitmap
                val cropped = Bitmap.createBitmap(bitmap!!, marginStart, 0, 200, textureView.height)
                //imageView.setImageBitmap(croped)
                val image2 = InputImage.fromBitmap(cropped, 0)
                //imageView.setImageBitmap(cropped)
                poseDetector.process(image2)
                    .addOnCompleteListener { poses ->

                        if (poses.result!!.allPoseLandmarks.size > 0 && poses.result!!.getPoseLandmark(
                                PoseLandmark.NOSE).position.x>0
                            && poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.x<cropped.width
                            && poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.y>0
                            && poses.result!!.getPoseLandmark(PoseLandmark.NOSE).position.y<cropped.height
                            && ((poses.result!!.getPoseLandmark(PoseLandmark.LEFT_KNEE).position.x>0 &&
                                    poses.result!!.getPoseLandmark(PoseLandmark.LEFT_KNEE).position.x<cropped.width
                                    && poses.result!!.getPoseLandmark(PoseLandmark.LEFT_KNEE).position.y>0
                                    && poses.result!!.getPoseLandmark(PoseLandmark.LEFT_KNEE).position.y<cropped.height) ||
                                    (poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_KNEE).position.x>0 &&
                                            poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_KNEE).position.x<cropped.width
                                            && poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_KNEE).position.y>0
                                            && poses.result!!.getPoseLandmark(PoseLandmark.RIGHT_KNEE).position.y<cropped.height)
                                    )) {
                            runOnUiThread {
                                when (index) {
                                    0 -> {
                                        view1.setImageDrawable(getDrawable(R.drawable.ic_green_triangle))
                                    }
                                    1 -> {
                                        view1.setImageDrawable(getDrawable(R.drawable.ic_polygon_green))
                                    }
                                    2 -> {
                                        view1.setImageDrawable(getDrawable(R.drawable.ic_green_square))
                                    }
                                }
                            }
                        } else {
                            when (index) {
                                0 -> {
                                    view1.setImageDrawable(getDrawable(R.drawable.ic_red_triangle))
                                }
                                1 -> {
                                    view1.setImageDrawable(getDrawable(R.drawable.ic_polygon_red))
                                }
                                2 -> {
                                    view1.setImageDrawable(getDrawable(R.drawable.ic_red_square))
                                }
                            }
                        }
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
            MainActivity.CAMERA_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                    manager.openCamera(cameraId, stateCallback, mBackgroundHandler)
                    //startCamera()
                } else {
                    Toast.makeText(this@FallingBoxes, "Permission Not granted", Toast.LENGTH_LONG)
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
        if(textureView.isAvailable){
            openCamera()
        }else{
            textureView.surfaceTextureListener = textureListener
        }
    }

    private fun startBackGroundThread() {
        mBackgroundThread= HandlerThread("Camera Thread")
        mBackgroundThread!!.start()
        mBackgroundHandler=Handler(mBackgroundThread!!.looper)
    }

    override fun onPause() {
        stopBackGroundThread()
        super.onPause()

    }

    private fun stopBackGroundThread() {

        mBackgroundThread!!.quitSafely()
        mBackgroundThread!!.join()
        mBackgroundThread=null
        mBackgroundHandler=null
    }
}








































