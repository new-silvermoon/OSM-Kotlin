package org.silvermoon.osm_kotlin.mapunits


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import org.silvermoon.osm_kotlin.R
import org.silvermoon.osm_kotlin.listeners.IMapInteractionListener
import org.silvermoon.osm_kotlin.model.ScaleGestureHelper
import org.silvermoon.osm_kotlin.model.ScaleGestureHelper.IScaleGestureListener
import org.silvermoon.osm_kotlin.util.CountDownTimer


class OsmMapView(
    context: Context?,
    mapbuilder: OsmMapViewBuilder,
    mapInteractionListener: IMapInteractionListener?
) :
    OsmMapViewBase(context, mapbuilder.mapTypeId) {
    val mapIntereractionListener: IMapInteractionListener?
    private val mMapOverlays: MutableList<OsmOverlay>
    private val mDetector: GestureDetector

    private var mScaleGesture: ScaleGestureHelper? = null
    private var mZoomFactorForScaleGesture = 0
    private var mIsScrolling = false
    private val mMarkerOverlay: OsmMarkerOverlay
    private val mTrackOverlay: OsmTrackOverlay
    private val mPolygonOverlay: OsmPolygonOverlay
    private val mTrackStartEndMarkerOverlay: OsmMarkerOverlay
    private var mActionDownEventX = 0f
    private var mActionDownEventY = 0f
    private var mActionMoveEventX = 0f
    private var mActionMoveEventY = 0f
    private var mAnimationOffsetRight = 0f
    private var mAnimationOffsetLeft = 0f
    private var mAnimationOffsetTop = 0f
    private var mAnimationOffsetBottom = 0f
    fun addMarker(marker: MapMarker?) {
        mMarkerOverlay.addMarker(marker)
        invalidate()
    }

    fun addMarkers(markers: List<MapMarker?>?) {
        mMarkerOverlay.addMarkers(markers)
        invalidate()
    }

    fun addMarkersFadeIn(markers: List<MapMarker?>?) {
        mMarkerOverlay.addMarkersFadeIn(markers)
    }

    fun removeMarkers(markers: List<MapMarker?>?) {
        mMarkerOverlay.removeMarkers(markers)
        invalidate()
    }

    fun removeMarkersFadeOut(markers: List<MapMarker?>?) {
        mMarkerOverlay.removeMarkersFadeOut(markers)
    }

    fun removeMarkers() {
        mMarkerOverlay.removeMarkers()
        invalidate()
    }

    fun removeMarker(marker: MapMarker?) {
        mMarkerOverlay.removeMarker(marker)
        invalidate()
    }

    fun addOverlay(overlay: OsmOverlay) {
        mMapOverlays.add(overlay)
        invalidate()
    }

    fun removeOverlay(overlay: OsmOverlay?) {
        mMapOverlays.remove(overlay)
        invalidate()
    }

    override fun invalidate() {
        super.invalidate()
    }

    val markers: List<MapMarker>
        get() = mMarkerOverlay.getMarkers()

    fun addTracks(
        tracks: List<MapTrack>,
        showStartEndMarkers: Boolean
    ) {
        var lastTrack: MapTrack? = null
        var startMarker: MapMarker? = null
        for (track in tracks) {
            if (startMarker == null) {
                startMarker = track.startMarker
            }
            mTrackOverlay.addTrack(track)
            lastTrack = track
        }
        if (showStartEndMarkers) {
            if (lastTrack != null && lastTrack.endMarker != null) mTrackStartEndMarkerOverlay.addMarker(
                lastTrack.endMarker
            )
            if (startMarker != null) mTrackStartEndMarkerOverlay.addMarker(startMarker)
        }
    }

    fun removeTracks() {
        mTrackOverlay.removeTracks()
        mTrackStartEndMarkerOverlay.removeMarkers()
    }

    override fun clear() {
        removeMarkers()
        removeTracks()
        removePolygons()
        mMapOverlays.clear()
        super.clear()
    }

    fun getLatitudeSpanE6(mapWidth: Int, mapHeight: Int): Int {
        val top = offsetY
        val bottom: Int
        bottom = offsetY - mapHeight
        val gTop =
            getProjectionFromPixels(offsetX, top)
        val gBottom =
            getProjectionFromPixels(offsetX, bottom)
        return gTop.latitudeE6 - gBottom.latitudeE6
    }

    fun getLongitudeSpanE6(mapWidth: Int, mapHeight: Int): Int {
        val left = offsetX
        val right: Int
        right = offsetX + mapWidth
        val gRight =
            getProjectionFromPixels(right, offsetY)
        val gLeft =
            getProjectionFromPixels(left, offsetY)
        return gLeft.longitudeE6 - gRight.longitudeE6
    }

    fun zoomInOneLevel(): Boolean {
        return animateZoomIn()
    }

    fun zoomOutOneLevel(): Boolean {
        return animateZoomOut()
    }

    fun setCenter(location: GeoPoint?) {
        if (location != null) setCenter(location.latitudeE6 / 1E6, location.longitudeE6 / 1E6)
    }

    fun setCenter(
        maxLat: Double,
        maxLon: Double,
        minLat: Double,
        minLon: Double
    ) {

//		if (getWidth() == 0 && getHeight() == 0) {
//			mSetCenterForBBoxWhenGetView = new Double[4];
//			mSetCenterForBBoxWhenGetView[0] = maxLat;
//			mSetCenterForBBoxWhenGetView[1] = maxLon;
//			mSetCenterForBBoxWhenGetView[2] = minLat;
//			mSetCenterForBBoxWhenGetView[3] = minLon;
//			return;
//		}
        val centerLat = maxLat - (maxLat - minLat) / 2
        val centerLon = maxLon - (maxLon - minLon) / 2
        setCenter(centerLat, centerLon)
    }

    fun setZoom(
        maxLat: Double,
        maxLon: Double,
        minLat: Double,
        minLon: Double,
        paddingWidth: Int,
        paddingHeight: Int
    ) {
        val zoom = Projection.getZoomLevelFromBox(
            maxLat, maxLon, minLat, minLon,
            width, height, paddingWidth, paddingHeight
        )
        setZoom(zoom)
    }

    //	@Override
    //	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    //		super.onSizeChanged(w, h, oldw, oldh);
    //
    //		if (mSetCenterForBBoxWhenGetView != null && w > 0 && h > 0) {
    //			setCenterAndZoom(mSetCenterForBBoxWhenGetView[0], mSetCenterForBBoxWhenGetView[1],
    //					mSetCenterForBBoxWhenGetView[2], mSetCenterForBBoxWhenGetView[3]);
    //			mSetCenterForBBoxWhenGetView = null;
    //		}
    //	}
    val zoomLevel: Int
        get() = mZoomLevel

    fun translate(pixelX: Int, pixelY: Int) {
        offsetX = offsetX + pixelX
        offsetY = offsetY + pixelY
        invalidate()
    }

    fun animateTranslation(
        pixelX: Int, pixelY: Int, animationTime: Int,
        listener: OnAnimateTranslationListener?
    ) {
        val interval: Long = 30
        mAnimationOffsetBottom = 0f
        mAnimationOffsetLeft = 0f
        mAnimationOffsetRight = 0f
        mAnimationOffsetTop = 0f
        val multiplier =
            animationTime.toFloat() / interval / animationTime.toDouble()
        object : CountDownTimer(animationTime.toLong(), interval) {
            override fun onTick() {
                if (mAnimationOffsetLeft < pixelX || mAnimationOffsetRight > pixelX || mAnimationOffsetBottom > pixelY || mAnimationOffsetTop < pixelY
                ) {
                    var progressPositionX = 0
                    if (mAnimationOffsetLeft < pixelX) {
                        progressPositionX = Math.round(pixelX * multiplier).toInt()
                        mAnimationOffsetLeft = mAnimationOffsetLeft + progressPositionX
                    }
                    if (mAnimationOffsetRight > pixelX) {
                        progressPositionX = Math.round(pixelX * multiplier).toInt()
                        mAnimationOffsetRight = mAnimationOffsetRight + progressPositionX
                    }
                    var progressPositionY = 0
                    if (mAnimationOffsetBottom > pixelY) {
                        progressPositionY = Math.round(pixelY * multiplier).toInt()
                        mAnimationOffsetBottom = mAnimationOffsetBottom + progressPositionY
                    }
                    if (mAnimationOffsetTop < pixelY) {
                        progressPositionY = Math.round(pixelY * multiplier).toInt()
                        mAnimationOffsetTop = mAnimationOffsetTop + progressPositionY
                    }
                    translate(progressPositionX, progressPositionY)
                    invalidate()
                } else {
                    this.cancel()
                }
            }

            override fun onFinish() {
                invalidate()
                listener?.onAnimateTranslationEnds()
            }
        }.start()
    }

    fun pixelToGeoPointProjection(x: Int, y: Int): GeoPoint {
        val offsetX = offsetX - x
        val offsetY = offsetY - y
        return getProjectionFromPixels(offsetX, offsetY)
    }

    fun geopointToPixelProjection(coordinate: GeoPoint?): Point {
        if (coordinate == null) {
            return Point(0, 0)
        }
        val lat = coordinate.latitudeE6 / 1E6
        val lon = coordinate.longitudeE6 / 1E6
        val x = Projection.getXPixelFromLongitude(
            lon,
            zoomLevel
        ).toDouble()
        val y =
            Projection.getYPixelFromLatitude(lat, zoomLevel).toDouble()
        val point = Point()
        point.x = (offsetX - (0 - x)).toInt()
        point.y = (offsetY - (0 - y)).toInt()
        return point
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //mLocationOverlay.dispatchDraw(canvas, this, mLocation, mLocationHeading);
        mapIntereractionListener!!.onMapDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mActionDownEventX = event.x
                mActionDownEventY = event.y
                mActionMoveEventX = event.x
                mActionMoveEventY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                mActionMoveEventX = event.x
                mActionMoveEventY = event.y
                mIsScrolling = true
            }
            MotionEvent.ACTION_UP -> {
                if (mIsScrolling && mapIntereractionListener != null) {
                    mapIntereractionListener.onMapStopPanning()
                }
                mIsScrolling = false
            }
        }
        if (mapIntereractionListener!!.onMapTouchEvent(event)) return true
        return if (mScaleGesture != null && mScaleGesture!!.onTouchEvent(event)) true else mDetector.onTouchEvent(
            event
        )
    }

    fun getMaxZoomLevel(): Int {
        return MAX_ZOOM_LEVEL
    }

    override fun onDoubleTap(arg0: MotionEvent): Boolean {
        mIsDoubleTap = true
        animateZoomIn()
        //mMapInteractionListener.onMapZoomChanged(mPendingZoomLevel);
        return false
    }

    override fun onDoubleTapEvent(arg0: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        mapIntereractionListener!!.onMapSingleTapConfirmed(event)
        return super.onSingleTapConfirmed(event)
    }

    override fun onLongPress(event: MotionEvent) {
        super.onLongPress(event)
        try {
            val xDownRounded = Math.round(mActionDownEventX / 20f)
            val yDownRounded = Math.round(mActionDownEventY / 20f)
            val xMoveRounded = Math.round(mActionMoveEventX / 20f)
            val yMoveRounded = Math.round(mActionMoveEventY / 20f)
            //Log.i("", "xDownRounded = " + xDownRounded + "  xMoveRounded = " + xMoveRounded);
            //Log.i("", "yDownRounded = " + yDownRounded + "  yMoveRounded = " + yMoveRounded);
            if (xDownRounded == xMoveRounded && yDownRounded == yMoveRounded && mapIntereractionListener != null
            ) {
                mapIntereractionListener.onMapLongClick(event)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setCenterAndZoomOnTracksAndMarkers(): Boolean {
        val bbox = getBBoxForTracksAndMarkers()
        if (bbox != null) {
            setZoom(bbox[0]!!, bbox[1]!!, bbox[2]!!, bbox[3]!!, 0, 0)
            setCenter(bbox[0]!!, bbox[1]!!, bbox[2]!!, bbox[3]!!)
            return true
        }
        return false
    }

    fun getBBoxForTracksAndMarkers(): Array<Double?>? {
        val markers = markers
        var lat = 0
        var lon = 0
        var maxLat: Int? = null
        var maxLon: Int? = null
        var minLat: Int? = null
        var minLon: Int? = null
        for (marker in markers) {
            val geoPoint = marker.coordinate
            lat = geoPoint!!.latitudeE6
            lon = geoPoint.longitudeE6
            if (maxLat == null || maxLat < lat) maxLat = lat
            if (maxLon == null || maxLon < lon) maxLon = lon
            if (minLat == null || minLat > lat) minLat = lat
            if (minLon == null || minLon > lon) minLon = lon
        }
        val tracks: List<MapTrack> = mTrackOverlay.tracks
        for (track in tracks) {
            for (point in track.track!!) {
                lat = point.latitudeE6
                lon = point.longitudeE6
                if (maxLat == null || maxLat < lat) maxLat = lat
                if (maxLon == null || maxLon < lon) maxLon = lon
                if (minLat == null || minLat > lat) minLat = lat
                if (minLon == null || minLon > lon) minLon = lon
            }
        }
        if (maxLat != null && maxLon != null && minLat != null && minLon != null) {
            val bbox = arrayOfNulls<Double>(4)
            bbox[0] = maxLat / 1E6
            bbox[1] = maxLon / 1E6
            bbox[2] = minLat / 1E6
            bbox[3] = minLon / 1E6
            return bbox
        }
        return null
    }

    fun getMinZoomLevel(): Int {
        return mMinZoomLevel
    }

    fun setMinZoomLevel(zoomLevel: Int) {
        mMinZoomLevel = zoomLevel
    }

    fun addPolygon(polygon: MapPolygon?) {
        mPolygonOverlay.addPolygon(polygon!!)
    }

    fun removePolygons() {
        mPolygonOverlay.removePolygons()
    }

    fun getPolygons(): List<MapPolygon> {
        return mPolygonOverlay.polygons
    }

    fun setCenterAndZoomOnMarkers(markers: List<MapMarker>) {
        var lat = 0
        var lon = 0
        var maxLat: Int? = null
        var maxLon: Int? = null
        var minLat: Int? = null
        var minLon: Int? = null
        for (marker in markers) {
            val geoPoint = marker.coordinate
            lat = geoPoint!!.latitudeE6
            lon = geoPoint.longitudeE6
            if (maxLat == null || maxLat < lat) maxLat = lat
            if (maxLon == null || maxLon < lon) maxLon = lon
            if (minLat == null || minLat > lat) minLat = lat
            if (minLon == null || minLon > lon) minLon = lon
        }
        if (maxLat != null && maxLon != null && minLat != null && minLon != null) {
            val bbox = arrayOfNulls<Double>(4)
            bbox[0] = maxLat / 1E6
            bbox[1] = maxLon / 1E6
            bbox[2] = minLat / 1E6
            bbox[3] = minLon / 1E6
            setZoom(bbox[0]!!, bbox[1]!!, bbox[2]!!, bbox[3]!!, 0, 0)
            setCenter(bbox[0]!!, bbox[1]!!, bbox[2]!!, bbox[3]!!)
        }
    }

    fun setCenterAndZoomOnPolygons() {
        val polygons: List<MapPolygon> = mPolygonOverlay.polygons
        var lat = 0
        var lon = 0
        var maxLatE6: Int? = null
        var maxLonE6: Int? = null
        var minLatE6: Int? = null
        var minLonE6: Int? = null
        for (polygon in polygons) {
            val points =
                polygon.polygon
            for (point in points!!) {
                lat = point.latitudeE6
                lon = point.longitudeE6
                if (maxLatE6 == null || maxLatE6 < lat) maxLatE6 = lat
                if (maxLonE6 == null || maxLonE6 < lon) maxLonE6 = lon
                if (minLatE6 == null || minLatE6 > lat) minLatE6 = lat
                if (minLonE6 == null || minLonE6 > lon) minLonE6 = lon
            }
        }
        if (maxLatE6 != null && maxLonE6 != null && minLatE6 != null && minLonE6 != null) {
            val maxLat = maxLatE6 as Double / 1E6
            val minLat = minLatE6 as Double / 1E6
            val maxLon = maxLonE6 as Double / 1E6
            val minLon = minLonE6 as Double / 1E6
            setZoom(maxLat, maxLon, minLat, minLon, 0, 0)
            setCenter(maxLat, maxLon, minLat, minLon)
        }
    }

    override fun onAnimationEnd() {
        super.onAnimationEnd()
        if (mapIntereractionListener != null) mapIntereractionListener.onMapZoomChanged(mZoomLevel)
    }

    private inner class MySimpleOnScaleGestureListener : IScaleGestureListener {
        override fun onScale(distanceF: Float) {
            val distance = distanceF.toInt()
            val zoomFactor = Math.floor(distance / 100.toDouble()).toInt()

            //Log.i("", "zoomFactor = " + zoomFactor);
            if (mZoomFactorForScaleGesture == 0) {
                mZoomFactorForScaleGesture = zoomFactor
                return
            }
            if (zoomFactor > mZoomFactorForScaleGesture) {
                animateZoomIn()
            } else if (zoomFactor < mZoomFactorForScaleGesture) {
                animateZoomOut()
            }
            mZoomFactorForScaleGesture = zoomFactor
        }

        override fun onScaleEnd() {
            mZoomFactorForScaleGesture = 0
        }
    }



    class OsmMapViewBuilder {
        var backgrounColor: Int = Color.parseColor("#FFDADBD7")
        var mapTileUnavailableBitmap: Bitmap? = null
        var isNetworkRequestAllowed = false
        var mapTypeId = 1
        var positionIndicatorDrawableId: Int = R.drawable.position_indicator
        var arrowPositionIndicatorDrawableId: Int = R.drawable.arrow_indicator

    }

    interface OnAnimateTranslationListener {
        fun onAnimateTranslationEnds()
    }

    init {
        mMapOverlays = overlays
        mMapOverlays.clear()
        setBackgroundColor(mapbuilder.backgrounColor) // Map tile background color before loading tiles
        try {
            mScaleGesture = ScaleGestureHelper(
                context,
                MySimpleOnScaleGestureListener()
            )
        } catch (e: Error) {
            // catch error for 1.6 platform that doesn't handle multitouch

        }
        setMapTileUnavailableBitmap(mapbuilder.mapTileUnavailableBitmap)
        startTileThreads(mapbuilder.isNetworkRequestAllowed)
        mTrackOverlay = OsmTrackOverlay(this)
        mPolygonOverlay = OsmPolygonOverlay(this)
        mTrackStartEndMarkerOverlay = OsmMarkerOverlay(this, null)
        mMarkerOverlay = OsmMarkerOverlay(this, null)
        mMapOverlays.add(mPolygonOverlay)
        mMapOverlays.add(mTrackOverlay)
        mMapOverlays.add(mTrackStartEndMarkerOverlay)
        mMapOverlays.add(mMarkerOverlay)
        mapIntereractionListener = mapInteractionListener
        mDetector = GestureDetector(context, this)
    }
}