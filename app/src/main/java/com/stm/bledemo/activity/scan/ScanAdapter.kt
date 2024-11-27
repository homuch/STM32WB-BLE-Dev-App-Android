package com.stm.bledemo.activity.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.stm.bledemo.R
import com.stm.bledemo.activity.scan.fragment.AdvertisingDataFragment
import com.stm.bledemo.ble.BLEManager
import com.stm.bledemo.databinding.RowScanResultBinding
import java.util.Locale
import java.util.UUID
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.random.Random
import kotlin.math.pow

@SuppressLint("NotifyDataSetChanged", "MissingPermission")
class ScanAdapter (
    private val items: List<ScanResult>,
    private val delegate: Delegate
) : RecyclerView.Adapter<ScanAdapter.ViewHolder>() {

    private val itemsCopy: ArrayList<ScanResult> = arrayListOf()
    private val artworkTitles = listOf(
        "Starry Night",
        "Mona Lisa",
        "The Scream",
        "The Persistence of Memory",
        "Guernica",
        "American Gothic",
        "The Kiss",
        "The Birth of Venus",
        "Girl with a Pearl Earring",
        "The Great Wave off Kanagawa"
    )
    private val imageResources = listOf(
        R.drawable.ic_paint,
        R.drawable.ic_paint2,
        R.drawable.ic_statue,
        R.drawable.ic_masks,
        R.drawable.ic_abstract,
        R.drawable.ic_origami
    )
    private val colorResources = listOf(R.color.st_pink, R.color.teal_200, R.color.purple_200, R.color.purple_700, R.color.green, R.color.orange)

    interface Delegate {
        fun onConnectButtonClick(result: ScanResult)
        fun onItemClick(dialog: DialogFragment)
    }

    inner class ViewHolder(val binding: RowScanResultBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.connectButton.setOnClickListener {
                val result = items[bindingAdapterPosition]
                delegate.onConnectButtonClick(result)
            }
            itemView.setOnClickListener {
                val result = items[bindingAdapterPosition]
                val dialog = AdvertisingDataFragment(result)
                delegate.onItemClick(dialog)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<RowScanResultBinding>(
            inflater,
            R.layout.row_scan_result,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = items[position]
        // Generating a random index based on the device address for consistent image and color
        val randomNameIndex = abs(result.device.address.hashCode()) % artworkTitles.size
        val randomIndex = abs(result.device.address.hashCode()) % imageResources.size

        with(holder.binding) {
            deviceName.text = result.device.name ?: artworkTitles[randomNameIndex]
            macAddress.text = result.device.address
            signalStrength.text = "${result.rssi} dBm"
            val distance = 10.0.pow((-15 - result.rssi) / 30.0)
            estiDist.text = String.format(Locale.getDefault(), "%.2f m", distance / 100.0)
            bluetoothIcon.setImageResource(imageResources[randomIndex])
            bluetoothIcon.imageTintList = ContextCompat.getColorStateList(holder.itemView.context, colorResources[randomIndex])

//            connectButton.visibility = if (!result.isConnectable) View.GONE else View.VISIBLE
            connectButton.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    // Filter Recycler View by given text
    fun filter(value: String, type:String) {
        if (value.isNotEmpty()) {
            itemsCopy.clear()
            itemsCopy.addAll(items)

            when (type) {
                "name" -> BLEManager.deviceNameFilter = value
                "rssi" -> BLEManager.deviceRSSIFilter = value
            }
            BLEManager.scanResults.clear()

            for (item in itemsCopy) {
                if (filterCompare(item, value, type)) {
                    BLEManager.scanResults.add(item)
                }
            }

            notifyDataSetChanged()
        }
    }

    fun filterCompare(item: ScanResult, value: String, type: String): Boolean {
        if (value.isEmpty()) return true

        return if (type == "name") {
            item.device.name != null && item.device.name.uppercase().contains(value.uppercase())
        } else {
            item.rssi >= value.toInt()
        }
    }
}