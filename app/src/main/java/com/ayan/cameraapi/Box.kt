package com.ayan.cameraapi

import android.util.Log
import java.lang.Integer.max

class Box(
    val width: Int,
    val height: Int,

) {
    var boxWidth:Int = 0

    var score=0
        get() = field
        set(value) {
            field = value
        }

    @JvmName("getBoxSize1")
    fun getBoxSize(): Int {
        boxWidth=(width/2..width).random()
        return boxWidth
    }

    fun getConstraints():Margins{
        Log.e("Width",width.toString())
        Log.e("BoxWidth",boxWidth.toString())
        var marginStart= (0..width-boxWidth).random()
        if(marginStart+boxWidth>width){
            marginStart=width-boxWidth-30
        }
        return Margins(marginStart,height)
    }
}