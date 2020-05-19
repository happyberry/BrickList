package com.example.bricklist

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.util.Log
import android.os.AsyncTask
import com.example.bricklist.Database
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL


class Item {
    var id: Int? = null
    var itemType: String? = null
    var itemId: Int? = -9
    var quantityInSet: Int? = null
    var quantityInStore: Int? = 0
    var colorCode: Int? = null
    var extra: Boolean? = null

    var colorName: String? = null
    var code: String? = null
    var designId: Int? = null
    var image: Bitmap? = null
    var imageSrc: String? = null
    var name: String = ""
    //var alternate: Boolean? = null

    constructor(id: Int, itemType: String, itemId: Int, quantityInSet: Int, quantityInStore: Int, colorCode: Int, extra: Boolean){
        this.id = id
        this.itemType = itemType
        this.itemId = itemId
        this.quantityInSet = quantityInSet
        this.quantityInStore = quantityInStore
        this.colorCode = colorCode
        this.extra = extra
        //this.alternate = alternate
    }

    constructor()

    fun showItem() {
        Log.i("StateChange", "itemId: " + itemId + " qInSet: " + quantityInSet + " qInStore: " + quantityInStore + " image: " + image + " designId: " + designId + " color: " + colorName)
    }


    @SuppressLint("StaticFieldLeak")
    inner class DownloadImage(private var database: Database, private var item: Item, private var url: String): AsyncTask<String, Int, String>() {
        override fun doInBackground(vararg params: String?): String {
            try {
                BufferedInputStream(URL(url).content as InputStream).use {
                    val baf = ArrayList<Byte>()
                    var current: Int
                    while (true) {
                        current = it.read()
                        if (current == -1)
                            break
                        baf.add(current.toByte())
                    }
                    val blob = baf.toByteArray()
                    val blobValues = ContentValues()
                    blobValues.put("Image", blob)
                    //database.saveImageToDatabase(item, blobValues)
                }
            } catch (e: IOException) {
                try {
                    url = "http://img.bricklink.com/P/" + item.colorName + "/" + item.code + ".gif"

                    BufferedInputStream(URL(url).content as InputStream).use {
                        val baf = ArrayList<Byte>()
                        var current: Int
                        while (true) {
                            current = it.read()
                            if (current == -1)
                                break
                            baf.add(current.toByte())
                        }
                        val blob = baf.toByteArray()
                        val blobValues = ContentValues()
                        blobValues.put("Image", blob)
                        //database.saveImageToDatabase(item, blobValues)
                    }
                } catch (e: IOException) {
                    try {
                        item.imageSrc = "https://www.bricklink.com/PL/" + item.code + ".jpg"
                        BufferedInputStream(URL(url).content as InputStream).use {
                            val baf = ArrayList<Byte>()
                            var current: Int
                            while (true) {
                                current = it.read()
                                if (current == -1)
                                    break
                                baf.add(current.toByte())
                            }
                            val blob = baf.toByteArray()
                            val blobValues = ContentValues()
                            blobValues.put("Image", blob)
                            //database.saveImageToDatabase(item, blobValues)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        return "IOException"
                    }
                }
            }
            return "success"
        }
    }
}
