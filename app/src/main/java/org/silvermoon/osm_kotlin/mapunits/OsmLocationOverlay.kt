package org.silvermoon.osm_kotlin.mapunits

import android.content.Context
import android.graphics.*
import android.location.Location
import android.view.View


class OsmLocationOverlay(
    c: Context,
    mapbuilder: OsmMapView.OsmMapViewBuilder,
    mapView: OsmMapView
) :
    OsmOverlay() {
    private val mLocationDot: Bitmap
    private lateinit var mArrowBitmaps: Array<Bitmap?>
    private val mOsmMapViewBuilder: OsmMapView.OsmMapViewBuilder
    private val mContext: Context
    private var mLocation: Location? = null
    private var mHeading: Int? = null
    private val mMapView: OsmMapView
    override fun draw(canvas: Canvas?, view: View?) {
        var g: GeoPoint? = null
        if (mLocation != null) {
            g = GeoPoint(
                (mLocation!!.getLatitude() * 1E6).toInt(),
                (mLocation!!.getLongitude() * 1E6).toInt()
            )
        }
        if (g != null) {
            if (mHeading != null) {
                drawBearingArrow(canvas!!, mMapView, g, mHeading!!)
            } else {
                val center: Point = mMapView.geopointToPixelProjection(g)
                canvas!!.drawBitmap(
                    mLocationDot, (center.x - mLocationDot.width / 2).toFloat(),
                    (center.y - mLocationDot.height / 2).toFloat(), null
                )
            }
        }
    }

    private fun initHeadingArrow() {
        mArrowBitmaps = arrayOfNulls(20)
        mArrowBitmaps[0] = BitmapFactory.decodeResource(
            mContext.getResources(),
            mOsmMapViewBuilder.arrowPositionIndicatorDrawableId
        )
        var i = 1
        var angle = 20
        while (angle <= 360) {
            val matrix = Matrix()
            matrix.postRotate(angle.toFloat())
            mArrowBitmaps[i] = Bitmap.createBitmap(
                mArrowBitmaps[0]!!, 0, 0, mArrowBitmaps[0]!!.width,
                mArrowBitmaps[0]!!.height, matrix, true
            )
            i++
            angle += 20
        }
    }

    fun setLocation(location: Location?) {
        mLocation = location
    }

    fun setHeading(heading: Int?) {
        mHeading = heading
    }

    private fun drawBearingArrow(
        canvas: Canvas,
        mapView: OsmMapView,
        currentLocation: GeoPoint,
        heading: Int
    ) {
        try {
            val indexArrowBitmap = heading / 20
            // translate the GeoPoint to screen pixels
            val screenPts: Point = mapView.geopointToPixelProjection(currentLocation)

            // add the rotated marker to the canvas
            canvas.drawBitmap(
                mArrowBitmaps[indexArrowBitmap]!!,
                (screenPts.x - mArrowBitmaps[indexArrowBitmap]!!.width / 2).toFloat(), (screenPts.y
                        - mArrowBitmaps[indexArrowBitmap]!!.height / 2).toFloat(), null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        mLocationDot = BitmapFactory.decodeResource(
            c.getResources(),
            mapbuilder.positionIndicatorDrawableId
        )
        mOsmMapViewBuilder = mapbuilder
        mContext = c
        mMapView = mapView
        initHeadingArrow()
    }

    init{

    }


   
}