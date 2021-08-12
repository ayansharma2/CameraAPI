package com.ayan.cameraapi

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
        val marginStart= (0..width-boxWidth).random()
        return Margins(marginStart,height)
    }
}