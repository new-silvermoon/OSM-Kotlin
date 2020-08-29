package org.silvermoon.osm_kotlin.listeners

import android.graphics.Canvas
import android.view.MotionEvent
import org.silvermoon.osm_kotlin.mapunits.MapMarker


interface IMapInteractionListener {
    fun onMapTouchEvent(event: MotionEvent?): Boolean
    fun onMapDraw(canvas: Canvas?)
    fun onMapSingleTapConfirmed(event: MotionEvent?)
    fun onMapLongClick(event: MotionEvent?)
    fun onMapMarkerTap(overlayItem: MapMarker?)
    fun onMapStopPanning()
    fun onMapZoomChanged(zoomLevel: Int)
    fun onMapCalloutTap(event: MotionEvent?)
}