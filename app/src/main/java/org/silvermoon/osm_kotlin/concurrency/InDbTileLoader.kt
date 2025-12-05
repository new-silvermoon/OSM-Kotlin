package org.silvermoon.osm_kotlin.concurrency

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.silvermoon.osm_kotlin.mapunits.Tile
import org.silvermoon.osm_kotlin.model.AppDatabase
import java.util.Collections

class InDbTileLoader(context: Context, private val mDbTileLoaderListener: IDbTileLoaderListener) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mTilesQueue = Channel<Tile>(Channel.UNLIMITED)
    private val tileDao = AppDatabase.getDatabase(context).tileDao()

    init {
        scope.launch {
            for (tile in mTilesQueue) {
                processTile(tile)
            }
        }
    }

    fun queue(tile: Tile) {
        scope.launch {
            mTilesQueue.send(tile)
        }
    }
    
    fun interrupt() {
        // scope.cancel()
    }

    private suspend fun processTile(tile: Tile) {
        val tileEntity = tileDao.getTile(tile.mapY, tile.mapX, tile.zoom)
        
        if (tileEntity != null) {
            val bitmap = BitmapFactory.decodeByteArray(tileEntity.image, 0, tileEntity.image.size)
            mDbTileLoaderListener.onTilesLoadedFromDb(mapOf(tile to bitmap))
        } else {
            mDbTileLoaderListener.onTilesNotLoadedFromDb(listOf(tile))
        }
    }

    interface IDbTileLoaderListener {
        fun onTilesLoadedFromDb(tileBitmapMap: Map<Tile, Bitmap>)
        fun onTilesNotLoadedFromDb(tiles: List<Tile?>)
    }
}
