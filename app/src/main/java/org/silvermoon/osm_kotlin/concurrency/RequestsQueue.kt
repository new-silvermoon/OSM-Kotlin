package org.silvermoon.osm_kotlin.concurrency

import org.silvermoon.osm_kotlin.mapunits.Tile
import java.util.*

class RequestsQueue internal constructor(var id: Int, private val mTileStackSizeLimit: Int) {
    private val queue: Stack<Tile> = Stack<Tile>()
    private val lock = Any()
    fun queue(tile: Tile?) {
        synchronized(lock) {
            if (tile != null && !contains(tile)) {
                if (mTileStackSizeLimit > 0 && queue.size > mTileStackSizeLimit) queue.remove(
                    queue.lastElement()
                )
                queue.push(tile)
            }
        }
    }

    operator fun contains(tile: Tile): Boolean {
        synchronized(lock) {
            for (qTile in queue) {
                if (qTile.key != null && qTile.key.equals(tile.key)) return true
            }
            return false
        }
    }

    fun dequeue(): Tile {
        synchronized(lock) { return queue.pop() }
    }

    fun hasRequest(): Boolean {
        synchronized(lock) { return queue.size !== 0 }
    }

    fun clear() {
        synchronized(lock) { queue.clear() }
    }

    fun size(): Int {
        synchronized(lock) { return queue.size }
    }

}