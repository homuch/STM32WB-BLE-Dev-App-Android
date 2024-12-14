package com.stm.bledemovisitor.activity.connection

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.stm.bledemovisitor.R
import com.stm.bledemovisitor.ble.BLEManager
import com.stm.bledemovisitor.ble.BLEManager.isIndicatable
import com.stm.bledemovisitor.ble.BLEManager.isNotifiable
import com.stm.bledemovisitor.ble.BLEManager.isReadable
import com.stm.bledemovisitor.ble.BLEManager.isWritable
import com.stm.bledemovisitor.ble.BLEManager.isWritableWithoutResponse
import com.stm.bledemovisitor.ble.Characteristic
import com.stm.bledemovisitor.ble.Service
import com.stm.bledemovisitor.databinding.ActivityConnectionBinding

class ConnectionActivity : AppCompatActivity(), ConnectionInterface {

    private lateinit var binding: ActivityConnectionBinding
    private lateinit var connectionAdapter: ConnectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_connection)
        connectionAdapter = ConnectionAdapter()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        BLEManager.connectionInterface = this
        setupRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        BLEManager.disconnect()
    }

    /** Recycler View */

    private fun setupRecyclerView() {
        binding.connectionRecyclerView.apply {
            adapter = connectionAdapter
            layoutManager = LinearLayoutManager(
                this@ConnectionActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
            setHasFixedSize(true)
        }

        // Turns Off Update Animation
        val animator = binding.connectionRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    /** Connection Interface */

    // Adds Discovered Services & Characteristics
    override fun addDiscoveredItems() {
        runOnUiThread {
            val items: ArrayList<ConnectionItem> = arrayListOf()

            // Setup List of ConnectionInterface Items (Services & Characteristics)
            BLEManager.bGatt?.services?.forEach { service ->
                // Add Service
                items.add(
                    ConnectionItem(
                        Service.getServiceName(service.uuid.toString()),
                        "Service",
                        service
                    )
                )

                // Add Characteristics
                service.characteristics.forEach { characteristic ->
                    items.add(
                        ConnectionItem(
                            Characteristic.getCharacteristicName(
                                characteristic.uuid.toString(),
                                service.uuid.toString()
                            ),
                            "Characteristic",
                            service,
                            characteristic,
                            characteristic.isReadable(),
                            characteristic.isWritable(),
                            characteristic.isWritableWithoutResponse(),
                            characteristic.isNotifiable(),
                            characteristic.isIndicatable()
                        )
                    )
                }
            }

            connectionAdapter.addItemList(items)
        }
    }

    override fun valueUpdated(characteristic: BluetoothGattCharacteristic) {
        connectionAdapter.valueUpdated(characteristic)
    }

    override fun finishActivity() {
        finish()
    }
}