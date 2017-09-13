package com.threshold.rxupdownloader

import android.Manifest
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.threshold.logger.PrettyLogger
import com.threshold.logger.debug
import com.threshold.logger.info
import com.threshold.logger.util.Utils
import com.threshold.logger.warn

import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(),PrettyLogger,EasyPermissions.PermissionCallbacks {
    companion object {
        const val RC_STORAGE = 100
        val REQUIRE_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        requestWriteExternalStoragePermission()
    }

    @AfterPermissionGranted(RC_STORAGE)
    private fun requestWriteExternalStoragePermission() {
        if (EasyPermissions.hasPermissions(this, *REQUIRE_PERMISSIONS)) {
            debug { "Got write external storage permission" }
        } else {
            debug { "No permissions, now we request it." }
            EasyPermissions.requestPermissions(this,"We need write external storage to download or upload file.", RC_STORAGE,*REQUIRE_PERMISSIONS)
        }
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
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        info { "PermissionGranted: [$requestCode] : ${Utils.toString(perms)}" }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
        debug { "PermissionDenied: [$requestCode] : ${Utils.toString(perms)}" }
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms!!)) {
            warn("You permanently denied some permissions,which will cause we can't download or upload file, you can change it at app settings.")
            AppSettingsDialog.Builder(this).build().show()
        }
    }

}
