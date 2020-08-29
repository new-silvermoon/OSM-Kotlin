package org.silvermoon.osm_kotlin.model.entities

import android.content.ContentValues
import android.database.Cursor
import org.silvermoon.osm_kotlin.model.OSMModel


import org.silvermoon.osm_kotlin.util.DateUtil
import java.util.*
import kotlin.collections.ArrayList


class MapEntity : OSMModel() {
    var c_entityId = 0
    var c_associatedEntityId = 0
    var c_associatedEntityType: String? = null
    var c_title: String? = null
    var c_maxLat = 0.0
    var c_maxLon = 0.0
    var c_minLat = 0.0
    var c_minLon = 0.0
    var c_creationDate: Calendar? = null

    companion object {
        const val ENTITY_TYPE_REGION = "REGION"
        const val ENTITY_TYPE_TRIP = "TRIP"
        private const val TABLE_ENTITY_NAME = "mapEntities"
        fun getById(entityId: Int): MapEntity? {
            var mapEntity: MapEntity? = null
            val c: Cursor = mDbHelper!!.get(
                TABLE_ENTITY_NAME, "entityId=$entityId",
                null, null, "1"
            )
            while (c.moveToNext()) {
                mapEntity = getEntityFromCursor(c)
                break
            }
            c.close()
            return mapEntity
        }

        fun deleteById(entityId: Int): Int {
            return mDbHelper!!.delete(TABLE_ENTITY_NAME, "entityId=$entityId")
        }

        fun hasEntity(
            associatedEntityId: Int,
            associatedEntityType: String
        ): Boolean {
            val columnsSelected = arrayOf("entityId")
            val c: Cursor = mDbHelper!!.get(
                TABLE_ENTITY_NAME, "associatedEntityId=" + associatedEntityId
                        + " AND associatedEntityType LIKE '" + associatedEntityType + "'",
                columnsSelected, null, null
            )
            if (c.getCount() > 0) {
                c.close()
                return true
            }
            c.close()
            return false
        }

        fun insertEntity(
            associatedEntityId: Int,
            associatedEntityType: String,
            associatedEntityTitle: String?,
            maxLat: Double,
            minLat: Double,
            maxLon: Double,
            minLon: Double
        ): Int {
            var hasEntity = false
            if (associatedEntityId != 0) hasEntity =
                hasEntity(associatedEntityId, associatedEntityType)
            var id: Long = 0
            if (!hasEntity) {
                //INSERT
                val tileValues = ContentValues()
                tileValues.put("associatedEntityId", associatedEntityId)
                tileValues.put("associatedEntityType", associatedEntityType)
                tileValues.put("title", associatedEntityTitle)
                tileValues.put("maxLat", maxLat)
                tileValues.put("maxLon", maxLon)
                tileValues.put("minLat", minLat)
                tileValues.put("minLon", minLon)
                tileValues.put(
                    "creationDate", DateUtil.longToSqlDateFormat(
                        Calendar.getInstance().getTimeInMillis()
                    )
                )
                id = mDbHelper!!.insert(TABLE_ENTITY_NAME, tileValues)
            }
            return id.toInt()
        }

        val entities: List<MapEntity>
            get() {
                val mapEntities: MutableList<MapEntity> = ArrayList()
                val c: Cursor = mDbHelper!!.get(
                    TABLE_ENTITY_NAME,
                    null,
                    null,
                    "creationDate DESC",
                    null
                )
                while (c.moveToNext()) {
                    val mapEntity = getEntityFromCursor(c)
                    mapEntities.add(mapEntity)
                }
                c.close()
                return mapEntities
            }

        fun getEntityFromCursor(c: Cursor): MapEntity {
            val mapEntity = MapEntity()
            mapEntity.c_entityId = c.getInt(c.getColumnIndex("entityId"))
            mapEntity.c_associatedEntityId = c.getInt(c.getColumnIndex("associatedEntityId"))
            mapEntity.c_associatedEntityType = c.getString(c.getColumnIndex("associatedEntityType"))
            mapEntity.c_title = c.getString(c.getColumnIndex("title"))
            mapEntity.c_maxLon = c.getDouble(c.getColumnIndex("maxLon"))
            mapEntity.c_maxLat = c.getDouble(c.getColumnIndex("maxLat"))
            mapEntity.c_minLon = c.getDouble(c.getColumnIndex("minLon"))
            mapEntity.c_minLat = c.getDouble(c.getColumnIndex("minLat"))
            mapEntity.c_creationDate =
                DateUtil.stringToCalendar(c.getString(c.getColumnIndex("creationDate")))
            return mapEntity
        }
    }
}