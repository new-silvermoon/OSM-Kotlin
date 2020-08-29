package org.silvermoon.osm_kotlin.mapunits

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Point
import android.view.View

class OsmTrackOverlay(mapView: OsmMapView) : OsmOverlay() {
    private val mTracks: MutableList<MapTrack>
    private var mStartLocation: GeoPoint? = null
    private val mMapView: OsmMapView
    private val mStartPoints: MutableMap<Path, Point>
    fun addTrack(track: MapTrack) {
        mTracks.add(track)
    }

    fun removeTracks() {
        mTracks.clear()
        mStartPoints.clear()
    }

    val tracks: List<MapTrack>
        get() = mTracks

    override fun draw(canvas: Canvas?, view: View?) {
        super.draw(canvas, view)
        for (track in mTracks) {
            if (track.path.isEmpty) {
                drawTrack(track.path)
            }
            offsetPath(track.path)
            canvas!!.drawPath(track.path, track.paint!!)
        }
    }

    private fun drawTrack(path: Path) {
        if (!path.isEmpty()) path.rewind()
        var previousPoint: GeoPoint? = null
        val startPoint = Point()
        startPoint.x = mMapView.offsetX
        startPoint.y = mMapView.offsetY
        mStartPoints[path] = startPoint
        for (track in mTracks) {
            for (g in track.track!!) {
                if (previousPoint == null) {
                    previousPoint = g
                    if (mStartLocation == null) mStartLocation = g
                    continue
                }
                drawLine(path, g, previousPoint)
                previousPoint = g
            }
            previousPoint = null
        }
    }

    override fun onZoomLevelChanges(view: View?) {
        super.onZoomLevelChanges(view)
        for (track in mTracks) {
            drawTrack(track.path)
        }
    }

    private fun offsetPath(path: Path?) {
        val startPoint: Point? = mStartPoints[path]
        if (startPoint == null && path != null && !path.isEmpty) {
            drawTrack(path)
        }
        if (startPoint != null && !path!!.isEmpty) {
            val x: Int = mMapView.offsetX - startPoint.x
            val y: Int = mMapView.offsetY - startPoint.y
            path.offset(x.toFloat(), y.toFloat())
            startPoint.x = mMapView.offsetX
            startPoint.y = mMapView.offsetY
        }
    }

    fun drawLine(
        path: Path,
        gp1: GeoPoint,
        gp2: GeoPoint
    ) {
        val p1: Point = convertGeoPointToPixel(gp1)
        val p2: Point = convertGeoPointToPixel(gp2)
        path.moveTo(p2.x.toFloat(), p2.y.toFloat())
        path.lineTo(p1.x.toFloat(), p1.y.toFloat())
    }

    private fun convertGeoPointToPixel(g: GeoPoint): Point {
        return mMapView.geopointToPixelProjection(g)
    }

    init {
        mTracks = ArrayList()
        mMapView = mapView
        mStartPoints = HashMap<Path, Point>()
    }
}