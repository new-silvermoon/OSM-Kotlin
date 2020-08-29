package org.silvermoon.osm_kotlin.concurrency

import android.util.Log
import org.silvermoon.osm_kotlin.mapunits.Tile
import java.io.*
import java.net.URL
import java.util.*

class RequestTile {
    private val mOsmBasePool = OsmBasePool()
    private val mBuffer: ByteArray

    @Throws(InterruptedException::class)
    fun loadBitmap(tile: Tile): ByteArray? {
        val tileUrl = mOsmBasePool.nextBase + tile.key
        var `in`: InputStream? = null
        var out: OutputStream? = null
        var dataStream: ByteArrayOutputStream? = null
        try {
            val urL = URL(tileUrl)
            val inStream: InputStream = urL.openStream()
            `in` = BufferedInputStream(inStream, IO_BUFFER_SIZE)
            dataStream = ByteArrayOutputStream()
            out = BufferedOutputStream(dataStream, IO_BUFFER_SIZE)
            copy(`in`, out)
            out.flush()
            out.close()
            return dataStream.toByteArray()
        } catch (e: IOException) {
            Log.e("class RequestTile", "IOException: Error getting the tile from url")
            Thread.sleep(3000)
        } catch (e: Exception) {
            Log.e("class RequestTile", "Exception: Error getting the tile from url")
            Thread.sleep(3000)
        }
        return null
    }

    private fun copy(input: InputStream?, out: OutputStream?) {
        var read: Int = 0
        try {
            while (input!!.read(mBuffer) != -1) {
                out!!.write(mBuffer, 0, read)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    internal inner class OsmBasePool {
        private val bases: Vector<String> = Vector<String>()
        private var iterator = 0
        val nextBase: String
            get() {
                ++iterator
                if (iterator == bases.size) {
                    iterator = 0
                }
                return bases.elementAt(iterator)
            }

        init {
            bases.add(URL_OSM_A)
            bases.add(URL_OSM_B)
            bases.add(URL_OSM_C)
        }
    }

    companion object {
        /* Switching the urls to OpenStreetMaps from cloudmade
	 * private static final String URL_OSM_A = "http://andy.sandbox.cloudmade.com/tiles/cycle/";
	private static final String URL_OSM_B = "http://andy.sandbox.cloudmade.com/tiles/cycle/";
	private static final String URL_OSM_C = "http://andy.sandbox.cloudmade.com/tiles/cycle/";
	*/
        private const val URL_OSM_A = "https://tile.openstreetmap.org/"
        private const val URL_OSM_B = "https://tile.openstreetmap.org/"
        private const val URL_OSM_C = "https://tile.openstreetmap.org/"
        private const val IO_BUFFER_SIZE = 8192
    }

    init {
        mBuffer = ByteArray(IO_BUFFER_SIZE)
    }
}