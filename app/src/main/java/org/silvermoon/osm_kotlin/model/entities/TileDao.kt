package org.silvermoon.osm_kotlin.model.entities

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TileDao {
    @Query("SELECT * FROM tiles WHERE row = :row AND col = :col AND zoom = :zoom LIMIT 1")
    suspend fun getTile(row: Int, col: Int, zoom: Int): TileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: TileEntity)

    @Query("DELETE FROM tiles")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM tiles")
    suspend fun getAllTiles(): List<TileEntity>
}
