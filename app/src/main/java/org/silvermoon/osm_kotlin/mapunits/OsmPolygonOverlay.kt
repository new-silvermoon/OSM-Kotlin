package org.silvermoon.osm_kotlin.mapunits

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Point
import android.view.View

class OsmPolygonOverlay(private val mMapView: OsmMapView) : OsmOverlay() {
    private val mPolygons: MutableList<MapPolygon>
    private var mStartLocation: GeoPoint? = null
    private val mStartPoints: MutableMap<MapPolygon, Point>
    fun addPolygon(polygon: MapPolygon) {
        mPolygons.add(polygon)
    }

    val polygons: List<MapPolygon>
        get() = mPolygons

    fun removePolygons() {
        for (mapPolygon in mPolygons) mapPolygon.path.reset()
        mPolygons.clear()
        mStartPoints.clear()
    }

    override fun draw(canvas: Canvas?, view: View?) {
        super.draw(canvas, view)
        for (mapPolygon in mPolygons) {
            val path: Path = mapPolygon.path
            if (path.isEmpty()) drawPathForMapPolygon(mapPolygon)
            offsetPath(path)
            canvas!!.drawPath(path, mapPolygon.paintStroke!!)
            canvas.drawPath(path, mapPolygon.paint!!)
        }
    }

    private fun drawPathForMapPolygon(polygon: MapPolygon) {
        val path: Path = polygon.path
        if (!path.isEmpty()) path.rewind()
        var previousPoint: GeoPoint? = null
        val startPoint = Point()
        startPoint.x = mMapView.offsetX
        startPoint.y = mMapView.offsetY
        mStartPoints[polygon] = startPoint
        for (g in polygon.polygon!!) {
            if (previousPoint == null) {
                previousPoint = g
                val p: Point = convertGeoPointToPixel(g)
                path.moveTo(p.x.toFloat(), p.y.toFloat())
                if (mStartLocation == null) mStartLocation = g
                continue
            }
            drawLine(path, g)
            previousPoint = g
        }
        path.close()
        previousPoint = null
    }

    override fun onZoomLevelChanges(view: View?) {
        super.onZoomLevelChanges(view)
        for (polygon in mPolygons) {
            drawPathForMapPolygon(polygon)
        }
    }

    private fun offsetPath(path: Path?) {
        val startPoint: Point? = mStartPoints[path as MapPolygon]
        if (startPoint == null && path != null && !path.isEmpty()) {
            for (polygon in mPolygons) {
                drawPathForMapPolygon(polygon)
            }
        }
        if (startPoint != null && !path!!.isEmpty()) {
            val x: Int = mMapView.offsetX - startPoint.x
            val y: Int = mMapView.offsetY - startPoint.y
            path.offset(x.toFloat(), y.toFloat())
            startPoint.x = mMapView.offsetX
            startPoint.y = mMapView.offsetY
        }
    }

    private fun drawLine(
        path: Path,
        gp2: GeoPoint
    ) {
        val p2: Point = convertGeoPointToPixel(gp2)
        path.lineTo(p2.x.toFloat(), p2.y.toFloat())
    }

    private fun convertGeoPointToPixel(g: GeoPoint): Point {
        return mMapView.geopointToPixelProjection(g)
    }

    init {
        mStartPoints = HashMap<MapPolygon, Point>()
        mPolygons = ArrayList()
    }
}