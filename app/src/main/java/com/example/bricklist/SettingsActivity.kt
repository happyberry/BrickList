package com.example.bricklist

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


class SettingsActivity : AppCompatActivity() {

    var url = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        url = prefs.getString(url, "http://fcds.cs.put.poznan.pl/MyWeb/BL/").toString()
        val i = Intent(this, MainActivity::class.java)
        i.putExtra("url", url)
        super.onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        url = prefs.getString(url, "").toString()
        val i = Intent(this, MainActivity::class.java)
        i.putExtra("url", url)
        super.onBackPressed()
    }

}