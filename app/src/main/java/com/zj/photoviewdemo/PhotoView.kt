package com.zj.photoviewdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class PhotoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val IMAGE_WIDTH = Utils.dpToPixel(300F)
    val bitmap = Utils.getBitmap(resources, IMAGE_WIDTH.toInt())
    val paint = Paint()


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(bitmap, 0F, 0F, paint)
    }
}

