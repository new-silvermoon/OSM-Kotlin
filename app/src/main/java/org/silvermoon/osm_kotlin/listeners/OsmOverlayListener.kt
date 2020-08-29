package org.silvermoon.osm_kotlin.listeners

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View


class OsmOverlayListener {
    fun draw(canvas: Canvas?, view: View?) {}
    fun onZoomLevelChanges(view: View?) {}
    fun onSingleTap(event: MotionEvent?, view: View?): Boolean {
        return false
    }
}