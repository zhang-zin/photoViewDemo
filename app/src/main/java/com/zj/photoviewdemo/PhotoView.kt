package com.zj.photoviewdemo

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.core.math.MathUtils

class PhotoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val IMAGE_WIDTH = Utils.dpToPixel(300F)
    private val bitmap = Utils.getBitmap(resources, IMAGE_WIDTH.toInt())
    private val paint = Paint()
    private val gestureDetector = GestureDetector(context, PhotoGestureDetector())
    private val scaleGestureDetector = ScaleGestureDetector(
        context, PhotoScaleGestureListener()
    )
    private val scaleAnimator: ObjectAnimator = ObjectAnimator.ofFloat(this, "currentScale", 0f)
    val overScroller = OverScroller(context)

    private var originalOffsetX = 0F
    private var originalOffsetY = 0F
    private var offSetX = 0F
    private var offSetY = 0F
    private var smallScale = 0F
    private var bigScale = 0F
    private var currentScale = 0F
        set(value) {
            field = value
            invalidate()
        }
    private var isEnlarge = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        originalOffsetX = (width - bitmap.width) / 2F
        originalOffsetY = (height - bitmap.height) / 2F

        if (bitmap.width * 1F / bitmap.height > width * 1F / height) {
            // 宽铺满全屏
            smallScale = width * 1F / bitmap.width
            bigScale = height * 1F / bitmap.height
        } else {
            smallScale = height * 1F / bitmap.height
            bigScale = width * 1F / bitmap.width
        }
        currentScale = smallScale
    }

    override fun onDraw(canvas: Canvas?) {

        super.onDraw(canvas)
        canvas?.run {
            // 双击缩小时，需要把图片放回中心位置,缩小时currentScale = smallScale，scaleFaction = 0
            // 放大时currentScale = bigScale, scaleFaction = 1
            val scaleFaction = (currentScale - smallScale) / (bigScale - smallScale)
            translate(offSetX * scaleFaction, offSetY * scaleFaction)
            scale(currentScale, currentScale, width / 2F, height / 2F)
            drawBitmap(bitmap, originalOffsetX, originalOffsetY, paint)  // 把图片移到中间
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var result = scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            result = gestureDetector.onTouchEvent(event)
        }
        return result
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scaleAnimator.cancel()
    }

    private fun getScaleAnimation(): ObjectAnimator {
        scaleAnimator.setFloatValues(smallScale, bigScale)
        return scaleAnimator
    }

    /**
     * 修正移动位置，不露出白边
     */
    private fun fixOffSet() {
        offSetX = MathUtils.clamp(
            offSetX,
            -(bitmap.width * bigScale - width) / 2,
            (bitmap.width * bigScale - width) / 2
        )
        offSetY = MathUtils.clamp(
            offSetY,
            -(bitmap.height * bigScale - height) / 2,
            (bitmap.height * bigScale - height) / 2
        )
    }

    inner class PhotoGestureDetector : GestureDetector.SimpleOnGestureListener() {
        // up触发，单击或者双击的第一次会触发，up时如果不是双击的第二次点击，不是长按则触发
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return super.onSingleTapUp(e)
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        /**
         * up 惯性滑动 大于50dp/s
         * velocityX ： x轴方向的运动速度
         */
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
//            if (isEnlarge) {
//                overScroller.fling(
//                    offSetX.toInt(), offSetY.toInt(), velocityX.toInt(), velocityY.toInt(),
//                    (-(bitmap.width * bigScale - width) / 2).toInt(),
//                    ((bitmap.width * bigScale - width) / 2).toInt(),
//                    (-(bitmap.height * bigScale - height) / 2).toInt(),
//                    ((bitmap.height * bigScale - height) / 2).toInt(),
//                    300, 300
//                )
//                postOnAnimation(FlingRunnable())
//            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        // 双击的第二次点击down时触发，双击触发时间 40ms --- 300ms
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            isEnlarge = !isEnlarge
            if (isEnlarge) {
                e?.run {
                    // 放大是以图片的中心放大，为了更好的效果放大点需要移回手指点击的位置
                    offSetX = (1 - bigScale / smallScale) * (x - width / 2)
                    offSetY = (1 - bigScale / smallScale) * (y - height / 2)
                    fixOffSet()
                }
                getScaleAnimation().start()
            } else {
                getScaleAnimation().reverse()
            }
            return super.onDoubleTap(e)
        }

        /**
         * 滚动
         *
         * e1:手指按下
         * e2:当前的
         * distanceX ：旧位置-新位置
         */
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (isEnlarge) {
                offSetX -= distanceX
                offSetY -= distanceY
            } else {
                offSetX += distanceX
                offSetY += distanceY
            }
//            fixOffSet()
            invalidate()
            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        // 单击按下时触发，双击时不触发，down，up时都可能触发
        // 延时300ms触发TAP事件
        // 300ms以内抬手 -- 才会触发TAP -- onSingleTapConfirmed
        // 300ms 以后抬手 --- 不是双击不是长按，则触发
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            return super.onSingleTapConfirmed(e)
        }

        // 延时100ms触发 -- 处理点击效果
        override fun onShowPress(e: MotionEvent?) {
            super.onShowPress(e)
        }

        // 双击的第二次down、move、up 都触发
        override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
            return super.onDoubleTapEvent(e)
        }

        // 长按 -- 默认300ms触发
        override fun onLongPress(e: MotionEvent?) {
            super.onLongPress(e)
        }
    }

    inner class FlingRunnable : Runnable {
        override fun run() {
            if (overScroller.computeScrollOffset()) {
                offSetX = overScroller.currX.toFloat()
                offSetY = overScroller.currY.toFloat()
                invalidate()
                postOnAnimation(this)
            }
        }
    }

    inner class PhotoScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {

        private var initScale = 0F;

        override fun onScaleBegin(p0: ScaleGestureDetector?): Boolean {
            initScale = currentScale
            return true
        }

        override fun onScaleEnd(p0: ScaleGestureDetector?) {
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector?.run {
                currentScale = initScale * scaleFactor
                if (currentScale <= smallScale) {
                    currentScale = smallScale
                } else if (currentScale >= bigScale * 1.5F) {
                    currentScale = bigScale * 1.5F
                }
                invalidate()
            }
//            isEnlarge = (currentScale >= smallScale)
            return false
        }

    }
}

