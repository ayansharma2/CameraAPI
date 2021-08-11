package com.ayan.cameraapi

class Box(
    val width: Int,
    val height: Int,

) {
    var boxSize = 50
    var score=0
        get() = field
        set(value) {
            field = value
        }

    @JvmName("getBoxSize1")
    fun getBoxSize(): Int {
        boxSize += 50
        return boxSize-50
    }

    fun getConstraints():Margins{
        val marginStart= (0..width-boxSize).random()
        val marginTop=(0..height-boxSize).random()
        return Margins(marginStart,marginTop)
    }
}