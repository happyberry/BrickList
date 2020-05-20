package com.example.bricklist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_project.toolbar

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
        for (i: Int in 0 until partsList.size) {
            partsList[i] = database.getItemImage(partsList[i])
        }
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
        tableLayout.removeAllViews()
        for (i in itemList) {
            val tableRowInfo = TableRow(this)
            val tableRowButtons= TableRow(this)
            val itemInfo = TextView(this)
            var linearLayoutInfo = LinearLayout(this)
            var image  = ImageView(this)
            image.minimumHeight = 250
            image.minimumWidth = 250
            image.setImageBitmap(i.image)
            image.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            itemInfo.text = i.name + "\n" + i.colorName + "\n" + i.quantityInStore.toString() + " out of " + i.quantityInSet.toString() + "\n"
            linearLayoutInfo.setOrientation(LinearLayout.HORIZONTAL);
            linearLayoutInfo.weightSum = 2f
            tableLayout.addView(tableRowInfo)
            tableRowInfo.addView(linearLayoutInfo)
            linearLayoutInfo.addView(image)
            linearLayoutInfo.addView(itemInfo)


            var linearLayoutButtons = LinearLayout(this)
            val plusButton: Button = createButton("+")
            val minusButton: Button = createButton("-")

            linearLayoutButtons.setOrientation(LinearLayout.HORIZONTAL);
            linearLayoutButtons.weightSum = 2f

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
            tableLayout.addView(tableRowButtons)
            tableRowButtons.addView(linearLayoutButtons)
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


}
