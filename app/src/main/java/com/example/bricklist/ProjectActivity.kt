package com.example.bricklist

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.internal.ContextUtils.getActivity
import kotlinx.android.synthetic.main.activity_project.*
import kotlinx.android.synthetic.main.content_project.*
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


class ProjectActivity : AppCompatActivity() {

    var projectName: String = ""
    var itemList: ArrayList<Item>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)
        setSupportActionBar(toolbar)
        this.projectName = intent.extras?.getString("projectName").toString()
        Log.i("tag", projectName)
        this.findViewById<TextView>(R.id.activityTitle).setText(projectName)
        val database = Database(this@ProjectActivity, null, null, 1)
        database.updateInventoryDate(projectName)
        var partsList = database.getInventoryParts(projectName)
        partsList = database.getItemsDesignIds(partsList)
        partsList = database.getItemsNames(partsList)
        partsList = database.getColorNames(partsList)
        partsList = fillPartsList(partsList)
        itemList = partsList
        //TODO update parts quantity I PO WYJSCIU Z TEJ AKTYWNOSCI TEZ!!!!!!!
    }

    override fun onBackPressed() {
        val database = Database(this@ProjectActivity, null, null, 1)
        database.updateQuantity(itemList, projectName)
        super.onBackPressed()
    }

    fun fillPartsList(itemList: ArrayList<Item>): ArrayList<Item> {
        listView.removeAllViews()
        var number = 10
        for (i in itemList) {

            var linearLayoutInfo = LinearLayout(this)
            listView.addView(linearLayoutInfo)
            linearLayoutInfo.setOrientation(LinearLayout.HORIZONTAL);
            linearLayoutInfo.weightSum = 2f
            var image  = ImageView(this)
            image.id = number
            number = number + 10
            //image.setImageBitmap(i.image)
            DownloadImage(i.colorCode!!, i.itemId!!, "https://www.lego.com/service/bricks/5/2/" + i.designId.toString(), image.id).execute()
            image.minimumHeight = 350
            image.minimumWidth = 350
            //image.maxHeight = 350
            //image.maxWidth=350
            /*image.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )*/
            val itemInfo = TextView(this)
            itemInfo.text = i.name + "\n" + i.colorName + "\n" + i.quantityInStore.toString() + " out of " + i.quantityInSet.toString() + "\n"
            linearLayoutInfo.setOrientation(LinearLayout.HORIZONTAL);
            itemInfo.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            linearLayoutInfo.addView(image)
            linearLayoutInfo.addView(itemInfo)


            var linearLayoutButtons = LinearLayout(this)
            listView.addView(linearLayoutButtons)
            linearLayoutButtons.setOrientation(LinearLayout.HORIZONTAL);
            linearLayoutButtons.weightSum = 2f
            val plusButton: Button = createButton("+")
            val minusButton: Button = createButton("-")

            plusButton.setOnClickListener{
                if (i.quantityInSet != i.quantityInStore) {
                    i.quantityInStore = i.quantityInStore?.plus(1)
                    val info: TextView = linearLayoutInfo.getChildAt(1) as TextView
                    info.text =
                        i.name + "\n" + i.colorName + "\n" + i.quantityInStore.toString() + " out of " + i.quantityInSet.toString() + "\n"
                    this.itemList = itemList
                } else {
                    Toast.makeText(this, "All elements already found", Toast.LENGTH_LONG).show()
                }
            }

            minusButton.setOnClickListener{
                if (i.quantityInStore != 0) {
                    i.quantityInStore = i.quantityInStore?.minus(1)
                    val quantity: TextView = linearLayoutInfo.getChildAt(1) as TextView
                    quantity.text = i.name + "\n" + i.colorName + "\n" + i.quantityInStore.toString() + " out of " + i.quantityInSet.toString() + "\n"
                    this.itemList = itemList
                } else {
                    Toast.makeText(this, "The quantity cannot be less than zero", Toast.LENGTH_LONG).show()
                }
            }

            linearLayoutButtons.addView(plusButton)
            linearLayoutButtons.addView(minusButton)

        }
        return itemList
    }

    fun createButton(text: String): Button{
        val button = Button(this)
        button.text = text
        button.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        button.textSize = 25F
        button.setBackgroundColor(Color.parseColor("#FFFFFF"))
        //button.maxWidth = 150
        return button
    }

    fun export(v: View) {
        val filename = projectName + ".xml"
        //TODO: filename popup
        try {
            exportToXML(filename)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "An error occured during export. Try again", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Export finished succesfully", Toast.LENGTH_LONG).show()
    }

    fun exportToXML(filename: String) {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@ProjectActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = documentBuilder.newDocument()
        val rootElement = document.createElement("INVENTORY")
        itemList!!.forEach {
            val item = document.createElement("ITEM")
            val itemType = document.createElement("ITEMTYPE")
            val id = document.createElement("ITEMID")
            val color = document.createElement("COLOR")
            val quantity =document.createElement("QTYFIELD")

            itemType.textContent = it.itemType
            item.appendChild(itemType)
            id.textContent = it.itemId.toString()
            item.appendChild(id)
            color.textContent = it.colorCode.toString()
            item.appendChild(color)
            val lack = it.quantityInSet!!.minus(it.quantityInStore!!)
            if (lack != 0) {
                quantity.textContent = lack.toString()
                item.appendChild(quantity)
            }
            rootElement.appendChild(item)
        }
        document.appendChild(rootElement)
        val path = this.filesDir
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        //val file = File(path, filename)
        val file = File(Environment.getExternalStorageDirectory(), filename)
        transformer.transform(DOMSource(document), StreamResult(file))
    }

    inner class DownloadImage(private var colorCode: Int, private var code: String, private var url: String, private var imageId: Int): AsyncTask<String, Int, Pair<Int, Drawable>>() {
        override fun doInBackground(vararg params: String?): Pair<Int, Drawable>? {
            try {
                val image = Drawable.createFromStream(BufferedInputStream(URL(url).content as InputStream), "src name")
                return Pair(imageId, image)
            } catch (e: IOException) {
                try {
                    url = "http://img.bricklink.com/P/" + colorCode + "/" + code + ".gif"
                    Log.i("link", url)
                    val image = Drawable.createFromStream(BufferedInputStream(URL(url).content as InputStream), "src name")
                    return Pair(imageId, image)
                } catch (e: IOException) {
                    try {
                        url = "https://www.bricklink.com/PL/" + code + ".jpg"
                        Log.i("link", url)
                        val image = Drawable.createFromStream(BufferedInputStream(URL(url).content as InputStream), "src name")
                        return Pair(imageId, image)

                    } catch (e: IOException) {
                        e.printStackTrace()
                        return null
                    }
                }
            }
            return null
        }

        override fun onPostExecute(result: Pair<Int, Drawable>?) {
            if (result != null){
                val image = findViewById<ImageView>(result.first)
                image.setImageDrawable(result.second)
            }
        }

    }



}
