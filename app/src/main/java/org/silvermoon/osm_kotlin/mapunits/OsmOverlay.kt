package org.silvermoon.osm_kotlin.mapunits

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View


abstract class OsmOverlay {
    open fun draw(canvas: Canvas?, view: View?) {}
    open fun onZoomLevelChanges(view: View?) {}
    open fun onSingleTap(event: MotionEvent?, view: View?): Boolean {
        return false
    }

    fun onInterceptSingleTap(event: MotionEvent?, view: View?): Boolean {
        return false
    }
}