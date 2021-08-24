package com.ayan.cameraapi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class CustomRectangle : View {
    var viewHeight = 0
    var viewWidth = 0
    var type = 0
    var mRect: Rect = Rect()
    var mPaint: Paint = Paint()
    var marginTop=0

    constructor(context: Context, height: Int, width: Int, marginTop:Int,type: Int) : super(context) {
        viewHeight = height
        viewWidth = width
        this.marginTop=marginTop
        this.type = type
    }


    override fun onDraw(canvas: Canvas?) {
        if (type == 0) {
            mRect.left=0
            mRect.top=0
            mRect.right=viewWidth/2
            mRect.bottom=viewHeight
            mPaint.color= Color.RED
            canvas!!.drawRect(mRect,mPaint)
        }
    }

}