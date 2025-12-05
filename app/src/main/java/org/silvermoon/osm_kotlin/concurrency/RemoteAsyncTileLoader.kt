package org.silvermoon.osm_kotlin.concurrency

import android.content.Context
import android.os.Handler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.silvermoon.osm_kotlin.mapunits.OsmMapViewBase
import org.silvermoon.osm_kotlin.mapunits.Tile
import org.silvermoon.osm_kotlin.model.AppDatabase
import org.silvermoon.osm_kotlin.model.entities.TileEntity

class RemoteAsyncTileLoader(context: Context, private val mHandler: Handler) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mRequestsQueue = Channel<Tile>(Channel.UNLIMITED)
    private val mRequestTile = RequestTile()
    private val tileDao = AppDatabase.getDatabase(context).tileDao()

    init {
        for (i in 0 until NB_THREAD_FOR_TILE_REQUEST) {
            scope.launch {
                for (tile in mRequestsQueue) {
                    loadTileAndSendMessage(tile)
                }
            }
        }
    }

    fun queueTileRequest(tile: Tile) {
        // Make sure the zoom is not > to the actual max Tile OSM zoom
        if (tile.zoom > OsmMapViewBase.MIN_ZOOM_LEVEL_FOR_TILES)
            return

        scope.launch {
            mRequestsQueue.send(tile)
        }
    }

    fun interruptThreads() {
        // scope.cancel()
    }

    private suspend fun loadTileAndSendMessage(remoteTile: Tile?) {

        if (remoteTile != null && remoteTile.key != null) {
            try {
                remoteTile.bitmap = mRequestTile.loadBitmap(remoteTile)

                val message = mHandler.obtainMessage()

                if (remoteTile.bitmap != null && remoteTile.bitmap!!.isNotEmpty()) {
                    val entity = TileEntity(
                        row = remoteTile.mapY,
                        col = remoteTile.mapX,
                        zoom = remoteTile.zoom,
                        image = remoteTile.bitmap!!
                    )
                    tileDao.insertTile(entity)
                    message.what = TileHandler.TILE_LOADED
                } else {
                    message.what = TileHandler.TILE_NOT_LOADED
                }

                mHandler.sendMessage(message)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val NB_THREAD_FOR_TILE_REQUEST = 3
    }
}
