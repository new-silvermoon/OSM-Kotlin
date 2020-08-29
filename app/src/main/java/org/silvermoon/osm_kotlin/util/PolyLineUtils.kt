package org.silvermoon.osm_kotlin.util

import org.silvermoon.osm_kotlin.mapunits.GeoPoint

class PolyLineUtils {

    fun decodePoly(encoded: String): List<GeoPoint>? {
        val poly: MutableList<GeoPoint> = ArrayList<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = GeoPoint(
                (lat / 1E5 * 1E6).toInt(),
                (lng / 1E5 * 1E6).toInt()
            )
            poly.add(p)
        }
        return poly
    }

    fun encodePoly(
        geopoints: List<GeoPoint>,
        level: Int,
        step: Int
    ): HashMap<String, String>? {
        val resultMap =
            HashMap<String, String>()
        val encodedPoints = StringBuffer()
        val encodedLevels = StringBuffer()
        var plat = 0
        var plng = 0
        for (g in geopoints) {
            val late5 = floor1e5(g.latitudeE6 / 1E6)
            val lnge5 = floor1e5(g.longitudeE6 / 1E6)
            val dlat = late5 - plat
            val dlng = lnge5 - plng
            plat = late5
            plng = lnge5
            encodedPoints.append(encodeSignedNumber(dlat)).append(encodeSignedNumber(dlng))
            encodedLevels.append(encodeNumber(level))
        }
        resultMap["encodedPoints"] = encodedPoints.toString()
        resultMap["encodedLevels"] = encodedLevels.toString()
        return resultMap
    }

    private fun encodeSignedNumber(num: Int): String? {
        var sgn_num = num shl 1
        if (num < 0) {
            sgn_num = sgn_num.inv()
        }
        return encodeNumber(sgn_num)
    }

    private fun encodeNumber(num: Int): String? {
        var num = num
        val encodeString = StringBuffer()
        while (num >= 0x20) {
            val nextValue = (0x20 or (num and 0x1f)) + 63
            encodeString.append(nextValue.toChar())
            num = num shr 5
        }
        num += 63
        encodeString.append(num.toChar())
        return encodeString.toString()
    }

    private fun floor1e5(coordinate: Double): Int {
        return Math.floor(coordinate * 1e5).toInt()
    }
}