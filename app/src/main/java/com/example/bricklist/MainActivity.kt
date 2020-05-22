package com.example.bricklist

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    var url = ""
    var archived = false
    var name = "BrickList.db"
    var path = "/data/data/com.example.bricklist/databases/"
    var tips = true

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        try {
            createDatabaseIfAbsent()
        } catch (e: IOException) {
            throw Error("Creating database error")
        }
        var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        archived = prefs.getBoolean("display", false)
        url = prefs.getString("url", "http://fcds.cs.put.poznan.pl/MyWeb/BL/").toString()
        tips = prefs.getBoolean("tips", true)
        if (tips) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Hello!")
            builder.setMessage("If you want to archive inventory try long-pressing it's name. If you want to add new one tap the plus button in the right bottom corner")
            builder.setPositiveButton("GOT IT",
                DialogInterface.OnClickListener { dialog, which ->
                    val editor: Editor = prefs.edit()
                    editor.putBoolean("tips", false)
                    editor.commit()
                    dialog.dismiss()
                })

            builder.setNegativeButton("Show this message next time",
                DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
            builder.show()
        }
        refillProjectList()
    }

    fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {}
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //val intent = Intent(this, SettingsActivity::class.java).apply {}
        //startActivity(intent)
        openSettings()
        return true
        /*return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }*/
    }

    @SuppressLint("RestrictedApi")
    override fun onResume() {
        var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        archived = prefs.getBoolean("display", false)
        url = prefs.getString("url", "fcds.cs.put.poznan.pl/MyWeb/BL/").toString()
        refillProjectList()
        super.onResume()
    }

    fun refillProjectList() {
        val database = Database(this@MainActivity, null, null, 1)
        var projectsList: ArrayList<String> = database.getProjects(archived)
        tableLayout.removeAllViews()
        for (i in projectsList) {
            val tableRow: TableRow = TableRow(this)
            val textView: TextView = TextView(this)
            textView.setText(i)
            textView.textSize = 18F
            tableRow.addView(textView)
            tableLayout.addView(tableRow)

            val view = View(this)
            tableLayout.addView(view)
            val params: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.height = 2
            params.setMargins(0,35,0,35)
            view.setBackgroundColor(Color.parseColor("#AAAAAA"))
            view.layoutParams = params


            tableRow.setOnClickListener{
                val projectNameTextView: TextView = tableRow.getChildAt(0) as TextView
                val projectName: String = projectNameTextView.text.toString()
                val i = Intent(baseContext, ProjectActivity::class.java)
                i.putExtra("projectName", projectName)
                Log.i("tag", projectName)
                startActivity(i)
            }
            tableRow.setOnLongClickListener{
                val projectNameTextView: TextView = tableRow.getChildAt(0) as TextView
                val projectName: String = projectNameTextView.text.toString()
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)

                builder.setTitle("Confirm")
                builder.setMessage("Do you want to archive this inventory?")

                builder.setPositiveButton("YES",
                    DialogInterface.OnClickListener { dialog, which ->
                        database.setProjectArchived(projectName)
                        dialog.dismiss()
                        refillProjectList()
                    })

                builder.setNegativeButton("NO",
                    DialogInterface.OnClickListener { dialog, which ->
                        // Do nothing
                        dialog.dismiss()
                    })
                builder.show()
                return@setOnLongClickListener true
            }

        }
    }

    fun startAddNew(v: View) {
        val intent = Intent(this, NewProjectActivity::class.java).apply {}
        startActivity(intent)
    }

    fun createDatabaseIfAbsent() {
        var file = File(path + name)
        var fileExists = file.exists()
        if (!fileExists) {
            try {
                val myInput = baseContext.assets.open(name)
                val outDir = File("/data/data/com.example.bricklist/", "databases")
                outDir.mkdir()
                val outFileName = path + name
                val myOutput = FileOutputStream(outFileName)
                val buffer = ByteArray(100)
                var length = myInput.read(buffer)
                while (length > 0) {
                    myOutput.write(buffer, 0, length)
                    length = myInput.read(buffer)
                }
                myOutput.flush()
                myOutput.close()
                myInput.close()
            } catch (e: IOException) {
                e.printStackTrace()
                throw Error("Error copying database")
            }
        }

    }
}
