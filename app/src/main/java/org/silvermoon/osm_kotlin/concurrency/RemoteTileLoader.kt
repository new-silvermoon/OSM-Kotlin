package org.silvermoon.osm_kotlin.concurrency

import android.content.Context
import android.os.Handler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.silvermoon.osm_kotlin.mapunits.Tile
import org.silvermoon.osm_kotlin.model.AppDatabase
import org.silvermoon.osm_kotlin.model.entities.TileEntity

class RemoteTileLoader(context: Context, private val mHandler: Handler, tileStackSizeLimit: Int) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mRequestsQueue = Channel<Tile>(tileStackSizeLimit)
    private val mRequestTile = RequestTile()
    private val tileDao = AppDatabase.getDatabase(context).tileDao()

    init {
        scope.launch {
            for (tile in mRequestsQueue) {
                processTile(tile)
            }
        }
    }
    
    fun queueTileRequest(tile: Tile) {
        scope.launch {
            mRequestsQueue.send(tile)
        }
    }

    private suspend fun processTile(tile: Tile) {
        val loadTileSuccess = loadTile(tile)

        val message = mHandler.obtainMessage()
        message.arg1 = 0 
        message.arg2 = 0

        if (loadTileSuccess)
            message.what = TileHandler.TILE_LOADED
        else
            message.what = TileHandler.TILE_NOT_LOADED
        mHandler.sendMessage(message)
    }

    private suspend fun loadTile(tile: Tile?): Boolean {
        if (tile == null || tile.key == null) {
            return false
        }
        try {
            val bitmapData = mRequestTile.loadBitmap(tile)
            if (bitmapData == null || bitmapData.isEmpty())
                return false

            addTile(tile, bitmapData)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }


    suspend fun addTile(tile: Tile?, bitmapData: ByteArray?) {
        if (tile == null || bitmapData == null || bitmapData.isEmpty()) {
            return
        }
        val entity = TileEntity(
            row = tile.mapY,
            col = tile.mapX,
            zoom = tile.zoom,
            image = bitmapData
        )
        tileDao.insertTile(entity)
    }
}
