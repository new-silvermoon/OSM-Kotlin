package org.silvermoon.osm_kotlin.concurrency

import android.os.Handler
import android.os.Message
import android.view.View

class TileHandler : Handler {
    var mView: View? = null

    constructor() : super() {}
    constructor(view: View?) : super() {
        mView = view
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            TILE_LOADED -> if (mView != null) mView!!.invalidate()
        }
    }

    companion object {
        const val TILE_NOT_LOADED = 10
        const val TILE_LOADED = 11
    }
}