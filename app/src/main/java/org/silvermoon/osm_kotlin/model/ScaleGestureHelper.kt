package org.silvermoon.osm_kotlin.model

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener


class ScaleGestureHelper(
    context: Context?,
    private val mListener: IScaleGestureListener?
) {
    private val mScaleGestureDetector: ScaleGestureDetector

    inner class MySimpleOnScaleGestureListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mListener?.onScale(detector.currentSpan)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            mListener?.onScaleEnd()
        }
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
        mScaleGestureDetector.onTouchEvent(event)
        return mScaleGestureDetector.isInProgress
    }

    interface IScaleGestureListener {
        fun onScale(distance: Float)
        fun onScaleEnd()
    }

    init {
        mScaleGestureDetector = ScaleGestureDetector(context, MySimpleOnScaleGestureListener())
    }
}