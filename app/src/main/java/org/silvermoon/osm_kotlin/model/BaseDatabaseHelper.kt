package org.silvermoon.osm_kotlin.model


import android.content.ContentValues
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.silvermoon.osm_kotlin.R

import java.io.*


open class BaseDatabaseHelper //mAlreadyTriedToOpenDb = false;
    (//private boolean mAlreadyTriedToOpenDb;
    protected var mContext: Context
) {
    var mDb: SQLiteDatabase? = null

    fun openDatabase(context: Context?, dbFile: File): Boolean {
        try {
            Log.i("SQLiteHelper", "Opening database at $dbFile")
            mDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun openOrCreateDatabase(
        context: Context?,
        dbFile: File
    ): Boolean {
        try {
            return if (dbFile.exists()) {
                Log.i("SQLiteHelper", "Opening database at $dbFile")
                mDb = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                true

                // Test if DB works properly
                //get(MapTile.TABLE_TILE_NAME, "tilekey");
                //---

                //if (DATABASE_VERSION > db.getVersion())
                //upgrade();
            } else {
                Log.i("SQLiteHelper", "Creating database at $dbFile")
                mDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
                Log.i("SQLiteHelper", "Opened database at $dbFile")
                upgradeFromFile(mDb, R.raw.kotlin_osm_maptile)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun upgradeFromFile(db: SQLiteDatabase?, ressourceId: Int) {
        var sqlFile: InputStream? = null
        sqlFile = try {
            mContext.resources.openRawResource(ressourceId)
        } catch (e: NotFoundException) {
            e.printStackTrace()
            return
        }
        val br = BufferedReader(InputStreamReader(sqlFile))
        var line: String? = null
        try {
            while (br.readLine().also { line = it } != null) {
                db!!.execSQL(line)
            }
        } catch (se: SQLException) {
        } catch (e: IOException) {
        }
    }

    fun close() {
        if (mDb != null && mDb!!.isOpen) mDb!!.close()
    }

    val isOpen: Boolean
        get() = if (mDb == null) false else mDb!!.isOpen

    fun insert(table: String?, values: ContentValues?): Long {
        try {
            return mDb!!.insertOrThrow(table, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    operator fun get(table: String?, select: String?): Cursor {
        return mDb!!.query(true, table, null, select, null, null, null, null, null)
    }

    operator fun get(
        table: String?,
        select: String?,
        columns: Array<String?>?,
        orderBy: String?
    ): Cursor {
        return mDb!!.query(true, table, columns, select, null, null, null, orderBy, null)
    }

    operator fun get(
        table: String?,
        select: String?,
        columns: Array<String>?,
        orderBy: String?,
        limit: String?
    ): Cursor {
        return mDb!!.query(true, table, columns, select, null, null, null, orderBy, limit)
    }

    operator fun get(
        table: String?,
        select: String?,
        orderBy: String?,
        limit: String?
    ): Cursor {
        return mDb!!.query(true, table, null, select, null, null, null, orderBy, limit)
    }

    operator fun get(
        table: String?,
        select: String?,
        limit: String?
    ): Cursor {
        return mDb!!.query(true, table, null, select, null, null, null, null, limit)
    }

    fun query(sql: String?): Cursor? {
        try {
            return mDb!!.rawQuery(sql, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun delete(table: String?, where: String?): Int {
        return mDb!!.delete(table, where, null)
    }

    fun update(table: String?, values: ContentValues?, where: String?): Int {
        return mDb!!.update(table, values, where, null)
    }

}