package org.silvermoon.osm_kotlin.concurrency

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import org.silvermoon.osm_kotlin.cache.InMemoryTilesCache
import org.silvermoon.osm_kotlin.cache.ResizedTilesCache

import org.silvermoon.osm_kotlin.concurrency.InDbTileLoader.IDbTileLoaderListener
import org.silvermoon.osm_kotlin.mapunits.Tile


class TilesProvider(
    context: Context?,
    handler: Handler,
    allowRequestTilesViaInternet: Boolean
) :
    IDbTileLoaderListener {
    private var mInMemoryTilesCache: InMemoryTilesCache? = null
    private var mResizedTilesCache: ResizedTilesCache? = null
    private var mRemoteTileLoader: RemoteAsyncTileLoader? = null
    private val mInDbTileLoader: InDbTileLoader
    private val mHandler: Handler
    private val mAllowRequestTilesViaInternet: Boolean
    private var mMapTileUnavailableBitmap: Bitmap? = null
    fun setResizeBitmapCacheSize(size: Int) {
        mResizedTilesCache!!.setBitmapCacheSize(size)
    }

    fun setBitmapMemoryCacheSize(size: Int) {
        mInMemoryTilesCache!!.setBitmapCacheSize(size)
    }

    fun setMapTileUnavailableBitmap(bitmap: Bitmap?) {
        mMapTileUnavailableBitmap = bitmap
        mResizedTilesCache!!.setMapTileUnavailableBitmap(mMapTileUnavailableBitmap)
    }

    fun getTileBitmap(tile: Tile): Bitmap? {
        if (mInMemoryTilesCache!!.hasTile(tile.key)) {
            return mInMemoryTilesCache!!.getTileBitmap(tile.key)
        }
        if (mAllowRequestTilesViaInternet) {
            mInDbTileLoader.queue(Tile(tile))
            if (mResizedTilesCache!!.hasTile(tile)) {
                return mResizedTilesCache!!.getTileBitmap(tile)
            }
        } else {
            if (mResizedTilesCache!!.hasTile(tile)) {
                return mResizedTilesCache!!.getTileBitmap(tile)
            }
            mInDbTileLoader.queue(Tile(tile))
        }
        return null
    }

    override fun onTilesLoadedFromDb(tileBitmapMap: Map<Tile, Bitmap>) {
        for ((tile, bitmap) in tileBitmapMap) {
            mInMemoryTilesCache!!.add(tile.key, bitmap)
        }
        val message: Message = mHandler.obtainMessage()
        message.what = TileHandler.TILE_LOADED
        mHandler.sendMessage(message)
    }

    override fun onTilesNotLoadedFromDb(tiles: List<Tile?>) {
        for (tile in tiles) {
            val tileMinusOneZoomLevel: Tile = mResizedTilesCache!!.findClosestMinusTile(tile)
            if (tileMinusOneZoomLevel != null) {
                mResizedTilesCache!!.queueResize(Tile(tile!!))
            }
            if (mAllowRequestTilesViaInternet) mRemoteTileLoader!!.queueTileRequest(Tile(tile!!))
        }
    }

    fun stopThreads() {
        mRemoteTileLoader!!.interruptThreads()
        mResizedTilesCache!!.interrupt()
        mInDbTileLoader.interrupt()
    }

    fun clearCache() {
        mInMemoryTilesCache!!.clean()
    }

    fun clearResizeCache() {
        mResizedTilesCache!!.clear()
    }

    init {
        mInMemoryTilesCache = InMemoryTilesCache(context, handler)
        mRemoteTileLoader = RemoteAsyncTileLoader(handler)
        mResizedTilesCache = ResizedTilesCache(handler)
        mInDbTileLoader = InDbTileLoader(this)
        mHandler = handler
        mAllowRequestTilesViaInternet = allowRequestTilesViaInternet
    }
}