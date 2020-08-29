package org.silvermoon.osm_kotlin.cache

import java.io.File
import java.util.*


class ManageTilesCached(private val mDirectory: File?) {
    /**
     * Delete tiles above a certain limit (first in first out)
     * @param limit in Mb
     */
    fun deleteTilesAboveLimit(limit: Int) {
        val limitKb = limit * 1024 //put limit in Kb
        if (mDirectory == null) return
        val t: Thread = object : Thread() {
            override fun run() {

                //long time = Calendar.getInstance().getTimeInMillis();
                val files = getAllFilesInDirectories(mDirectory)
                val filesMap: MutableMap<Long, File> = HashMap()
                for (file in files) {
                    filesMap[file.lastModified()] = file
                }
                val filesSizeKb = AVERAGE_TILE_SIZE * files.size
                if (filesSizeKb > limitKb) {
                    var nbFilesToDeleted = (filesSizeKb - limitKb) / AVERAGE_TILE_SIZE
                    val keys = TreeSet(filesMap.keys)
                    for (key in keys) {
                        if (nbFilesToDeleted <= 0) break
                        val fileValue = filesMap[key]
                        fileValue!!.delete()
                        nbFilesToDeleted--
                    }
                }

                //Log.i("ManageTilesCached", "File count= " + files.size());
                //Log.i("ManageTilesCached", "time= " + (Calendar.getInstance().getTimeInMillis() - time) + "ms");
            }
        }
        t.start()
    }

    companion object {
        private const val AVERAGE_TILE_SIZE = 35 // in Kb
        fun getAllFilesInDirectories(directory: File): List<File> {
            val files: MutableList<File> = ArrayList()
            for (file in directory.listFiles()) {
                if (file.isFile) {
                    files.add(file)
                }
                if (file.isDirectory) {
                    files.addAll(getAllFilesInDirectories(file))
                }
            }
            return files
        }
    }
}