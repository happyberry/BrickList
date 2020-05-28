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
import java.io.*
import java.net.URL

class Database(var context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int): SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object{
        private val DATABASE_NAME = "BrickList.db"
        private val DATABASE_VERSION = 1
    }
    //var name = "BrickList.db"

    override fun onCreate(db: SQLiteDatabase?){
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
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
                lacking += "\n" + properties.item(3).textContent.toString().trim() + " color:" + colorname
            } else {
                db.insert("InventoriesParts", null, values)
            }
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()
        Log.i("lacking parts", lacking)
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

    private fun getItemId(code: String): Int {
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

    private fun getTypeId(typeName: String): Int {
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

    private fun getColorByCode(code: Int): String {
        val db = this.readableDatabase
        val query = "select * from Colors where Code = ${code}"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val colorName = cursor.getString(2)
        cursor.close()
        return colorName
    }

    private fun getColorIdByCode(code: Int): Int {
        val db = this.readableDatabase
        val query = "select * from Colors where Code = ${code}"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val colorId = cursor.getInt(0)
        cursor.close()
        return colorId
    }

    fun getItemsDesignIds(items: ArrayList<Item>): ArrayList<Item>{
        items.forEach {
            val colorId = getColorIdByCode(it.colorCode!!)
            val itemId = getItemId(it.itemId.toString())
            //Log.i("DOWNLOAD", colorId.toString() + " " + itemId.toString())
            val query = "select Code from Codes where ColorID=${colorId} and ItemID=${itemId}"
            val db = this.readableDatabase
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                it.designId = cursor.getInt(0)
                //Log.i("DOWN", it.designId.toString())
            } else {
                it.designId = null
            }
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }

        }
        return items
    }

    fun getInventoryParts(name: String): ArrayList<Item>{
        val inventoryId = getInventoryId(name)
        val query = "select * from InventoriesParts where InventoryID = $inventoryId order by quantityinset - quantityinstore desc"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        val items = ArrayList<Item>()
        if(cursor.moveToFirst()) {
            items.add(Item(cursor.getInt(0), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6), cursor.getInt(7) == 1))
        }
        while(cursor.moveToNext()) {
            items.add(Item(cursor.getInt(0), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6), cursor.getInt(7) == 1))
        }
        if (cursor != null && !cursor.isClosed) {
            cursor.close()
        }
        return items
    }

    fun getItemsNames(items: ArrayList<Item>): ArrayList<Item>{
        items.forEach {
            val query = "select Name from Parts where code like \"${it.itemId}\""
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
                //Log.e("tag", it.colorName)
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

    fun setProjectArchived(projectName: String) {
        val id = getInventoryId(projectName)
        val db = writableDatabase
        db.beginTransaction()
        val cv = ContentValues()
        cv.put("active", "0")
        db.update("INVENTORIES", cv, "id="+id, null);
        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()
    }
}