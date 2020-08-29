package org.silvermoon.osm_kotlin.model.entities

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import org.silvermoon.osm_kotlin.mapunits.Tile
import org.silvermoon.osm_kotlin.model.OSMModel
import org.silvermoon.osm_kotlin.model.OsmDatabaseHelper



object MapTileEntity : OSMModel() {
    const val TABLE_TILE_ENTITY_NAME = "mapTilesEntities"
    fun insertTilesForEntity(tiles: List<Tile?>, entityId: Int) {
        val db: OsmDatabaseHelper = mDbHelper!!
        val values: MutableList<ContentValues> = ArrayList()
        db.mDb!!.beginTransaction()
        try {
            for (tile in tiles) {
                val tileId = MapTile.getTileId(tile!!)
                if (tileId != null) {
                    val contentValue = ContentValues()
                    contentValue.put("tilekey", tileId)
                    contentValue.put("entityId", entityId)
                    values.add(contentValue)
                }
            }
            for (value in values) {
                db.insert(TABLE_TILE_ENTITY_NAME, value)
            }
            db.mDb!!.setTransactionSuccessful()
        } finally {
            db.mDb!!.endTransaction()
        }
    }

    fun getTilesForEntity(entityId: Int): List<Tile> {
        val db: OsmDatabaseHelper = mDbHelper!!
        val tiles: MutableList<Tile> = ArrayList<Tile>()
        val mapTileTable = MapTile.TABLE_TILE_NAME
        val sql =
            "SELECT row, col, zoom FROM " + mapTileTable + ", " + TABLE_TILE_ENTITY_NAME +
                    " WHERE " + mapTileTable + ".tilekey=" + TABLE_TILE_ENTITY_NAME + ".tilekey" +
                    " AND " + TABLE_TILE_ENTITY_NAME + ".entityId=" + entityId + ";"
        val c: Cursor? = db.query(sql)
        Log.i("request", sql + "       count=" + c!!.getCount())
        while (c!!.moveToNext()) {
            tiles.add(MapTile.getTileFromCursor(c))
        }
        c.close()
        return tiles
    }

    fun deleteByEntityId(entityId: Int): Int {
        return mDbHelper!!.delete(TABLE_TILE_ENTITY_NAME, "entityId=$entityId")
    }
}