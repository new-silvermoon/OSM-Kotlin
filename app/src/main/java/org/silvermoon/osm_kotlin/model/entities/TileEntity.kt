package org.silvermoon.osm_kotlin.model.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tiles",
    indices = [Index(value = ["row", "col", "zoom"], unique = true)]
)
data class TileEntity(
    @PrimaryKey(autoGenerate = true)
    val tilekey: Long = 0,

    @ColumnInfo(name = "row")
    val row: Int,

    @ColumnInfo(name = "col")
    val col: Int,

    @ColumnInfo(name = "zoom")
    val zoom: Int,

    @ColumnInfo(name = "image", typeAffinity = ColumnInfo.BLOB)
    val image: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TileEntity

        if (tilekey != other.tilekey) return false
        if (row != other.row) return false
        if (col != other.col) return false
        if (zoom != other.zoom) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tilekey.hashCode()
        result = 31 * result + row
        result = 31 * result + col
        result = 31 * result + zoom
        result = 31 * result + image.contentHashCode()
        return result
    }
}
