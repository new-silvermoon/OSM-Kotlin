package org.silvermoon.osm_kotlin.concurrency

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.silvermoon.osm_kotlin.mapunits.Tile
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.util.Vector

class RequestTile {
    private val mOsmBasePool = OsmBasePool()

    suspend fun loadBitmap(tile: Tile): ByteArray? = withContext(Dispatchers.IO) {
        val tileUrl = mOsmBasePool.nextBase + tile.key
        try {
            val url = URL(tileUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            connection.getInputStream().use { input ->
                BufferedInputStream(input, IO_BUFFER_SIZE).use { bufferedInput ->
                    ByteArrayOutputStream().use { output ->
                        bufferedInput.copyTo(output, IO_BUFFER_SIZE)
                        return@withContext output.toByteArray()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("class RequestTile", "IOException: Error getting the tile from url: $tileUrl", e)
            delay(3000)
        } catch (e: Exception) {
            Log.e("class RequestTile", "Exception: Error getting the tile from url: $tileUrl", e)
            delay(3000)
        }
        return@withContext null
    }

    internal inner class OsmBasePool {
        private val bases: Vector<String> = Vector<String>()
        private var iterator = 0
        val nextBase: String
            get() {
                synchronized(this) {
                    ++iterator
                    if (iterator == bases.size) {
                        iterator = 0
                    }
                    return bases.elementAt(iterator)
                }
            }

        init {
            bases.add(URL_OSM_A)
            bases.add(URL_OSM_B)
            bases.add(URL_OSM_C)
        }
    }

    companion object {
        private const val URL_OSM_A = "https://tile.openstreetmap.org/"
        private const val URL_OSM_B = "https://tile.openstreetmap.org/"
        private const val URL_OSM_C = "https://tile.openstreetmap.org/"
        private const val IO_BUFFER_SIZE = 8192
    }
}