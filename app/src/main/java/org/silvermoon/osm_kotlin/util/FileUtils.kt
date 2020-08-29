package org.silvermoon.osm_kotlin.util

import java.io.*
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

class FileUtils {
    companion object {
        fun deleteInsideDirectoryThread(dir: File) {
            val t: Thread = object : Thread() {
                override fun run() {
                    if (dir.isDirectory) {
                        val children = dir.list()
                        for (i in children.indices) {
                            deleteDirectory(
                                File(
                                    dir,
                                    children[i]
                                )
                            )
                        }
                    }
                }
            }
            t.start()
        }

        fun deleteInsideADirectory(dir: File): Boolean {
            if (dir.isDirectory) {
                val children = dir.list()
                for (i in children.indices) {
                    val success = deleteDirectory(
                        File(
                            dir,
                            children[i]
                        )
                    )
                    if (!success) return false
                }
            }
            return true
        }

        fun deleteDirectory(dir: File): Boolean {
            return if (deleteInsideADirectory(dir) == false) false else dir.delete()
        }

        fun countFilesInDirectory(directory: File): Int {
            var count = 0
            for (file in directory.listFiles()) {
                if (file.isFile) {
                    count++
                }
                if (file.isDirectory) {
                    count += countFilesInDirectory(file)
                }
            }
            return count
        }

        @Throws(IOException::class)
        fun copyFile(src: InputStream?, dest: OutputStream?): Boolean {
            val buff = ByteArray(1024)
            var length: Int
            var result = false
            try {
                while (src!!.read(buff).also { length = it } > 0) {
                    dest!!.write(buff, 0, length)
                }
                result = true
            } catch (e: Exception) {
                result = false
            } finally {
                try {
                    if (dest != null) {
                        try {
                            dest.flush()
                        } finally {
                            dest.close()
                        }
                    }
                } finally {
                    src?.close()
                }
            }
            return result
        }

        @Throws(Exception::class)
        fun extractFileToDestination(
            src: InputStream?,
            dest: OutputStream?
        ) {
            val buff = ByteArray(8192)
            var length: Int
            val inZip = GZIPInputStream(src)
            val `in` = BufferedInputStream(inZip)
            val out = BufferedOutputStream(dest)
            try {
                while (`in`.read(buff).also { length = it } > 0) {
                    out.write(buff, 0, length)
                }
            } finally {
                `in`?.close()
                out?.close()
                src?.close()
                dest?.close()
            }
        }

        @Throws(IOException::class)
        fun copyFolder(src: File, dest: File) {
            if (src.isDirectory) {

                // if directory not exists, create it
                if (!dest.exists()) {
                    dest.mkdir()
                }

                // list all the directory contents
                val files = src.list()
                for (file in files) {
                    // construct the src and dest file structure
                    val srcFile = File(src, file)
                    val destFile = File(dest, file)
                    // recursive copy
                    copyFolder(srcFile, destFile)
                }
            } else {
                // if file, then copy it
                // Use bytes stream to support all file types
                val `in`: InputStream = FileInputStream(src)
                val out: OutputStream = FileOutputStream(dest)
                val buffer = ByteArray(1024)
                var length: Int
                // copy the file content in bytes
                while (`in`.read(buffer).also { length = it } > 0) {
                    out.write(buffer, 0, length)
                }
                `in`.close()
                out.close()
            }
        }

        @Throws(IOException::class)
        fun getStringFromHtmlFile(src: InputStream?): String {
            val stringBuffer = StringBuffer()
            try {
                var character = src!!.read()
                while (character > 0) {
                    stringBuffer.append(character.toChar())
                    character = src.read()
                }
            } finally {
                src?.close()
            }
            return stringBuffer.toString()
        }

        @Throws(IOException::class)
        fun writeStringBufferToFile(
            stringBuffer: StringBuffer,
            dest: OutputStream
        ) {
            try {
                for (i in 0 until stringBuffer.length) {
                    dest.write(stringBuffer[i].toInt())
                }
                dest.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        @Throws(IOException::class)
        fun readFile(file: File?): String {
            val stream = FileInputStream(file)
            return try {
                val fc = stream.channel
                val bb = fc.map(
                    FileChannel.MapMode.READ_ONLY, 0,
                    fc.size()
                )
                Charset.defaultCharset().decode(bb).toString()
            } finally {
                stream.close()
            }
        }
    }
}