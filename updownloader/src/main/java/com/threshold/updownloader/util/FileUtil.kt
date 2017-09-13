package com.threshold.updownloader.util

import android.webkit.MimeTypeMap

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

internal object FileUtil {

    fun fileExt(file: File): String? {
        return fileExt(file.name)
    }

    fun fileExt(url: String): String? {
        var myUrl = url
        if (myUrl.contains("?")) {
            myUrl = myUrl.substring(0, myUrl.indexOf("?"))
        }
        return if (myUrl.lastIndexOf(".") == -1) {
            null
        } else {
            var ext = myUrl.substring(myUrl.lastIndexOf(".") + 1)
            if (ext.contains("%")) {
                ext = ext.substring(0, ext.indexOf("%"))
            }
            if (ext.contains("/")) {
                ext = ext.substring(0, ext.indexOf("/"))
            }
            ext.toLowerCase()
        }
    }

    fun getMimeTypeFromFileExtension(fileExtension: String): String {
        var myFileExt = fileExtension
        if (myFileExt.contains(".")) {
            myFileExt = myFileExt.replace(".", "")
        }
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getMimeTypeFromExtension(myFileExt)
    }

    fun getMimeTypeFromFileUri(fileUri: String): String? {
        val fileExt = fileExt(fileUri) ?: return null
        return getMimeTypeFromFileExtension(fileExt)
    }

    fun dirExists(dirLocation: String): Boolean {
        val file = File(dirLocation)
        return file.exists() || file.mkdirs()
    }

    fun setFileToLatestModified(file: File) {
        try {
            RandomAccessFile(file, "rw").use { raf ->
                val length = raf.length()
                raf.setLength(length + 1)
                raf.setLength(length)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}