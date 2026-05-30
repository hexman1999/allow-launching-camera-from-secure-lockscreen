package com.example.allowcamera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.TreeSet

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val listView = findViewById<ListView>(R.id.camera_app_list)
        val statusText = findViewById<TextView>(R.id.status_text)
        val cameraApps = getCameraApps()

        if (cameraApps.isEmpty()) {
            statusText.text = getString(R.string.no_camera_apps_found)
            return
        }

        val adapter = CameraAppAdapter(this, cameraApps)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE

        val currentPkg = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PACKAGE, null)
        val currentIndex = cameraApps.indexOfFirst { it.packageName == currentPkg }
        if (currentIndex >= 0) {
            listView.setItemChecked(currentIndex, true)
            statusText.text = getString(R.string.selected_camera, cameraApps[currentIndex].label)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = cameraApps[position]
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (currentPkg == selected.packageName) {
                prefs.edit().remove(KEY_PACKAGE).apply()
                listView.setItemChecked(position, false)
                statusText.text = getString(R.string.no_app_selected)
                Toast.makeText(this, getString(R.string.using_default), Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putString(KEY_PACKAGE, selected.packageName).apply()
                statusText.text = getString(R.string.selected_camera, selected.label)
                Toast.makeText(this, getString(R.string.restart_systemui), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getCameraApps(): List<CameraAppInfo> {
        val seen = TreeSet<String>()
        val result = mutableListOf<CameraAppInfo>()
        val pm = packageManager

        val intents = listOf(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        )

        for (intent in intents) {
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (ri in activities) {
                val ai = ri.activityInfo ?: continue
                val pkg = ai.packageName
                if (pkg in seen) continue
                seen.add(pkg)
                val appInfo = ai.applicationInfo
                result.add(
                    CameraAppInfo(
                        label = appInfo.loadLabel(pm).toString(),
                        packageName = pkg,
                        icon = appInfo.loadIcon(pm)
                    )
                )
            }
        }

        return result.sortedBy { it.label }
    }

    companion object {
        const val PREFS_NAME = "camera_prefs"
        const val KEY_PACKAGE = "selected_camera_package"
    }
}

data class CameraAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

class CameraAppAdapter(
    context: Context,
    private val apps: List<CameraAppInfo>
) : ArrayAdapter<CameraAppInfo>(context, android.R.layout.simple_list_item_single_choice, apps) {
    override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
        val view = super.getView(position, convertView, parent) as android.widget.TextView
        val app = apps[position]
        view.text = app.label
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(app.icon, null, null, null)
        view.compoundDrawablePadding = 24
        return view
    }
}
