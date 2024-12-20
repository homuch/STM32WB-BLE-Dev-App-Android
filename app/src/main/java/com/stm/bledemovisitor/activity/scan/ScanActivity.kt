package com.stm.bledemovisitor.activity.scan

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.stm.bledemovisitor.BuildConfig
import com.stm.bledemovisitor.R
import com.stm.bledemovisitor.activity.connection.ConnectionActivity
import com.stm.bledemovisitor.activity.scan.fragment.AlertDialogFragment
import com.stm.bledemovisitor.activity.scan.fragment.DeviceInfoFragment
import com.stm.bledemovisitor.activity.scan.fragment.RSSIFilterFragment
import com.stm.bledemovisitor.ble.BLEManager
import com.stm.bledemovisitor.ble.BLEManager.bAdapter
import com.stm.bledemovisitor.ble.ENABLE_BLUETOOTH_REQUEST_CODE
import com.stm.bledemovisitor.databinding.ActivityScanBinding

const val visitor_mode = true

class ScanActivity : AppCompatActivity(), ScanAdapter.Delegate, ScanInterface {

    private lateinit var binding: ActivityScanBinding
    private var scanItem: MenuItem? = null
    private var alertDialog: AlertDialogFragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan)

        setSupportActionBar(binding.toolbar)
        setupRecyclerView()

        BLEManager.scanInterface = this
        BLEManager.startScan(this)
    }

    override fun onResume() {
        super.onResume()
        BLEManager.startScan(this)
        scanItem?.setIcon(R.drawable.ic_play)
        if (!bAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onStop() {
        super.onStop()
        BLEManager.stopScan()
        scanItem?.setIcon(R.drawable.ic_play)
    }

    /** Permission & Bluetooth Requests */

    // Prompt to Enable BT
    override fun promptEnableBluetooth() {
        if(!bAdapter.isEnabled){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            ActivityCompat.startActivityForResult(
                this, enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE, null
            )
        }
    }

    // Request Runtime Permissions (Based on Android Version)
    @SuppressLint("ObsoleteSdkInt")
    override fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    // Rerequest Permissions if Not Given by User (Limit 2)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (BLEManager.hasPermissions(this)) {
            BLEManager.startScan(this)
        } else {
            requestPermissions()
        }
    }

    /** Toolbar Menu */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)

        // Get Current App Version
        menu.findItem(R.id.appVersionItem).apply {
            title = "$title ${BuildConfig.VERSION_NAME}"
        }

        val filterItem = menu.findItem(R.id.rssiFilterItem)
        filterItem?.isVisible = false

        val deviceInfoItem = menu.findItem(R.id.deviceInfoItem)
        deviceInfoItem?.isVisible = false

        val appVersionItem = menu.findItem(R.id.appVersionItem)
        appVersionItem?.isVisible = false


        scanItem = menu.findItem(R.id.scanItem)
        scanItem?.isVisible = false
        val item = menu.findItem(R.id.searchItem)
        item?.isVisible = false
        val searchView = item?.actionView as SearchView

        // Search Item on toolbar expanded/collapsed
        val expandListener = object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                BLEManager.deviceNameFilter = ""
                return true
            }
        }
        item.setOnActionExpandListener(expandListener)

        // Text entered into searchView on toolbar
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                BLEManager.scanAdapter?.filter(newText, "name")
                return true
            }
        })

        return true
    }

    // Item on Toolbar Selected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.scanItem -> {
                if (BLEManager.isScanning) {
                    BLEManager.stopScan()
                    item.setIcon(R.drawable.ic_play)
                } else {
                    BLEManager.startScan(this)
                    item.setIcon(R.drawable.ic_pause)
                }
            }
            R.id.rssiFilterItem -> {
                RSSIFilterFragment().show(supportFragmentManager, "rssiFilterFragment")
            }
            R.id.deviceInfoItem -> {
                DeviceInfoFragment().show(supportFragmentManager, "deviceInfoFragment")
            }
//            R.id.alertDialogFragment -> {
//                AlertDialogFragment().show(supportFragmentManager, "alertDialogFragment")
//            }
        }

        return false
    }

    /** Recycler View */

    // Sets Up the Recycler View for BLE Scan List
    private fun setupRecyclerView() {
        // Create & Set Adapter
        BLEManager.scanAdapter = ScanAdapter(BLEManager.scanResults, this, applicationContext, BLEManager.whiteListAddress)

        binding.scanResultsRecyclerView.apply {
            adapter = BLEManager.scanAdapter
            layoutManager = LinearLayoutManager(
                this@ScanActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        // Turns Off Update Animation
        val animator = binding.scanResultsRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    // Connect Button Clicked
    override fun onConnectButtonClick(result: ScanResult) {
        if (BLEManager.isScanning) {
            scanItem?.setIcon(R.drawable.ic_play)
        }

        BLEManager.connect(result, this)
    }

    // Item Clicked (Show Advertising Data)
    override fun onItemClick(dialog: DialogFragment) {
        dialog.show(supportFragmentManager, "advertisingDataFragment")
    }

    override fun showAlertDialog(title: String, msg: String, dialogListener: AlertDialogFragment.DialogListener) {
        if(alertDialog == null || !alertDialog!!.isVisible){
            alertDialog = AlertDialogFragment.newInstance(title, msg, dialogListener)
            alertDialog!!.show(supportFragmentManager, "alertDialog")
        } else {
            // ignore the request
        }
    }

    /** Helper Functions */

    // Go to ConnectionInterface Activity
    override fun startIntent() {
        Intent(this@ScanActivity, ConnectionActivity::class.java).apply {
            startActivity(this)
        }
    }

    override fun startToast(message: String) {
//        runOnUiThread {
//            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//        }
    }
}