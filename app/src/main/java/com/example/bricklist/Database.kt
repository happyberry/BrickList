package com.example.bricklist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import org.w3c.dom.NodeList
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Database(var context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int): SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object{
        private val DATABASE_NAME = "BrickList.db"
        private val DATABASE_VERSION = 1
    }
    var connection: SQLiteDatabase? = null
    var archived = false
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
        } catch (e: SQLiteException) {}
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
            query = "select Name from Inventories order by LastAccessed"
        } else {
            query = "select Name from Inventories where Active=1 order by LastAccessed"
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
        return inventoriesList
    }

    fun addInventory(name: String) {
        val db = this.writableDatabase
        var values = ContentValues()
        values.put("Name", name)
        values.put("Active", 1)
        values.put("LastAccessed", System.currentTimeMillis()/1000)
        db.insert("Inventories", null, values)
        db.close()
    }

    fun addInventoriesParts(inventoryId: Int, items: NodeList): String{
        val db = this.writableDatabase
        var lacking = ""
        for (i in 0 until items.length){
            val properties = items.item(i).childNodes
            val itemType = getTypeId(properties.item(1).textContent.toString().trim())
            var values = ContentValues()
            values.put("InventoryID", inventoryId)
            values.put("TypeID", itemType)
            values.put("ItemID", properties.item(3).textContent.toString().trim())
            values.put("QuantityInSet", properties.item(5).textContent.toString().trim())
            values.put("QuantityInStore", 0)
            values.put("ColorID", getColorIdByCode(properties.item(7).textContent.toString().trim().toInt()))
            if (itemType == -1) {
                val colorname = getColorByCode(properties.item(7).textContent.toString().trim().toInt())
                lacking += itemType.toString() + " " + colorname + ", "
                //TODO zwróć listę klocków których nie ma do wyświetlenia z poziomu new project activity czy coś
            } else {
                db.insert("InventoriesParts", null, values)
            }
        }
        if (lacking != "") {
            lacking = lacking.substring(0, lacking.length - 2)
        }
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
        val query = "select * from Colors where Code = code"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val colorName = cursor.getString(2)
        cursor.close()
        return colorName
    }

    fun getColorIdByCode(code: Int): Int {
        val db = this.readableDatabase
        val query = "select * from Colors where Code = code"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val colorId = cursor.getInt(0)
        cursor.close()
        return colorId
    }
}