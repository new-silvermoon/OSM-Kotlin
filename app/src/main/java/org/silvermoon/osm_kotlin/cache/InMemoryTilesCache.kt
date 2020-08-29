package org.silvermoon.osm_kotlin.cache

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler


class InMemoryTilesCache(context: Context?, handler: Handler?) {
    private var mBitmapCache: LRUMap<String, Bitmap> = LRUMap<String, Bitmap>(8, 8)
    private val mLock = Any()
    fun add(tileKey: String?, bitmap: Bitmap?) {
        synchronized(mLock) { mBitmapCache.put(tileKey!!, bitmap!!) }
    }

    fun hasTile(tileKey: String?): Boolean {
        synchronized(mLock) { return mBitmapCache.containsKey(tileKey) }
    }

    fun clean() {
        synchronized(mLock) { mBitmapCache.clear() }
    }

    fun getTileBitmap(tileKey: String?): Bitmap {
        synchronized(mLock) { return mBitmapCache.get(tileKey)!! }
    }

    fun setBitmapCacheSize(size: Int) {
        mBitmapCache = LRUMap<String, Bitmap>(size, size + 2)
    }
}