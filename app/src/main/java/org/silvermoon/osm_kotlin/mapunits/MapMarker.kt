package org.silvermoon.osm_kotlin.mapunits

import android.graphics.Point
import android.graphics.drawable.Drawable


class MapMarker {
    var title: String? = null
    var coordinate: GeoPoint? = null
    var isFocused = false
    var drawable: Drawable? = null
    var drawableFocused: Drawable? = null
    var isClickable = true
    var zIndex: Short
    var position = 0
    var drawableBound = BOUND_CENTER_BOTTOM
    var drawableFocusedBound = BOUND_CENTER_BOTTOM
    var alpha = 255
    var tag: Any? = null
    private var mDrawableCustomBoundFocused: Point? = null
    private var mDrawableCustomBound: Point? = null

    fun setDrawableBound(customBoundOffset: Point?) {
        drawableBound = BOUND_CUSTOM
        mDrawableCustomBound = customBoundOffset
    }

    val drawableCustomBound: Point?
        get() = mDrawableCustomBound

    fun setDrawableFocusedBound(customBoundOffset: Point?) {
        drawableFocusedBound = BOUND_CUSTOM
        mDrawableCustomBoundFocused = customBoundOffset
    }

    val drawableFocusedCustomBound: Point?
        get() = mDrawableCustomBoundFocused

    companion object {
        var BOUND_CENTER_BOTTOM: Short = 1
        var BOUND_CENTER: Short = 2
        var BOUND_CUSTOM: Short = 3
        var Z_INDEX_VERY_LOW: Short = -2
        var Z_INDEX_LOW: Short = -1
        var Z_INDEX_DEFAULT: Short = 0
        var Z_INDEX_HIGH: Short = 1
        var Z_INDEX_VERY_HIGH: Short = 2
    }

    init {
        zIndex = Z_INDEX_DEFAULT
    }
}