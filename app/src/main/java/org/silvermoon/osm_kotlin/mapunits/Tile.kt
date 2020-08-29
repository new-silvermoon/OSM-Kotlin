package org.silvermoon.osm_kotlin.mapunits

class Tile {


    var mapX = 0
    var mapY = 0
    var offsetX = 0
    var offsetY = 0
    var zoom = 0
    var key: String? = null
    var mapTypeId = 0
    var bitmap: ByteArray? = null

//    var keyVal: String?
//        get() = key
//        set(value) {
//            key = value
//        }

    constructor() {}
    constructor(x: Int, y: Int, z: Int) {
        mapX = x
        mapY = y
        zoom = z
        key = "$z/$x/$y.png"

    }

    constructor(x: Int, y: Int, z: Int, aMapTypeId: Int) {
        mapX = x
        mapY = y
        zoom = z
        key = "$z/$x/$y.png"
        mapTypeId = aMapTypeId

    }

    constructor(tile: Tile) {
        mapX = tile.mapX
        mapY = tile.mapY
        offsetX = tile.offsetX
        offsetY = tile.offsetY
        zoom = tile.zoom
        key = tile.key
        mapTypeId = tile.mapTypeId
        bitmap = tile.bitmap
    }



    companion object {
        const val TILE_SIZE = 256
        const val AVERAGE_TILE_SIZE = 35 // in Kb
    }
}