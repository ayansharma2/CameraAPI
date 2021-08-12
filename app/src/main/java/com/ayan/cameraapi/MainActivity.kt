package com.ayan.cameraapi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.File

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.coroutines.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    val textureView: TextureView by lazy {
        findViewById<TextureView>(R.id.texture_view)
    }
    companion object{
        const val CAMERA_REQUEST=2
    }
    private lateinit var previewSurface:Surface
    lateinit var cameraId:String
    lateinit var cameraDevice:CameraDevice
    lateinit var captureCameraSession: CameraCaptureSession
    lateinit var captureRequest: CaptureRequest
    lateinit var captureRequestBuilder: CaptureRequest.Builder
    lateinit var leftEye:TextView
    lateinit var rightEye:TextView
    lateinit var parent_layout:ConstraintLayout
    private lateinit var imageDimensions:Size
//    lateinit var imageReader: ImageReader
    lateinit var  file:File
    lateinit var mBackgroundHandler: Handler
    lateinit var mBackgroundThread:HandlerThread
    lateinit var options:PoseDetectorOptions
    private lateinit var button: Button
    private val ORIENTATIONS = SparseIntArray()
    lateinit var imageView:ImageView
    lateinit var eye:TextView
    lateinit var canvas: Canvas
    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
    }
    lateinit var box:View
    lateinit var leftThumb:TextView
    lateinit var rightThumb:TextView
    lateinit var leftHeel:TextView
    lateinit var rightHeel:TextView
    lateinit var backEnd:Box
    private lateinit var poseDetector:PoseDetector
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_main)
        imageView=findViewById(R.id.image_view)
        options=PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
        eye=findViewById(R.id.eye_text)
        leftThumb=findViewById(R.id.left_thumb)
        rightThumb=findViewById(R.id.right_thumb)
        parent_layout=findViewById(R.id.parent_layout)
        leftHeel=findViewById(R.id.left_heel)
        rightHeel=findViewById(R.id.right_heel)
        mBackgroundThread= HandlerThread("camera Background")
        mBackgroundThread.start()
        box=findViewById(R.id.box)
        mBackgroundHandler=Handler(mBackgroundThread.looper)
        button=findViewById(R.id.click)
        button.setOnClickListener {
            takePicture()
        }
        leftEye=findViewById(R.id.left_eye)
        rightEye=findViewById(R.id.right_eye)
        canvas= Canvas()
        textureView.surfaceTextureListener= textureListener
        poseDetector=PoseDetection.getClient(options)

    }


    private fun takePicture() {
        TODO("Not yet implemented")
    }
    private val textureListener=object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            TODO("Not yet implemented")
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

        manager=getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId=manager.cameraIdList[1]
        val characteristics=manager.getCameraCharacteristics(cameraId)
        val map=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        imageDimensions=map!!.getOutputSizes(SurfaceTexture::class.java)[0]
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST)
            return ;
        }
        manager.openCamera(cameraId,stateCallback,mBackgroundHandler)
    }

    private val stateCallback=object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice=camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            TODO("Not yet implemented")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            TODO("Not yet implemented")
        }

    }

    @SuppressLint("NewApi")
    private fun createCameraPreview() {
        val texture=textureView.surfaceTexture
        val displayMetrics=DisplayMetrics()
        Resources.getSystem().displayMetrics.heightPixels
        backEnd=Box(Resources.getSystem().displayMetrics.widthPixels,Resources.getSystem().displayMetrics.heightPixels)
        Log.e("ImageDimensions","${imageDimensions.width.toString()}  ${imageDimensions.height.toString()}")
        texture!!.setDefaultBufferSize(1000,1000)
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


        val surface=Surface(texture)
        captureRequestBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        //captureRequestBuilder.addTarget(imageReader.surface)
        cameraDevice.createCaptureSession(mutableListOf(surface),object :CameraCaptureSession.StateCallback(){
            override fun onConfigured(session: CameraCaptureSession) {
                captureCameraSession=session
                updatePreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {

            }

        },null)
    }

    private fun updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO)
        captureCameraSession.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler)
        startImages()

    }

    @SuppressLint("InlinedApi")
    private fun startImages() {
        val background= box.background as GradientDrawable
        thread {
            while(true){

                val margin=backEnd.getConstraints()
                val width=backEnd.getBoxSize()
                runOnUiThread {
                    background.setStroke(15,Color.YELLOW)
                    //imageView.setImageBitmap(textureView.bitmap)
                    val lp=box.layoutParams
                    lp.height=imageDimensions.height
                    lp.width=width
                    box.layoutParams=lp
                    val constraintSet=ConstraintSet()
                    constraintSet.clone(parent_layout)
                    Log.e("MarginsAre","${margin.marginStart}  ${margin.marginTop}")
                    constraintSet.connect(box.id,ConstraintSet.START,
                    parent_layout.id,ConstraintSet.START,margin.marginStart)

                    constraintSet.connect(box.id,ConstraintSet.TOP,
                        parent_layout.id,ConstraintSet.TOP,0)
                    constraintSet.applyTo(parent_layout)
                }
                Thread.sleep(5000)
                val image=InputImage.fromBitmap(textureView.bitmap,0)
                val cropped=getRectangleShape(textureView.bitmap,margin,imageDimensions.height)
                //imageView.setImageBitmap(croped)
                poseDetector.process(image)
                    .addOnCompleteListener {pose->
                        if(pose.result.allPoseLandmarks.size>0){
                            poseDetector.process(InputImage.fromBitmap(cropped,0))
                                .addOnCompleteListener {it->
                                    if(it.result.allPoseLandmarks.size==0){
                                        runOnUiThread {
                                            background.setStroke(15,Color.GREEN)
                                        }
                                    }else{
                                        runOnUiThread {
                                            background.setStroke(15,Color.RED)
                                        }
                                    }
                                }
                        }else{
                            runOnUiThread {
                                background.setStroke(15,Color.RED)
                            }
                        }//else{
//                            val constraintSet=ConstraintSet()
//                            constraintSet.clone(parent_layout)
//                            constraintSet.connect(eye.id,ConstraintSet.START,parent_layout.id,ConstraintSet.START,0)
//                            constraintSet.connect(eye.id,ConstraintSet.TOP,parent_layout.id,ConstraintSet.TOP,0)
//                            constraintSet.applyTo(parent_layout)
//                        }
                    }
                    .addOnFailureListener {
                        Log.e("Exception",it.toString())
                    }

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

        when(requestCode){
            CAMERA_REQUEST->{
                if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"Permission Granted",Toast.LENGTH_LONG).show()
                    manager.openCamera(cameraId,stateCallback,mBackgroundHandler)
                    //startCamera()
                }else{
                    Toast.makeText(this@MainActivity,"Permission Not granted",Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(cameraId: String, activity: Activity, isFrontFacing: Boolean): Int {
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
            margin.marginStart.toFloat(),margin.marginTop.toFloat(),(margin.marginStart+height).toFloat(),(margin.marginStart+height).toFloat(),Path.Direction.CCW
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
}