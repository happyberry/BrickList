package com.example.bricklist

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.internal.ContextUtils.getActivity

import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    //var database:Database? = null
    var url = ""
    var archived = false

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        //this.database = Database(this)
        val database = Database(this@MainActivity, null, null, 1)
        try {
            database.createDatabaseIfAbsent()
        } catch (e: IOException) {
            throw Error("Creating database error")
        }
        /*try {
            database.openDatabase()
        } catch (e: Exception) {
            throw Error("Open database error")
        }*/
        var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        archived = prefs.getBoolean("display", false)
        url = prefs.getString("url", "http://fcds.cs.put.poznan.pl/MyWeb/BL/").toString()
        /////////////////////////-------------------------------------------------------
        //Toast.makeText(getActivity(this), archived.toString() + url, Toast.LENGTH_LONG).show()
        /////////////////////////////---------------------------------------------------
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
        /////////////////////////-------------------------------------------------------
        Toast.makeText(getActivity(this), archived.toString() + url, Toast.LENGTH_LONG).show()
        /////////////////////////////---------------------------------------------------
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
            textView.textSize = 20F
            tableRow.addView(textView)

            tableRow.setOnClickListener{
                val projectNameTextView: TextView = tableRow.getChildAt(0) as TextView
                val projectName: String = projectNameTextView.text.toString()
                val i = Intent(baseContext, ProjectActivity::class.java)
                i.putExtra("projectName", projectName)
                Log.i("tag", projectName)
                startActivity(i)
            }
            tableLayout.addView(tableRow)
        }
    }

    fun startAddNew(v: View) {
        val intent = Intent(this, NewProjectActivity::class.java).apply {}
        startActivity(intent)
    }
}
