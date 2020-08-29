package org.silvermoon.osm_kotlin.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import org.silvermoon.osm_kotlin.mapunits.Tile
import java.io.PipedInputStream
import java.io.PipedOutputStream


class BitmapScaler {

    companion object {
        fun scaleTo(
            bitmap: Bitmap?,
            scaleFactor: Float,
            xIncrement: Int,
            yIncrement: Int
        ): Bitmap? {
            if (bitmap == null) return bitmap
            try {
                val width = (bitmap.width / 2 / scaleFactor).toInt()
                val height = (bitmap.height / 2 / scaleFactor).toInt()
                val resizedBitmap =
                    Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, Bitmap.Config.RGB_565)
                val x = width * xIncrement
                val y = height * yIncrement
                val src = Rect(x, y, x + width, y + height)
                val dest =
                    Rect(0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE)
                val c = Canvas(resizedBitmap)
                c.drawBitmap(bitmap, src, dest, null)
                return resizedBitmap
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun convertBitmapToStream(src: Bitmap): PipedOutputStream {
            val os = PipedOutputStream()
            src.compress(Bitmap.CompressFormat.PNG, 100, os)
            return os
        }

        fun scaleToCompress(bitmap: Bitmap, scaleFactor: Float): Bitmap? {
            try {
                val pin =
                    PipedInputStream(convertBitmapToStream(bitmap))
                return decodeFile(pin, scaleFactor)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bitmap
        }

        private fun decodeFile(`is`: PipedInputStream, scaleFactor: Float): Bitmap? {
            try {
                //Decode image size
                val o = BitmapFactory.Options()
                o.inJustDecodeBounds = true
                BitmapFactory.decodeStream(`is`, null, o)


                //Decode with inSampleSize
                val o2 = BitmapFactory.Options()
                o2.inSampleSize = scaleFactor.toInt()
                return BitmapFactory.decodeStream(`is`, null, o2)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}