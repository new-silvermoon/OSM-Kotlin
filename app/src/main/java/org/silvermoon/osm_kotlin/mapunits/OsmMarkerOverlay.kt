package org.silvermoon.osm_kotlin.mapunits

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import org.silvermoon.osm_kotlin.util.CountDownTimer
import java.util.Collections
import java.util.LinkedList

class OsmMarkerOverlay(private val mOsmMapView: OsmMapView, private val mDrawable: Drawable?) :
    OsmOverlay() {

    private val mMarkers: MutableList<MapMarker> = ArrayList()
    private val mPaint = Paint()

    fun addMarkers(markers: List<MapMarker>) {
        mMarkers.addAll(markers)
        sortItems()
    }

    fun addMarker(mapMarker: MapMarker) {
        mMarkers.add(mapMarker)
        sortItems()
    }

    fun addMarkersFadeIn(markers: List<MapMarker>) {

        for (marker in markers)
            marker.alpha = 0

        addMarkers(markers)

        object : CountDownTimer(300, 30) {
            override fun onTick(millisUntilFinished: Long) {
                var alpha: Int
                for (marker in markers) {
                    alpha = marker.alpha + 25
                    if (alpha > 255)
                        alpha = 255
                    marker.alpha = alpha
                }
                mOsmMapView.invalidate()
            }

            override fun onFinish() {
                for (marker in markers)
                    marker.alpha = 255
                mOsmMapView.invalidate()

            }
        }.start()
    }

    fun removeMarkers() {
        mMarkers.clear()
    }

    fun removeMarker(markerToRemove: MapMarker) {
        val it = mMarkers.iterator()
        while (it.hasNext()) {
            val marker = it.next()
            if (markerToRemove === marker) {
                it.remove()
            }
        }
    }

    fun removeMarkersFadeOut(markers: List<MapMarker>) {

        for (marker in markers)
            marker.alpha = 255

        object : CountDownTimer(500, 50) {
            override fun onTick(millisUntilFinished: Long) {
                var alpha: Int
                for (marker in markers) {
                    alpha = marker.alpha - 25
                    if (alpha < 0)
                        alpha = 0
                    marker.alpha = alpha
                }
                mOsmMapView.invalidate()
            }

            override fun onFinish() {
                removeMarkers(markers)
                mOsmMapView.invalidate()

            }
        }.start()
    }

    fun removeMarkers(markersToRemove: List<MapMarker>) {
        //	mMarkers.removeAll(markersToRemove);
        val it = mMarkers.iterator()
        while (it.hasNext()) {
            val marker = it.next()
            if (markersToRemove.contains(marker)) {
                it.remove()
            }
        }
    }

    val markers: List<MapMarker>
        get() = mMarkers

    fun onTap(markerTapped: MapMarker) {

        try {

            val mapListener = mOsmMapView.mapIntereractionListener

            val it = mMarkers.iterator()

            while (it.hasNext()) {
                val mapMarker = it.next()
                if (mapMarker === markerTapped && mapMarker.isClickable) {
                    mapListener?.onMapMarkerTap(mapMarker)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun draw(canvas: Canvas, view: OsmMapViewBase) {
        super.draw(canvas, view)

        var p: Point
        var bitmap: Bitmap
        var g: GeoPoint
        var selectedMarker: MapMarker? = null
        for (marker in mMarkers) {

            g = marker.coordinate
            p = mOsmMapView.geopointToPixelProjection(g)

            if (marker.isFocused) {
                selectedMarker = marker
                continue
            }

            if (marker.drawable != null)
                bitmap = (marker.drawable as BitmapDrawable).bitmap
            else if (mDrawable != null)
                bitmap = (mDrawable as BitmapDrawable).bitmap
            else
                continue

            setBoundForBitmap(marker.drawableBound, bitmap, p, marker)

            mPaint.alpha = marker.alpha
            canvas.drawBitmap(bitmap, p.x.toFloat(), p.y.toFloat(), mPaint)
        }

        if (selectedMarker != null)
            drawMarker(view.context, canvas, selectedMarker)
    }

    private fun drawMarker(context: Context, canvas: Canvas, selectedMarker: MapMarker) {
        val bitmap: Bitmap?
        val g = selectedMarker.coordinate
        val p = mOsmMapView.geopointToPixelProjection(g)

        bitmap = (selectedMarker.drawableFocused as BitmapDrawable).bitmap

        setBoundForBitmap(selectedMarker.drawableFocusedBound, bitmap, p, selectedMarker)

        mPaint.alpha = selectedMarker.alpha
        canvas.drawBitmap(bitmap, p.x.toFloat(), p.y.toFloat(), mPaint)
    }

    private fun setBoundForBitmap(bound: Short, bitmap: Bitmap, p: Point, marker: MapMarker) {

        if (bound == MapMarker.BOUND_CENTER) {
            p.x -= bitmap.width / 2
            p.y -= bitmap.height / 2
        } else if (bound == MapMarker.BOUND_CENTER_BOTTOM) {
            p.x -= bitmap.width / 2
            p.y -= bitmap.height
        } else if (bound == MapMarker.BOUND_CUSTOM) {
            val customBound =
                if (marker.isFocused) marker.drawableFocusedCustomBound else marker.drawableCustomBound
            p.x -= bitmap.width - customBound.x
            p.y -= bitmap.height - customBound.y
        }
    }

    override fun onSingleTap(event: MotionEvent, view: OsmMapViewBase): Boolean {
        super.onSingleTap(event, view)

        val tappedItems = HashMap<MapMarker, Int>()

        for (marker in mMarkers) {

            val itemCoord = marker.coordinate
            val itemPoint = mOsmMapView.geopointToPixelProjection(itemCoord)

            var itemWidth = 0
            var itemHeight = 0


            if (marker.drawable != null) {
                itemWidth = marker.drawable!!.intrinsicWidth
                itemHeight = marker.drawable!!.intrinsicHeight
            } else if (mDrawable != null) {
                itemWidth = mDrawable.intrinsicWidth
                itemHeight = mDrawable.intrinsicHeight
            }

            //Log.i("", "itemWidth = " + itemWidth+ " itemHeight=" + itemHeight);

            if (itemWidth == 0 || itemHeight == 0)
                continue

            val XboundaryMax = itemPoint.x + (itemWidth / 2)
            val XboundaryMin = itemPoint.x - (itemWidth / 2)
            var YboundaryMax = 0
            var YboundaryMin = 0

            if (marker.drawableBound == MapMarker.BOUND_CENTER) {

                YboundaryMax = itemPoint.y + (itemHeight / 2)
                YboundaryMin = itemPoint.y - (itemHeight / 2)

            } else {

                YboundaryMax = itemPoint.y
                YboundaryMin = itemPoint.y - itemHeight

            }

            //Log.i("", "getX = " + event.getX() + " XboundaryMax=" + XboundaryMax);
            //Log.i("", "event.getY()" + event.getY() + " YboundaryMax=" + YboundaryMax);

            if (event.x <= XboundaryMax && event.x >= XboundaryMin
                && event.y <= YboundaryMax && event.y >= YboundaryMin
            ) {

                var score = 0
                val x1 = event.x - XboundaryMin
                val x2 = XboundaryMax - event.x
                val y1 = event.y - YboundaryMin
                val y2 = YboundaryMax - event.y

                if (x1 < x2 && y1 < y2)
                    score = (x1 * y1).toInt()
                else if (x2 < x1 && y2 < y1)
                    score = (x2 * y2).toInt()
                else if (x2 < x1 && y1 < y2)
                    score = (x2 * y1).toInt()
                else if (x1 < x2 && y2 < y1)
                    score = (x1 * y2).toInt()

                tappedItems[marker] = score
            }

        }

        if (tappedItems.size == 0) {
            return false
        }

        if (tappedItems.size == 1) {
            for (marker in tappedItems.keys) {
                onTap(marker)
                return true
            }
        }

        val tappedItemSorted = sortByValue(tappedItems, false)

        //Log.i("", "Items tapped size = " + tappedItems.size());

        for (marker in tappedItemSorted.keys) {
            onTap(marker)
            return true
        }

        return false
    }

    private fun <K, V : Comparable<V>> sortByValue(map: Map<K, V>, sortASC: Boolean): Map<K, V> {

        val list = LinkedList(map.entries)
        Collections.sort(list) { o1, o2 ->
            if (sortASC)
                (o1.value).compareTo(o2.value)
            else
                (o1.value).compareTo(o2.value) * -1
        }

        val result = LinkedHashMap<K, V>()
        for ((key, value) in list) {
            result[key] = value
        }
        return result
    }

    protected fun sortItems() {

        val markerOverlays = ArrayList<MapMarker>()

        for (i in mMarkers.indices) {
            val item = mMarkers[i]
            if (item == null)
                continue
            val itemCoord = item.coordinate
            if (itemCoord != null) {
                markerOverlays.add(item)
            }
        }

        Collections.sort(markerOverlays) { o1, o2 ->
            val o1Lat = o1.coordinate.latitudeE6
            val o2Lat = o2.coordinate.latitudeE6

            if (o1.zIndex > o2.zIndex)
                return@sort 1
            else if (o2.zIndex > o1.zIndex)
                return@sort -1

            if (o1Lat > o2Lat)
                -1
            else if (o1Lat == o2Lat)
                0
            else
                1
        }

        mMarkers.clear()
        mMarkers.addAll(markerOverlays)
    }

}
