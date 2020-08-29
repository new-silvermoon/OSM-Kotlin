package org.silvermoon.osm_kotlin.mapunits

import java.io.Serializable


class GeoPoint : Serializable {
    var latitudeE6 = 0
        set(latitudeE6) {
            field = normalizeLatitude((latitudeE6 / 1E6)).toInt()
        }
    var longitudeE6 = 0
        set(longitudeE6) {
            field = normalizeLongitude((longitudeE6 / 1E6)).toInt()
        }

    fun normalizeLatitude(latitude: Double): Double {
        if (!java.lang.Double.isNaN(latitude)) {
            if (latitude < -90) return (-90 * 1E6) else if (latitude > 90) return (90 * 1E6)
        }
        return latitude * 1E6
    }

    fun normalizeLongitude(longitude: Double): Double {
        if (!java.lang.Double.isNaN(longitude)) {
            if (longitude < -180) return (((longitude - 180) % 360) + 180) * 1E6 else if (longitude > 180) return (((longitude + 180) % 360) - 180) * 1E6
        }
        return longitude * 1E6
    }

    constructor() {}
    constructor(latitudeE6: Int, longitudeE6: Int) {
        this@GeoPoint.latitudeE6 = latitudeE6
        this@GeoPoint.longitudeE6 = longitudeE6
    }

    companion object {
        private const val serialVersionUID = -6241356443051839339L
    }
}