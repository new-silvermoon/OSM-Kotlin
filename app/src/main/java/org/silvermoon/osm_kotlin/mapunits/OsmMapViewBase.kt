package org.silvermoon.osm_kotlin.mapunits

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.GestureDetector.OnDoubleTapListener
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import org.silvermoon.osm_kotlin.concurrency.TileHandler
import org.silvermoon.osm_kotlin.concurrency.TilesProvider
import java.util.Vector
import kotlin.math.ceil
import kotlin.math.pow

abstract class OsmMapViewBase(context: Context, mapTypeId: Int) : SurfaceView(context),
    GestureDetector.OnGestureListener, OnDoubleTapListener {

    protected var mZoomLevel: Int = 2
    protected var mPendingZoomLevel: Int = 2

    private var mOffsetX = 0
    private var mOffsetY = 0
    private var mTouchDownX = 0
    private var mTouchDownY = 0
    private var mTouchOffsetX = 0
    private var mTouchOffsetY = 0
    private var mTiles: Array<Tile?>? = null

    private var mIncrementsX: IntArray? = null
    private var mIncrementsY: IntArray? = null
    private val mZoomInAnimation: Animation
    private val mZoomOutAnimation: Animation
    private var mZoomInDoubleTapAnimation: Animation? = null
    private var mTilesProvider: TilesProvider? = null
    val overlays: MutableList<OsmOverlay> = ArrayList()
    private val mMaptTypeId: Int = mapTypeId
    private val mHandler: TileHandler
    private var setMapCenterWhenViewSizeChange: GeoPoint? = null
    protected var mIsDoubleTap: Boolean = false
    private var mMapTileUnavailableBitmap: Bitmap? = null

    init {
        holder.setFormat(PixelFormat.RGB_565)

        mZoomInAnimation = ScaleAnimation(
            1.0f, 2f, 1.0f, 2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        mZoomInAnimation.duration = 400L

        mZoomOutAnimation = ScaleAnimation(
            1f, 0.5f, 1f, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        mZoomOutAnimation.duration = 400L

        mHandler = TileHandler(this)
    }

    fun animateZoomIn(): Boolean {
        if (mPendingZoomLevel >= MAX_ZOOM_LEVEL) {
            return false
        }

        if (mIsDoubleTap) {
            val pivotX = this.mTouchDownX * 1.0 / width
            val pivotY = this.mTouchDownY * 1.0 / height
            mZoomInDoubleTapAnimation = ScaleAnimation(
                1f, 2f, 1f, 2f,
                Animation.RELATIVE_TO_SELF, pivotX.toFloat(),
                Animation.RELATIVE_TO_SELF, pivotY.toFloat()
            )
            mZoomInDoubleTapAnimation?.duration = 400L
        }

        if (this.mPendingZoomLevel == this.mZoomLevel) {
            this.mPendingZoomLevel++
            if (mIsDoubleTap) {
                mZoomInDoubleTapAnimation?.let { startAnimation(it) }
            } else {
                startAnimation(mZoomInAnimation)
            }

            val tiles: Array<Tile?> = if (mIsDoubleTap) {
                initializePendingTiles(
                    mZoomLevel + 1,
                    (offsetX) * 2 - this.mTouchDownX,
                    (offsetY) * 2 - this.mTouchDownY
                )
            } else {
                initializePendingTiles(
                    mZoomLevel + 1,
                    (offsetX) * 2 - (width / 2),
                    (offsetY) * 2 - (height / 2)
                )
            }

            for (i in (mTiles?.indices?.reversed() ?: IntRange.EMPTY)) {
                tiles[i]?.let { mTilesProvider?.getTileBitmap(it) }
            }
        }
        return true
    }

    fun animateZoomOut(): Boolean {
        if (mPendingZoomLevel <= mMinZoomLevel) {
            return false
        }

        if (mPendingZoomLevel == this.mZoomLevel) {
            this.mPendingZoomLevel--
            startAnimation(mZoomOutAnimation)
        }
        return true
    }

    val center: GeoPoint
        get() {
            val offsetX = mOffsetX - (width / 2)
            val offsetY = mOffsetY - (height / 2)
            return getProjectionFromPixels(offsetX, offsetY)
        }

    fun getProjectionFromPixels(x: Int, y: Int): GeoPoint {
        return Projection.getProjectionFromPixels(x, y, mZoomLevel)
    }

    fun setCenter(lat: Double, lon: Double, mapWidth: Int, mapHeight: Int) {
        val x = Projection.getXPixelFromLongitude(lon, mZoomLevel)
        val y = Projection.getYPixelFromLatitude(lat, mZoomLevel)

        val offsetX = (0 - x) + (mapWidth / 2)
        val offsetY = (0 - y) + (mapHeight / 2)

        this.offsetX = offsetX
        this.offsetY = offsetY

        invalidate()
    }

    fun setCenter(lat: Double, lon: Double) {
        val screenWidth = width
        val screenHeight = height

        if (screenHeight == 0 && screenWidth == 0)
            setMapCenterWhenViewSizeChange = GeoPoint((lat * 1E6).toInt(), (lon * 1E6).toInt())

        val x = Projection.getXPixelFromLongitude(lon, mZoomLevel)
        val y = Projection.getYPixelFromLatitude(lat, mZoomLevel)

        val offsetX = (0 - x) + (screenWidth / 2)
        val offsetY = (0 - y) + (screenHeight / 2)

        this.offsetX = offsetX
        this.offsetY = offsetY

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // On view change we have enough room for all the tiles
        if (width > 0 && height > 0)
            initMapTileArrayCoordinate()
        //---

        setMapCenterWhenViewSizeChange?.let {
            val lat = it.latitudeE6 / 1E6
            val lon = it.longitudeE6 / 1E6
            setCenter(lat, lon)
            setMapCenterWhenViewSizeChange = null
        }
    }

    fun setMapTileUnavailableBitmap(bitmap: Bitmap?) {
        mMapTileUnavailableBitmap = bitmap
    }

    private fun initMapTileArrayCoordinate() {
        val xRows = ceil((width + Tile.TILE_SIZE).toDouble() / Tile.TILE_SIZE.toDouble()).toInt()
        val yRows = ceil((height + Tile.TILE_SIZE).toDouble() / Tile.TILE_SIZE.toDouble()).toInt()

        mTilesProvider?.setResizeBitmapCacheSize(xRows * yRows * 2)
        mTilesProvider?.setBitmapMemoryCacheSize(xRows * yRows * 2)
        mTilesProvider?.setMapTileUnavailableBitmap(mMapTileUnavailableBitmap)

        mIncrementsX = IntArray(xRows * yRows)
        mIncrementsY = IntArray(xRows * yRows)

        val oldTiles = mTiles
        mTiles = arrayOfNulls(xRows * yRows)
        if (oldTiles != null && oldTiles.isNotEmpty()) {
            var i = 0
            while (i < oldTiles.size && i < mTiles!!.size) {
                mTiles!![i] = oldTiles[i]
                i++
            }
        }

        var pos = 0
        for (x in 0 until yRows) {
            for (i in 0 until xRows) {
                mIncrementsX!![pos++] = i
            }
        }

        pos = 0
        for (y in 0 until yRows) {
            for (i in 0 until xRows) {
                mIncrementsY!![pos++] = y
            }
        }
    }

    fun clear() {
        try {
            mTilesProvider?.clearCache()
            mTilesProvider?.clearResizeCache()
            mTilesProvider?.stopThreads()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startTileThreads(allowTileRequestViaHttp: Boolean) {
        mTilesProvider = TilesProvider(this.context, mHandler, allowTileRequestViaHttp)
    }

    private val maxOffsetX: Int
        get() = (0 - (2.0.pow(mZoomLevel.toDouble())) * TILE_SIZE).toInt()

    var offsetX: Int
        get() = mOffsetX
        set(offsetX) {
            if (mPendingZoomLevel == this.mZoomLevel) {
                if ((this.width != 0) && ((offsetX + 255) > this.width)) {
                    this.mOffsetX = this.width - 255
                } else if ((offsetX - 255) < maxOffsetX) {
                    this.mOffsetX = maxOffsetX + 255
                } else {
                    this.mOffsetX = offsetX
                }
            }
        }

    var offsetY: Int
        get() = mOffsetY
        set(offsetY) {
            if (mPendingZoomLevel == this.mZoomLevel) {
                if ((this.height != 0) && ((offsetY + 255) > this.height)) {
                    this.mOffsetY = this.height - 255
                } else if ((offsetY - 255) < maxOffsetX) {
                    this.mOffsetY = maxOffsetX + 255
                } else {
                    this.mOffsetY = offsetY
                }
                initializeCurrentTiles(mZoomLevel, this.mOffsetX, this.mOffsetY)
            }
        }

    private fun initializeCurrentTiles(zoomLevel: Int, offsetX: Int, offsetY: Int): Array<Tile?>? {
        val mapX = (0 - offsetX) / TILE_SIZE
        val mapY = (0 - offsetY) / TILE_SIZE

        if (mTiles != null) {
            for (index in mTiles!!.indices) {
                if (mTiles!![index] == null) {
                    mTiles!![index] = Tile()
                }

                // try to save on string relocations
                if (mTiles!![index]!!.mapX != (mapX + mIncrementsX!![index])
                    || mTiles!![index]!!.mapY != (mapY + mIncrementsY!![index])
                    || mTiles!![index]!!.zoom != zoomLevel
                ) {
                    mTiles!![index]!!.mapX = mapX + mIncrementsX!![index]
                    mTiles!![index]!!.mapY = mapY + mIncrementsY!![index]
                    mTiles!![index]!!.offsetX = mTiles!![index]!!.mapX * TILE_SIZE
                    mTiles!![index]!!.offsetY = mTiles!![index]!!.mapY * TILE_SIZE
                    mTiles!![index]!!.zoom = zoomLevel
                    mTiles!![index]!!.key =
                        (zoomLevel.toString() + "/" + mTiles!![index]!!.mapX + "/"
                                + mTiles!![index]!!.mapY + ".png").intern()
                    mTiles!![index]!!.mapTypeId = mMaptTypeId
                }
            }
        }
        return mTiles
    }

    private fun initializePendingTiles(zoomLevel: Int, offsetX: Int, offsetY: Int): Array<Tile?> {
        val mapX = (0 - offsetX) / TILE_SIZE
        val mapY = (0 - offsetY) / TILE_SIZE

        val tiles = arrayOfNulls<Tile>(mTiles!!.size)

        if (mTiles != null) {
            for (index in mTiles!!.indices) {
                tiles[index] = Tile()
                tiles[index]!!.mapX = mapX + mIncrementsX!![index]
                tiles[index]!!.mapY = mapY + mIncrementsY!![index]
                tiles[index]!!.offsetX = mTiles!![index]!!.mapX * TILE_SIZE
                tiles[index]!!.offsetY = mTiles!![index]!!.mapY * TILE_SIZE
                tiles[index]!!.zoom = zoomLevel
                tiles[index]!!.key = (zoomLevel.toString() + "/" + tiles[index]!!.mapX + "/"
                        + tiles[index]!!.mapY + ".png").intern()
                tiles[index]!!.mapTypeId = mMaptTypeId
            }
        }

        return tiles
    }

    private fun isOnScreen(tile: Tile?): Boolean {
        if (tile == null) {
            return false
        }

        val upperLeftX = tile.offsetX + this.mOffsetX
        val upperLeftY = tile.offsetY + this.mOffsetY
        val width = this.width
        val height = this.height

        if (((upperLeftX + TILE_SIZE) >= 0) && (upperLeftX < width)
            && ((upperLeftY + TILE_SIZE) >= 0) && (upperLeftY < height)
        ) {
            return isSane(tile)
        }
        return false
    }

    private fun isSane(tile: Tile): Boolean {
        return tile.mapX >= 0 && tile.mapY >= 0
                && tile.mapX <= (2.0.pow(mZoomLevel.toDouble()) - 1)
                && tile.mapY <= (2.0.pow(mZoomLevel.toDouble()) - 1)
    }

    override fun onAnimationEnd() {
        if (this.mZoomLevel > mPendingZoomLevel) {
            this.mZoomLevel = mPendingZoomLevel
            zoomOut()
        } else if (this.mZoomLevel < mPendingZoomLevel) {
            this.mZoomLevel = mPendingZoomLevel
            if (mIsDoubleTap)
                zoomInForDoubleTap()
            else
                zoomIn()
        }

        mTouchOffsetX = offsetX
        mTouchOffsetY = offsetY

        invalidate()

        super.onAnimationEnd()
    }

    private fun onZoomLevelChanges() {
        for (overlay in overlays) {
            overlay.onZoomLevelChanges(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mTiles != null) {
            for (i in mTiles!!.indices.reversed()) {
                if (isOnScreen(mTiles!![i])) {

                    val bitmap = mTilesProvider?.getTileBitmap(mTiles!![i]!!)

                    if (bitmap != null) {
                        canvas.drawBitmap(
                            bitmap,
                            (mOffsetX + mTiles!![i]!!.offsetX).toFloat(),
                            (mOffsetY + mTiles!![i]!!.offsetY).toFloat(),
                            null
                        )
                    }
                }
            }
        }

        for (overlay in overlays) {
            overlay.draw(canvas, this)
        }
    }

    fun setZoom(zoomLevel: Int): Int {
        var newZoomLevel = zoomLevel
        if (newZoomLevel > MAX_ZOOM_LEVEL)
            newZoomLevel = MAX_ZOOM_LEVEL

        if (newZoomLevel < mMinZoomLevel)
            newZoomLevel = mMinZoomLevel

        this.mZoomLevel = newZoomLevel
        this.mPendingZoomLevel = newZoomLevel

        onZoomLevelChanges()

        return this.mZoomLevel
    }

    private fun zoomInForDoubleTap() {
        offsetX = (offsetX) * 2 - this.mTouchDownX
        offsetY = (offsetY) * 2 - this.mTouchDownY
        //		mTilesProvider.clearResizeCache();
        mIsDoubleTap = false

        onZoomLevelChanges()
    }

    fun zoomIn() {
        offsetX = (offsetX) * 2 - (width / 2)
        offsetY = (offsetY) * 2 - (height / 2)
        //		mTilesProvider.clearResizeCache();

        onZoomLevelChanges()
    }

    fun zoomOut() {
        offsetX = offsetX / 2 + (width / 4)
        offsetY = offsetY / 2 + (height / 4)
        //		mTilesProvider.clearResizeCache();

        onZoomLevelChanges()
    }

    class Tiles : Vector<Tile>() {
        companion object {
            private const val serialVersionUID = -6468659912600523042L
        }
    }

    override fun onDown(event: MotionEvent): Boolean {
        //ACTION_DOWN
        this.mTouchDownX = event.x.toInt()
        this.mTouchDownY = event.y.toInt()
        this.mTouchOffsetX = this.mOffsetX
        this.mTouchOffsetY = this.mOffsetY
        return true
    }

    override fun onFling(
        downEvent: MotionEvent?,
        event: MotionEvent,
        distanceXf: Float,
        distanceYf: Float
    ): Boolean {
        //ACTION_UP
        offsetX = this.mTouchOffsetX + event.x.toInt() - this.mTouchDownX
        offsetY = this.mTouchOffsetY + event.y.toInt() - this.mTouchDownY
        return true
    }

    override fun onLongPress(arg0: MotionEvent) {
    }

    override fun onScroll(
        downEvent: MotionEvent?,
        currentEvent: MotionEvent,
        arg2: Float,
        arg3: Float
    ): Boolean {
        // ACTION_MOVE Equivalent
        offsetX = this.mTouchOffsetX + currentEvent.x.toInt() - this.mTouchDownX
        offsetY = this.mTouchOffsetY + currentEvent.y.toInt() - this.mTouchDownY
        invalidate()
        return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {

        for (overlay in overlays) {
            if (overlay.onInterceptSingleTap(event, this)) {
                overlay.onSingleTap(event, this)
                invalidate()
                return true
            }
        }
        for (overlay in overlays) {
            if (overlay.onSingleTap(event, this)) {
                invalidate()
                return true
            }
        }

        return false
    }

    override fun onShowPress(event: MotionEvent) {
    }

    companion object {

        const val MAX_ZOOM_LEVEL = 19
        const val MIN_ZOOM_LEVEL_FOR_TILES = 18

        private const val TILE_SIZE = Tile.TILE_SIZE

        protected var mMinZoomLevel = 0
    }

}
