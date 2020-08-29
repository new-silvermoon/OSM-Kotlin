package org.silvermoon.osm_kotlin.util

import org.silvermoon.osm_kotlin.mapunits.GeoPoint

object PolygonUtils {
    fun isPointInsidePolygon(
        polygon: List<GeoPoint>?,
        g: GeoPoint
    ): Boolean {
        if (polygon == null || polygon.size == 0) return false
        val x = IntArray(polygon.size)
        val y = IntArray(polygon.size)
        var i = 0
        for (point in polygon) {
            x[i] = point.longitudeE6
            y[i] = point.latitudeE6
            i++
        }
        return isPointInsidePolygon(x, y, g.longitudeE6, g.latitudeE6)
    }

    fun isPointInsidePolygon(
        X: IntArray,
        Y: IntArray,
        x: Int,
        y: Int
    ): Boolean {
        var i: Int
        var j: Int
        var c = false
        i = 0
        j = X.size - 2
        while (i < X.size - 1) {
            if ((Y[i] <= y && y < Y[j] || Y[j] <= y && y < Y[i]) &&
                x < (X[j] - X[i]) * (y - Y[i]) / (Y[j] - Y[i]) + X[i]
            ) c = !c
            j = i++
        }
        return c
    }
}