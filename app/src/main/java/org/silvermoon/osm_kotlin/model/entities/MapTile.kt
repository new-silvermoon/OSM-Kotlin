package org.silvermoon.osm_kotlin.model.entities

import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.silvermoon.osm_kotlin.mapunits.Tile
import org.silvermoon.osm_kotlin.model.OSMModel
import org.silvermoon.osm_kotlin.model.OsmDatabaseHelper
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.collections.HashMap


class MapTile : OSMModel() {

    companion object {
        const val SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        const val TABLE_TILE_NAME = "tiles"
        private val BITMAP_OPTIONS = BitmapFactory.Options()
        private val COLUMN_IMAGE = arrayOf("image")
        private var mMapMinZoomLevel = -1

        fun getTiles(tiles: List<Tile>): Map<Tile, Bitmap?> {
            val columnsSelected =
                arrayOf("row", "col", "zoom", "image")
            val tileBitmapMap: MutableMap<Tile, Bitmap?> = HashMap<Tile, Bitmap?>()
            for (t in tiles) {
                tileBitmapMap[t] = null
            }
            if (mDbHelper == null) return tileBitmapMap
            try {
                var tileSql = String()
                for (tile in tiles) {
                    if (tileSql.length > 0) tileSql += " OR "
                    tileSql += "(" + getOsmTileSQLRequest(tile.mapY, tile.mapX, tile.zoom) + ")"
                }
                val c: Cursor = mDbHelper!!.get(
                    TABLE_TILE_NAME,
                    tileSql,
                    columnsSelected,
                    null,
                    tiles.size.toString() + ""
                )
                while (c.moveToNext()) {
                    val tileBitmapByteArray = c.getBlob(c.getColumnIndex("image"))
                    val bitmapStream = ByteArrayInputStream(tileBitmapByteArray)
                    BITMAP_OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565
                    val tileBitmap =
                        BitmapFactory.decodeStream(bitmapStream, null, BITMAP_OPTIONS)
                    val row = c.getInt(c.getColumnIndex("row"))
                    val col = c.getInt(c.getColumnIndex("col"))
                    val zoom = c.getInt(c.getColumnIndex("zoom"))
                    for (t in tiles) {
                        if (t.zoom === zoom && t.mapX === col && t.mapY === row) {
                            tileBitmapMap[t] = tileBitmap
                            break
                        }
                    }
                }
                c.close()
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return tileBitmapMap
        }

        fun getTile(tile: Tile): Bitmap? {
            var tileBitmap: Bitmap? = null
            if (mDbHelper == null) return tileBitmap
            try {
                val c: Cursor = mDbHelper!!.get(
                    TABLE_TILE_NAME,
                    getOsmTileSQLRequest(tile.mapY, tile.mapX, tile.zoom),
                    COLUMN_IMAGE, null, "1"
                )
                if (c.moveToNext()) {
                    val tileBitmapByteArray = c.getBlob(c.getColumnIndex("image"))
                    val bitmapStream = ByteArrayInputStream(tileBitmapByteArray)
                    BITMAP_OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565
                    tileBitmap = BitmapFactory.decodeStream(bitmapStream, null, BITMAP_OPTIONS)
                }
                c.close()
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //Log.i("getTile", "get tile from DB    tile = " + tile.mapX+tile.mapY+tile.zoom );
            return tileBitmap
        }

        val minZoomLevel: Int
            get() {
                if (mMapMinZoomLevel > -1) return mMapMinZoomLevel
                val osmDb: OsmDatabaseHelper = mDbHelper!!
                val sql = "select value from preferences where name ='map.minZoom' "
                val c = osmDb.query(sql)
                if (c!!.moveToNext()) {
                    try {
                        mMapMinZoomLevel = c.getString(0).toInt()
                    } catch (e: OutOfMemoryError) {
                        e.printStackTrace()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                c.close()
                return mMapMinZoomLevel
            }

        fun hasTile(tile: Tile): Boolean {
            return if (getTileId(tile) != null) true else false
        }

        fun getTileId(tile: Tile): String? {
            var tileId: String? = null
            val columnsSelected = arrayOf("tilekey")
            val osmDb: OsmDatabaseHelper = mDbHelper ?: return tileId
            val c = osmDb[TABLE_TILE_NAME, getOsmTileSQLRequest(
                tile.mapY,
                tile.mapX,
                tile.zoom
            ), columnsSelected, null, "1"]
            if (c.moveToNext()) {
                tileId = c.getString(c.getColumnIndex("tilekey"))
            }
            c.close()
            return tileId
        }

        fun insertTile(tile: Tile, bitmapData: ByteArray?) {

            //long time = Calendar.getInstance().getTimeInMillis();
            try {
                var tileAlreadyInDb = false
                val osmDb: OsmDatabaseHelper = mDbHelper ?: return
                val columnsSelected = arrayOf("tilekey")
                val c = osmDb[TABLE_TILE_NAME, getOsmTileSQLRequest(
                    tile.mapY,
                    tile.mapX,
                    tile.zoom
                ), columnsSelected, null, "1"]
                if (c.count > 0) {
                    //Log.i("getTile", "x= " +tile.mapX+ " y=" +tile.mapY+ " zoom=" + tile.zoom + " mapTypeId=" + tile.mapTypeId + " ALREADY IN DB!!");
                    tileAlreadyInDb = true
                }
                if (!tileAlreadyInDb) {
                    // INSERT
                    val tileValues = ContentValues()
                    tileValues.put("row", tile.mapY) // OSM format: row -> Y
                    tileValues.put("col", tile.mapX) // OSM format: col -> X
                    tileValues.put("zoom", tile.zoom)
                    //tileValues.put("mapTypeId", tile.mapTypeId);
                    tileValues.put("image", bitmapData)
                    //tileValues.put("creationDate", DateUtil.longToSqlDateFormat(
                    //		Calendar.getInstance().getTimeInMillis()));
                    osmDb.insert(TABLE_TILE_NAME, tileValues)
                    //Log.i("getTile", "x= " +tile.mapX+ " y=" +tile.mapY+ " zoom=" + tile.zoom + " mapTypeId=" + tile.mapTypeId + " INSERTED!!");
                }

                //Log.i("insertTile", "INSERTED time=" + (Calendar.getInstance().getTimeInMillis() - time) + "ms");
                c.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getOsmTileSQLRequest(row: Int, col: Int, zoom: Int): String {
            return "row=$row AND col=$col AND zoom=$zoom"
        }

        fun getTileFromCursor(c: Cursor): Tile {
            val row = c.getInt(c.getColumnIndex("row"))
            val col = c.getInt(c.getColumnIndex("col"))
            val zoom = c.getInt(c.getColumnIndex("zoom"))
            //int mapTypeId = c.getInt(c.getColumnIndex("mapTypeId"));
            return Tile(col, row, zoom)
        }

        /**
         * Delete tiles above a certain limit (first in first out)
         * @param limit in Mb
         */
        fun deleteTilesAboveLimitThread(limit: Int) {
            val limitKb = limit * 1024 //put limit in Kb
            val t: Thread = object : Thread() {
                override fun run() {
                    val time: Long = Calendar.getInstance().getTimeInMillis()
                    val db: OsmDatabaseHelper = mDbHelper ?: return
                    val sql = "SELECT tilekey FROM " + TABLE_TILE_NAME +
                            " WHERE tilekey NOT IN " +
                            "(SELECT tilekey FROM " + MapTileEntity.TABLE_TILE_ENTITY_NAME + " GROUP BY tilekey) "
                    //"ORDER BY creationDate ASC;"; REMOVED from this version due to change in schema
                    val c = db.query(sql) ?: return
                    val tilesSizeKb: Int = Tile.AVERAGE_TILE_SIZE * c.count
                    if (tilesSizeKb > limitKb) {
                        var nbFilesToDeleted: Int = (tilesSizeKb - limitKb) / Tile.AVERAGE_TILE_SIZE
                        db.mDb!!.beginTransaction()
                        try {
                            while (c.moveToNext()) {
                                db.delete(
                                    TABLE_TILE_NAME,
                                    "tilekey=" + c.getInt(c.getColumnIndex("tilekey"))
                                )
                                if (nbFilesToDeleted <= 0) break
                                nbFilesToDeleted--
                            }
                            db.mDb!!.setTransactionSuccessful()
                        } finally {
                            db.mDb!!.endTransaction()
                        }
                    }
                    c.close()
                    Log.i(
                        "deleteTilesAbvLmtThread",
                        "deleteTilesAboveLimitThread time =" + (Calendar.getInstance()
                            .getTimeInMillis() - time).toString() + "ms"
                    )
                }
            }
            t.start()
        }

        fun deleteTilesWithNoEntity() {
            val sql = "DELETE FROM " + TABLE_TILE_NAME + " " +
                    "WHERE tilekey NOT IN (SELECT tilekey FROM " + MapTileEntity.TABLE_TILE_ENTITY_NAME + " GROUP BY tilekey);"
            val osmDb: OsmDatabaseHelper = mDbHelper ?: return
            osmDb.mDb!!.execSQL(sql)
        }

        fun getMinMaxZoomLevelForTiles(tiles: List<Tile>): IntArray {
            val minMaxZoom = IntArray(2)
            minMaxZoom[0] = 18
            minMaxZoom[1] = 0
            for (tile in tiles) {
                if (tile.zoom < minMaxZoom[0]) minMaxZoom[0] = tile.zoom
                if (tile.zoom > minMaxZoom[1]);
                minMaxZoom[1] = tile.zoom
            }
            return minMaxZoom
        }
    }
}