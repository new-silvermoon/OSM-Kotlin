package org.silvermoon.osm_kotlin.concurrency

import android.os.Handler
import org.silvermoon.osm_kotlin.mapunits.Projection
import org.silvermoon.osm_kotlin.mapunits.Tile
import org.silvermoon.osm_kotlin.model.entities.MapTile


class TilesDownloader(handler: Handler?) {
    private val mRemoteTileLoader: RemoteTileLoader

    /**
     * Check if tiles are in Db, if not download them
     * @param Tiles list
     * @return number of tiles added for download
     */
    fun download(tiles: List<Tile?>): Int {
        var tileAdded = 0
        for (tile in tiles) {
            if (!MapTile.hasTile(tile!!)) {
                mRemoteTileLoader.queueTileRequest(tile)
                tileAdded++
            }
        }
        return tileAdded
    }

    companion object {
        fun getTilesForBoundaryBox(
            mapTypeId: Int, minZoom: Int, maxZoom: Int, north: Double, south: Double,
            east: Double, west: Double
        ): List<Tile> {
            val tiles: MutableList<Tile> = ArrayList<Tile>()
            for (z in minZoom..maxZoom) {
                val upperLeft: Tile = Projection.getMapTileFromCoordinates(north, west, z)
                val lowerRight: Tile = Projection.getMapTileFromCoordinates(south, east, z)
                for (x in upperLeft.mapX..lowerRight.mapX) {
                    for (y in upperLeft.mapY..lowerRight.mapY) {
                        tiles.add(Tile(x, y, z, mapTypeId))
                    }
                }
            }
            return tiles
        }

        fun getNbTilesForBoundaryBox(
            minZoom: Int, maxZoom: Int, north: Double, south: Double,
            east: Double, west: Double
        ): Int {
            var count = 0
            for (z in minZoom..maxZoom) {
                val upperLeft: Tile = Projection.getMapTileFromCoordinates(north, west, z)
                val lowerRight: Tile = Projection.getMapTileFromCoordinates(south, east, z)
                for (x in upperLeft.mapX..lowerRight.mapX) {
                    for (y in upperLeft.mapY..lowerRight.mapY) {
                        count++
                    }
                }
            }
            return count
        }
    }

    init {
        mRemoteTileLoader = RemoteTileLoader(handler, 0)
    }
}