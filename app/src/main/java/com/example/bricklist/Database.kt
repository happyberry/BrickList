package com.example.bricklist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import org.w3c.dom.NodeList
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import kotlin.collections.ArrayList

class Database(var context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int): SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object{
        private val DATABASE_NAME = "BrickList.db"
        private val DATABASE_VERSION = 1
    }
    var connection: SQLiteDatabase? = null
    var name = "BrickList.db"
    var path = "/data/data/com.example.bricklist/databases/"


    override fun onCreate(db: SQLiteDatabase?){
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    fun createDatabaseIfAbsent() {
        var isDB: SQLiteDatabase? = null
        try {
            isDB = SQLiteDatabase.openDatabase(path + name, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteException) {

        }
        if (isDB != null) {
            isDB.close()
        } else {
            //this.readableDatabase
            try {
                val myInput = context.assets.open(name)
                val outFileName = path + name
                val myOutput = FileOutputStream(outFileName)
                val buffer = ByteArray(1024)
                var length = myInput.read(buffer)
                while (length > 0) {
                    myOutput.write(buffer, 0, length)
                    length = myInput.read(buffer)
                }
                myOutput.flush()
                myOutput.close()
                myInput.close()
            } catch (e: IOException) {
                throw Error("Error copying database")
            }
        }

    }

    fun openDatabase() {
        connection = SQLiteDatabase.openDatabase(path+name, null, SQLiteDatabase.OPEN_READWRITE)
    }

    fun getProjects(archived: Boolean): ArrayList<String>{
        var query = ""
        if (archived) {
            query = "select Name from Inventories order by LastAccessed desc"
        } else {
            query = "select Name from Inventories where Active=1 order by LastAccessed desc"
        }
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        val inventoriesList = ArrayList<String>()
        if(cursor.moveToFirst()) {
            inventoriesList.add(cursor.getString(0))
        }
        while(cursor.moveToNext()){
            inventoriesList.add(cursor.getString(0))
        }
        if (cursor != null && !cursor.isClosed) {
            cursor.close()
        }
        db.close()
        return inventoriesList
    }

    fun addInventory(name: String) {
        val db = this.writableDatabase
        db.beginTransaction()
        var values = ContentValues()
        values.put("Name", name)
        values.put("Active", 1)
        values.put("LastAccessed", System.currentTimeMillis()/1000)
        db.insert("Inventories", null, values)
        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()
    }

    fun addInventoriesParts(inventoryId: Int, items: NodeList): String{
        var db = this.writableDatabase
        db.beginTransaction()
        var lacking = ""
        for (i in 0 until items.length){
            val properties = items.item(i).childNodes
            val itemType = getTypeId(properties.item(1).textContent.toString().trim())
            val itemId = getItemId(properties.item(3).textContent.toString().trim())
            var values = ContentValues()
            values.put("InventoryID", inventoryId)
            values.put("TypeID", itemType)
            values.put("ItemID", properties.item(3).textContent.toString().trim())
            values.put("QuantityInSet", properties.item(5).textContent.toString().trim())
            values.put("QuantityInStore", 0)
            values.put("ColorID", properties.item(7).textContent.toString().trim().toInt())
            val colorname = getColorByCode(properties.item(7).textContent.toString().trim().toInt())
            if (itemType == -1 || itemId == -1) {
                lacking += itemType.toString() + " " + colorname + ", "
            } else {
                db.insert("InventoriesParts", null, values)
                val colorId = getColorIdByCode(properties.item(7).textContent.toString().trim().toInt())
                val designId = getDesignId(colorId, itemId)
                if (designId != -1) {
                    val imageFound = checkIfImageInDatabase(designId)
                    if (!imageFound) {
                        Log.e("DOWNLOAD", "Pobieranie zdjecia do bazy" + designId.toString())
                        Log.e("DOWNLOAD", colorname + properties.item(3).textContent.toString().trim())
                        DownloadImage(colorname, properties.item(3).textContent.toString().trim(), designId, "https://www.lego.com/service/bricks/5/2/" + designId.toString()).execute()
                    }
                }
            }
        }
        if (lacking != "") {
            lacking = lacking.substring(0, lacking.length - 2)
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()
        Log.e("tag", lacking)
        return lacking
    }


    fun getInventoryId(name: String): Int {
        val db = this.readableDatabase
        val query = "select * from Inventories where Name like \"$name\""
        val cursor = db.rawQuery(query, null)
        if (cursor.count <= 0) {
            cursor.close()
            return -1
        }
        cursor.moveToFirst()
        val Id = cursor.getInt(0)
        cursor.close()
        return Id
    }

    fun getTypeId(typeName: String): Int {
        val db = this.readableDatabase
        val query = "select * from ItemTypes where Code like \"$typeName\""
        val cursor = db.rawQuery(query, null)
        if (cursor.count <= 0) {
            cursor.close()
            return -1
        }
        cursor.moveToFirst()
        val Id = cursor.getInt(0)
        cursor.close()
        return Id
    }

    fun getColorByCode(code: Int): String {
        val db = this.readableDatabase
        val query = "select * from Colors where Code = ${code}"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val colorName = cursor.getString(2)
        cursor.close()
        return colorName
    }

    fun getColorIdByCode(code: Int): Int {
        val db = this.readableDatabase
        val query = "select * from Colors where Code = ${code}"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val colorId = cursor.getInt(0)
        cursor.close()
        return colorId
    }

    fun getItemId(code: String): Int {
        val db = this.readableDatabase
        val query = "select id from Parts where Code like \"${code}\""
        val cursor = db.rawQuery(query, null)
        if (cursor.count <= 0) {
            cursor.close()
            return -1
        }
        cursor.moveToFirst()
        val itemId = cursor.getInt(0)
        cursor.close()
        return itemId
    }

    fun getDesignId(color: Int, item: Int): Int {
        val db = this.readableDatabase
        val query = "select Code from Codes where ColorID=${color} and ItemID=${item}"
        val cursor = db.rawQuery(query, null)
        if (cursor.count <= 0) {
            cursor.close()
            return -1
        }
        cursor.moveToFirst()
        val designId = cursor.getInt(0)
        cursor.close()
        return designId
    }

    fun getInventoryParts(name: String): ArrayList<Item>{
        val inventoryId = getInventoryId(name)
        val query = "select * from InventoriesParts where InventoryID = $inventoryId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        val items = ArrayList<Item>()
        if(cursor.moveToFirst()) {
            items.add(Item(cursor.getInt(0), cursor.getString(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6), cursor.getInt(7) == 1))
        }
        while(cursor.moveToNext()) {
            items.add(Item(cursor.getInt(0), cursor.getString(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6), cursor.getInt(7) == 1))
        }
        if (cursor != null && !cursor.isClosed) {
            cursor.close()
        }
        return items
    }

    private fun checkIfDesignIDExists(colorId: Int, itemId: Int): Boolean {
        val db = this.readableDatabase
        val query = "select Code from Codes where ColorID=$colorId and ItemID=$itemId"
        val cursor = db.rawQuery(query, null)
        if (cursor.count <= 0) {
            cursor.close()
            return false
        }
        if (cursor != null && !cursor.isClosed) {
            cursor.close()
        }
        return true
    }

    fun getItemsDesignIds(items: ArrayList<Item>): ArrayList<Item>{
        items.forEach {
            if (checkIfDesignIDExists(it.colorCode!!, it.itemId!!)) {
                val query = "select Code from Codes where ColorID=${it.colorCode} and ItemID=${it.itemId}"
                val db = this.readableDatabase
                val cursor = db.rawQuery(query, null)
                if(cursor.moveToFirst()) {
                    it.designId = cursor.getInt(0)
                }
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
            }
        }
        return items
    }

    fun getItemsIds(items: ArrayList<Item>): ArrayList<Item>{
        items.forEach {
            val query = "select _id from Parts where Code=\"${it.code}\""
            val db = this.readableDatabase
            val cursor = db.rawQuery(query, null)
            if(cursor.moveToFirst()) {
                it.itemId = cursor.getInt(0)
            } else {
                it.itemId = -9
            }
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
        return items
    }

    fun getItemsNames(items: ArrayList<Item>): ArrayList<Item>{
        items.forEach {
            val query = "select Name from Parts where id=${it.itemId}"
            val db = this.readableDatabase
            val cursor = db.rawQuery(query, null)
            if(cursor.moveToFirst()) {
                it.name = cursor.getString(0)
            }
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
        return items
    }

    fun getColorNames(items: ArrayList<Item>): ArrayList<Item>{
        items.forEach{
            val query = "select Name from Colors where code=\"${it.colorCode}\""
            val db = this.readableDatabase
            val cursor = db.rawQuery(query, null)
            if(cursor.moveToFirst()) {
                it.colorName = cursor.getString(0)
                Log.e("tag", it.colorName)
            }
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
        return items
    }

    fun updateInventoryDate(name: String){
        val inventoryId: Int = getInventoryId(name)
        val db = this.writableDatabase
        db.beginTransaction()
        val query = "update Inventories set LastAccessed=" + System.currentTimeMillis()/1000 + " where id=" + inventoryId + ";"
        writableDatabase.execSQL(query)
        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
    }

    fun updateQuantity(items: ArrayList<Item>?, name: String){
        if (items != null) {
            items.forEach {
                val id: Int = getInventoryId(name)
                val db = this.writableDatabase
                db.beginTransaction()
                val query = "update InventoriesParts set QuantityInStore=" + it.quantityInStore + " where InventoryID=" + id + " and id=" + it.id+ ";"
                writableDatabase.execSQL(query)
                writableDatabase.setTransactionSuccessful()
                writableDatabase.endTransaction()
            }
        }
    }

    fun getItemImage(item: Item): Item {
        val query = "select Image from Codes where Code=" + item.designId + ";"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        val blob: ByteArray?
        if (cursor.moveToFirst()) {
            blob = cursor.getBlob(0)
            if (blob != null) {
                item.image = BitmapFactory.decodeByteArray(blob, 0, blob.size)
                Log.i("INFO", item.code + item.colorName + "pobieranie obrazu z bazy")
            }
        }
        if (cursor != null && !cursor.isClosed) {
            cursor.close()
        }
        return item
    }

    fun checkIfImageInDatabase(designId: Int): Boolean{
        val query = "select Image from Codes where Code=" + designId + ";"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        val blob: ByteArray?
        if (cursor.moveToFirst()) {
            blob = cursor.getBlob(0)
            if (blob == null) {
                return false
            }
        }
        if (cursor != null && !cursor.isClosed) {
            cursor.close()
        }
        return true
    }

    fun saveImageToDatabase(designId: Int, image:ContentValues){
        val db = writableDatabase
        db.beginTransaction()
        val selection = "Code=" + designId + ";"
        db.update("CODES", image, selection, null)
        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()
    }

    inner class DownloadImage(private var colorName: String, private var code: String, private var designId: Int, private var url: String): AsyncTask<String, Int, String>() {
        override fun doInBackground(vararg params: String?): String {
            try {
                BufferedInputStream(URL(url).content as InputStream).use {
                    Log.i("link", url)
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
                    saveImageToDatabase(designId, blobValues)
                }
            } catch (e: IOException) {
                try {
                    url = "http://img.bricklink.com/P/" + colorName + "/" + code + ".gif"
                    Log.i("link", url)
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
                        saveImageToDatabase(designId, blobValues)
                    }
                } catch (e: IOException) {
                    try {
                        url = "https://www.bricklink.com/PL/" + code + ".jpg"
                        Log.i("link", url)
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
                            saveImageToDatabase(designId, blobValues)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        return "IOException"
                    }
                }
            }
            Log.e("XDDDDDD", "POBRANO COS TAM")
            return "success"
        }
    }
}