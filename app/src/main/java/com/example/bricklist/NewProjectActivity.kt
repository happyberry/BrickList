package com.example.bricklist

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_new_project.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.lang.Thread.sleep
import java.net.MalformedURLException
import java.net.URL
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class NewProjectActivity : AppCompatActivity() {

    var url = ""
    var archived = false
    var setNumber = 0
    var projectName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project)
        var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        archived = prefs.getBoolean("display", false)
        url = prefs.getString("url", "http://fcds.cs.put.poznan.pl/MyWeb/BL/").toString()
    }

    fun addProject(v: View) {
        if (projectNameEditText.text.toString().trim().length == 0) return
        if (setNumberEditText.text.toString().trim().length == 0) return
        val name = projectNameEditText.text.toString()
        projectName = name
        val setnumber = setNumberEditText.text.toString()
        this.setNumber = setnumber.toInt()
        val database = Database(this@NewProjectActivity, null, null, 1)
        if (database.getInventoryId(name) != -1) {
            val toast =
                Toast.makeText(baseContext, "This project already exists.", Toast.LENGTH_LONG)
            toast.show()
            return
        } else {
            val downloadXML = DownloadXML()
            downloadXML.execute()
        }
    }

    private inner class DownloadXML() : AsyncTask<String, Int, String>() {

        override fun doInBackground(vararg params: String?): String? {
            try {
                val database = Database(this@NewProjectActivity, null, null, 1)
                val completeUrl = URL(url + setNumber + ".xml")
                //Log.e("tag", url + setNumber + ".xml")
                val documentbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
                val documentBuilder: DocumentBuilder = documentbf.newDocumentBuilder()
                val document: Document = documentBuilder.parse(InputSource(completeUrl.openStream()))
                document.documentElement.normalize()
                val items = document.getElementsByTagName("ITEM")
                database.addInventory(projectName)
                val Id = database.getInventoryId(projectName)
                val lacking = database.addInventoriesParts(Id, items)
                return lacking
            } catch (e: Exception) {
                e.printStackTrace()
                return "BAD"
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result.equals("BAD")) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this@NewProjectActivity)
                builder.setTitle("Import Failed")
                builder.setMessage("Failed on collecting set data. URL address may be wrong")
                builder.setPositiveButton("TRY AGAIN",
                    DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    })
                builder.setNegativeButton("QUIT",
                    DialogInterface.OnClickListener { dialog, which ->
                        // Do nothing
                        dialog.dismiss()
                        onBackPressed()
                    })
                builder.show()
            } else if (result != "") {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this@NewProjectActivity)
                builder.setTitle("Import Successfull")
                builder.setMessage("Elements not found in database:\n" + result)
                builder.setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                        onBackPressed()
                    })
                builder.show()
            } else {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this@NewProjectActivity)
                builder.setTitle("Import Successfull")
                builder.setMessage("Project added successfully")
                builder.setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                        onBackPressed()
                    })
                builder.show()
            }

        }
    }
}
