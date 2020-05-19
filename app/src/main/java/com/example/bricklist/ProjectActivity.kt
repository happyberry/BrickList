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
        //val partsListView: ListView = this.findViewById(R.id.partsListView)
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
        val database = Database(this@ProjectActivity, null, null, 1)
        tableLayout.removeAllViews()
        for (i in itemList) {
            val tableRowName: TableRow = TableRow(this)
            val tableRowColor: TableRow = TableRow(this)
            val tableRowQt: TableRow = TableRow(this)
            val tableRowButtons: TableRow = TableRow(this)
            val itemName: TextView = TextView(this)
            val itemColor: TextView = TextView(this)
            val itemQuantity: TextView = TextView(this)
            itemName.text = i.name
            tableRowName.addView(itemName)
            itemColor.text = i.colorName
            itemQuantity.text = i.quantityInStore.toString() + " out of " + i.quantityInSet.toString() + "\n"
            tableRowColor.addView(itemColor)
            tableRowQt.addView(itemQuantity)
            tableLayout.addView(tableRowName)
            tableLayout.addView(tableRowColor)
            tableLayout.addView(tableRowQt)

            var linearLayout = LinearLayout(this)
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.weightSum = 2f
            val plusButton: Button = createButton("+")
            plusButton.setText("+")
            plusButton.setOnClickListener{
                i.quantityInStore = i.quantityInStore?.plus(1)
                val quantity: TextView = tableRowQt.getChildAt(0) as TextView
                quantity.text = i.quantityInStore.toString() + " out of " + i.quantityInSet.toString() + "\n"
                this.itemList = itemList
            }
            val minusButton: Button = createButton("-")
            minusButton.setText("-")
            minusButton.setOnClickListener{
                if (i.quantityInStore != 0) {
                    i.quantityInStore = i.quantityInStore?.minus(1)
                    val quantity: TextView = tableRowQt.getChildAt(0) as TextView
                    quantity.text = i.quantityInStore.toString() + " out of " + i.quantityInSet.toString() + "\n"
                    this.itemList = itemList
                } else {
                    Toast.makeText(this, "The quantity cannot be less than zero", Toast.LENGTH_LONG).show()
                }
            }
            tableLayout.addView(tableRowButtons)
            tableRowButtons.addView(linearLayout)
            linearLayout.addView(plusButton)
            linearLayout.addView(minusButton)

        }
        return itemList
    }

    inner class PartsListViewAdapter(context: Context, private val inventoryPartsList: ArrayList<Item>):
        BaseAdapter() {

        private var inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getCount(): Int {
            return inventoryPartsList.size
        }

        override fun getItem(position: Int): Any {
            return inventoryPartsList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView = inflater.inflate(R.layout.project, parent, false)
            val idTextView = rowView.findViewById(R.id.ItemId) as TextView
            val colorTextView = rowView.findViewById(R.id.Color) as TextView
            val nameTextView = rowView.findViewById(R.id.Name) as TextView
            val quantityInSetTextView = rowView.findViewById(R.id.QuantityInSet) as TextView
            val quantityInStoreNumberPicker = rowView.findViewById(R.id.QuantityInStore) as NumberPicker
            //val itemImage = rowView.findViewById(R.id.ItemImage) as ImageView

            val item = getItem(position) as Item

            quantityInStoreNumberPicker.minValue = 0
            quantityInStoreNumberPicker.maxValue = 100

            idTextView.text = item.itemId.toString()
            nameTextView.text = item.name
            colorTextView.text = item.colorName
            quantityInSetTextView.text = item.quantityInSet.toString()
            quantityInStoreNumberPicker.value = item.quantityInStore!!
            //itemImage.setImageBitmap(item.image)

            quantityInStoreNumberPicker.setOnValueChangedListener{_, _, newVal ->
                inventoryPartsList[position].quantityInStore = newVal
                quantityInStoreNumberPicker.tag = newVal
                if (item.quantityInSet!! <= newVal)
                    rowView.setBackgroundColor(Color.LTGRAY)
                else
                    rowView.setBackgroundColor(Color.TRANSPARENT)
            }
            if (item.quantityInSet!! <= item.quantityInStore!!)
                rowView.setBackgroundColor(Color.LTGRAY)
            else
                rowView.setBackgroundColor(Color.TRANSPARENT)

            return rowView
        }
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
