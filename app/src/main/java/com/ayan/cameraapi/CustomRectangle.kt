package com.ayan.cameraapi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewGroup

class CustomRectangle : View {
    private var viewHeight = 0
    private var viewWidth = 0
    var type = 0
    private var mRect: Rect = Rect()
    var mRect1: Rect = Rect()
    var mRect2: Rect = Rect()
    var mPaint: Paint = Paint()
    var canvas:Canvas?=null

    constructor(context: Context, height: Int, width: Int,type: Int) : super(context) {
        viewHeight = height
        viewWidth = width
        this.type = type
    }
    init {
        mPaint.color= 0xFdb5677
        mPaint.alpha=100
        when(type){
            0->{
                layoutParams=ViewGroup.LayoutParams(viewWidth/2,viewHeight)
            }
            1->{
                layoutParams=ViewGroup.LayoutParams(viewWidth,viewHeight)
            }
            2->{
                layoutParams=ViewGroup.LayoutParams(viewWidth,viewHeight)
            }
            3->{
                layoutParams=ViewGroup.LayoutParams(viewWidth,viewHeight)
            }
            4->{
                layoutParams=ViewGroup.LayoutParams(viewWidth,viewHeight)
            }

        }
    }
    fun tempAlpha(){
        mPaint.alpha=mPaint.alpha+4
        postInvalidate()
    }
    fun changeColor(newColor:Int){
        mPaint.color=newColor
        postInvalidate()
    }
    fun changeAlpha(newAlpha:Int){
        mPaint.color=0xF14b5a5
        mPaint.alpha=newAlpha
        postInvalidate()
    }
    override fun onDraw(canvas: Canvas?) {
        this.canvas=canvas
        when(type){
            0->{
                mRect.left=0
                mRect.top=0
                mRect.right=viewWidth/2
                mRect.bottom=viewHeight
                canvas!!.drawRect(mRect,mPaint)
            }
            1->{
                mRect.left=0
                mRect.top=0
                mRect.right=viewWidth/2
                mRect.bottom=viewHeight/2
                mRect1.left=viewWidth/2
                mRect1.top=0
                mRect1.right=viewWidth
                mRect1.bottom=viewHeight/2
                var mPaint1=Paint()
                mPaint1.color=Color.TRANSPARENT
                mRect2.left=0
                mRect2.top=viewHeight/2
                mRect2.right=viewWidth
                mRect2.bottom=viewHeight
                canvas!!.drawRect(mRect,mPaint)
                canvas.drawRect(mRect1,mPaint1)
                canvas.drawRect(mRect2,mPaint)
            }
            2->{
                mRect.left=0
                mRect.top=0
                mRect.right=viewWidth
                mRect.bottom=viewHeight/2
                mRect1.left=0
                mRect1.top=viewHeight/2
                mRect1.right=viewWidth/2
                mRect1.bottom=viewHeight
                var mPaint1=Paint()
                mPaint1.color=Color.TRANSPARENT
                mRect2.left=viewWidth/2
                mRect2.top=viewHeight/2
                mRect2.right=viewWidth
                mRect2.bottom=viewHeight
                canvas!!.drawRect(mRect,mPaint)
                canvas.drawRect(mRect1,mPaint)
                canvas.drawRect(mRect2,mPaint1)
            }
            3->{
                mRect.left=0
                mRect.top=0
                mRect.right=viewWidth/2
                mRect.bottom=viewHeight/2
                mRect1.left=viewWidth/2
                mRect1.top=0
                mRect1.right=viewWidth
                mRect1.bottom=viewHeight/2
                var mPaint1=Paint()
                mPaint1.color=Color.TRANSPARENT
                mRect2.left=0
                mRect2.top=viewHeight/2
                mRect2.right=viewWidth
                mRect2.bottom=viewHeight
                canvas!!.drawRect(mRect,mPaint1)
                canvas.drawRect(mRect1,mPaint)
                canvas.drawRect(mRect2,mPaint)
            }
            4->{
                mRect.left=0
                mRect.top=viewHeight/2
                mRect.right=viewWidth
                mRect.bottom=viewHeight

                canvas!!.drawRect(mRect,mPaint)
            }
        }
    }

}