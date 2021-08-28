package com.ayan.cameraapi

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class PermissionsActivity : AppCompatActivity() {
    companion object{
        const val CAMERA_REQUEST_CODE=2
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        if(checkSelfPermission(android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
            startActivity(Intent(this@PermissionsActivity,MainActivity::class.java))
        }else{
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode== CAMERA_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            startActivity(Intent(this@PermissionsActivity,MainActivity::class.java))
        }else{
            Toast.makeText(this@PermissionsActivity,"Camera Permission Required to run Activity",Toast.LENGTH_SHORT).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}