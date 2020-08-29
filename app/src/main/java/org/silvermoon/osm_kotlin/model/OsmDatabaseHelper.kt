package org.silvermoon.osm_kotlin.model

import android.content.Context
import java.io.File


class OsmDatabaseHelper(context: Context?) :
    BaseDatabaseHelper(context!!) {
    var databaseFile: File? = null

    fun openOrCreateDatabase(): Boolean {
        return if (databaseFile != null) super.openOrCreateDatabase(
            mContext,
            databaseFile!!
        ) else false
    }

    fun openDatabase(): Boolean {
        return if (databaseFile!!.exists()) super.openDatabase(mContext, databaseFile!!) else false
    }

}