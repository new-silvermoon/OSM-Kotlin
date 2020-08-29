package org.silvermoon.osm_kotlin.mapunits

import android.graphics.Paint
import android.graphics.Path

class MapTrack {
    var track: List<GeoPoint>? = null
    var startMarker: MapMarker? = null
    var endMarker: MapMarker? = null
    private var mPaint: Paint? = null
    private val mPath: Path

    var paint: Paint?
        get() = mPaint
        set(paint) {
            mPaint = paint
        }

    val path: Path
        get() = mPath

    init {
        mPath = Path()
    }
}