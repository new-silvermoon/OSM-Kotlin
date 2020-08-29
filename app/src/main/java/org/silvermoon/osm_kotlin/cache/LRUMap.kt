package org.silvermoon.osm_kotlin.cache

import java.util.*


class LRUMap<K, V>(initialCapacity: Int, private val maxCapacity: Int) :
    LinkedHashMap<K, V>(initialCapacity, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
        return size > maxCapacity
    }

    companion object {
        private const val serialVersionUID = 1L
    }

}