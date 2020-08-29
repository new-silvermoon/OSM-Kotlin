package org.silvermoon.osm_kotlin.mapunits

import android.graphics.Paint
import android.graphics.Path

class MapPolygon {
    var polygon: List<GeoPoint>? =
        null
    private var mPaintStroke: Paint? = null
    private var mPaint: Paint? = null
    private var mPath: Path

    var paintStroke: Paint?
        get() = mPaintStroke
        set(paint) {
            mPaintStroke = paint
        }

    var paint: Paint?
        get() = mPaint
        set(paint) {
            mPaint = paint
        }

    var path: Path
        get() = mPath
        set(path) {
            mPath = path
        }

    init {
        mPath = Path()
    }
}