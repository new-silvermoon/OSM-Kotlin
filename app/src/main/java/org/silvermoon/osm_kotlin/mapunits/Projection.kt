package org.silvermoon.osm_kotlin.mapunits

class Projection {

    companion object {
        // OrignShift  2 * math.pi * 6378137 / 2.0
        private const val MAX_X = 20037508.342789244
        private const val MAX_Y = 20037508.342789244
        fun getMapSize(zoomLevel: Int): Double {
            return Math.pow(
                2.0,
                zoomLevel.toDouble()
            ) * Tile.TILE_SIZE
        }

        fun getXPixelFromLongitude(lon: Double, zoomLevel: Int): Int {
            val mapWidth = getMapSize(zoomLevel)

            // Converts given lon in WGS84 Datum to XY in Spherical Mercator EPSG:900913
            var x = lon * MAX_X / 180.0

            // Scale to map
            x = (x + MAX_X) / (MAX_X * 2) * mapWidth
            return x.toInt()
        }

        fun getYPixelFromLatitude(lat: Double, zoomLevel: Int): Int {
            val mapWidth = getMapSize(zoomLevel)

            //Converts given lat in WGS84 Datum to XY in Spherical Mercator EPSG:900913
            var y =
                Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0)
            y = y * MAX_Y / 180.0

            // Scale to map
            y = mapWidth - (y + MAX_Y) / (MAX_Y * 2) * mapWidth
            return y.toInt()
        }

        fun getProjectionFromPixels(
            x: Int,
            y: Int,
            zoom: Int
        ): GeoPoint {
            //Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84 Datum
            val mapWidth = getMapSize(zoom)
            // Convert Map pixel to mercator coord
            val mercatorX = MAX_X - MAX_X * 2 * (x / mapWidth)
            val mercatorY =
                -(MAX_Y - MAX_Y * 2 * ((y + mapWidth) / mapWidth))
            val lon = mercatorX / MAX_X * 180.0
            var lat = mercatorY / MAX_Y * 180.0
            lat =
                180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0)
            return GeoPoint((lat * 1E6).toInt(), (lon * 1E6).toInt())
        }

        fun getZoomLevelFromBox(
            maxLat: Double, maxLon: Double, minLat: Double,
            minLon: Double, viewWidth: Int, viewHeight: Int
        ): Int {
            return getZoomLevelFromBox(
                maxLat,
                maxLon,
                minLat,
                minLon,
                viewWidth,
                viewHeight,
                0,
                0
            )
        }

        fun getZoomLevelFromBox(
            maxLat: Double,
            maxLon: Double,
            minLat: Double,
            minLon: Double,
            viewWidth: Int,
            viewHeight: Int,
            viewPaddingWidth: Int,
            viewPaddingHeight: Int
        ): Int {

            // Reduce view by XX pixels to not have markers or tracks on edge of screen
            var viewWidth = viewWidth
            var viewHeight = viewHeight
            viewWidth -= viewPaddingWidth
            viewHeight -= viewPaddingHeight
            //---
            var zoomLevel: Int
            var maxLatOffset: Int
            var minLonOffset: Int
            val maxLatE6 = (maxLat * 1E6).toInt()
            val maxLonE6 = (maxLon * 1E6).toInt()
            val minLatE6 = (minLat * 1E6).toInt()
            val minLonE6 = (minLon * 1E6).toInt()

            //	Log.i("getZoomLevelFromBox", "maxLatE6:" + maxLatE6 + "  maxLonE6:" + maxLonE6 + "  minLatE6:" + minLatE6 + "  minLonE6:" + minLonE6);
            zoomLevel = OsmMapViewBase.MIN_ZOOM_LEVEL_FOR_TILES
            while (zoomLevel > 0) {
                maxLatOffset = 0 - getYPixelFromLatitude(maxLat, zoomLevel)
                minLonOffset = 0 - getXPixelFromLongitude(minLon, zoomLevel)
                val upperLeftScreen =
                    getProjectionFromPixels(minLonOffset, maxLatOffset, zoomLevel)
                val lowerRightcreen =
                    getProjectionFromPixels(
                        minLonOffset - viewWidth,
                        maxLatOffset - viewHeight,
                        zoomLevel
                    )

                //	Log.i("getZoomLevelFromBox", "upperLeftScreen.getLatitudeE6():" + upperLeftScreen.getLatitudeE6() + "   upperLeftScreen.getLongitudeE6():" + upperLeftScreen.getLongitudeE6()
                //			+ "   lowerRightcreen.getLatitudeE6():" + lowerRightcreen.getLatitudeE6() + "   lowerRightcreen.getLongitudeE6():" + lowerRightcreen.getLongitudeE6());
                if (upperLeftScreen.latitudeE6 >= maxLatE6 && upperLeftScreen.longitudeE6 <= minLonE6 && lowerRightcreen.latitudeE6 <= minLatE6 && lowerRightcreen.longitudeE6 >= maxLonE6
                ) {

                    //fit in screen
                    break
                }
                zoomLevel--
            }
            return zoomLevel
        }

        fun getMapTileFromCoordinates(
            lat: Double,
            lon: Double,
            zoom: Int
        ): Tile {
            val y = Math.floor(
                (1 - Math.log(
                    Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)
                ) / Math.PI) / 2 * (1 shl zoom)
            ).toInt()
            val x = Math.floor((lon + 180) / 360 * (1 shl zoom)).toInt()
            return Tile(x, y, zoom)
        }
    }
}