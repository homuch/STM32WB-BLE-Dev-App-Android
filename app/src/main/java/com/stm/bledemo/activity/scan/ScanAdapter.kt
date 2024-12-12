package com.stm.bledemo.activity.scan

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.recyclerview.widget.RecyclerView
import com.stm.bledemo.R
import com.stm.bledemo.activity.scan.fragment.AdvertisingDataFragment
import com.stm.bledemo.ble.BLEManager
import com.stm.bledemo.databinding.RowScanResultBinding
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.*
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow


@SuppressLint("NotifyDataSetChanged", "MissingPermission")
class ScanAdapter (
    private val items: List<ScanResult>,
    private val delegate: Delegate,
    private val context: Context
) : RecyclerView.Adapter<ScanAdapter.ViewHolder>() {

    private val itemsCopy: ArrayList<ScanResult> = arrayListOf()
    private val rssiHistory: MutableMap<String, MutableList<Pair<Int, Instant>>> = mutableMapOf()
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
    private val stmTxPower = -44.20289855
    private val stmRSSIN = 4.830917874
    private val averageFilterSize = 2000
    private var isAudioPlaying = false
    private var audioPlayJob: Job? = null
    private var lastPlayTime = 0L
    private var shouldStopAudio = false

    private val filename = "rssi_data.csv"
    private val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private val audioUri_android = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.android)
    private val audioUri_haruhi = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.haruhi_03)

    private val mediaItemList = listOf(
        MediaItem.fromUri(audioUri_android),
        MediaItem.fromUri(audioUri_haruhi)
    )


    private val player: Player = ExoPlayer.Builder(context).build().also { exoPlayer ->
        val mediaItem = mediaItemList[0]
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        //exoPlayer.playWhenReady = false
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    exoPlayer.seekTo(0)
                    exoPlayer.playWhenReady = false
                    isAudioPlaying = false
                }
            }
        })
    }


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

    fun changeMediaItemAndPlay(newMediaItem: MediaItem) {
        player.setMediaItem(newMediaItem)
        player.prepare()
        player.play()
        //player.playWhenReady = true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = items[position]
        val address = result.device.address

        // Store RSSI history
        rssiHistory.getOrPut(address) { mutableListOf() }.add(Pair(result.rssi, Instant.now()))
        if ((rssiHistory[address]?.size ?: 0) > averageFilterSize) {
            rssiHistory[address]?.removeFirstOrNull()
        }
        Timber.tag("RSSI History")
            .d("Test")
        Timber.tag("RSSI History").d("Address: $address, Size: ${rssiHistory[address]?.size}")

        // Calculate average RSSI
        val rssiValues = rssiHistory[address]?.map { it.first } ?: listOf(result.rssi)
        val avgRssi = calculateMedian(rssiValues)

        // Store recent 2000 RSSI values and their recorded time
        val recentRssiValues = rssiHistory[address]?.takeLast(2000) ?: emptyList()


        // Generating a random index based on the device address for consistent image and color
        val randomNameIndex = abs(result.device.address.hashCode()) % artworkTitles.size
        val randomIndex = abs(result.device.address.hashCode()) % imageResources.size

        with(holder.binding) {
            deviceName.text = result.device.name ?: artworkTitles[randomNameIndex]
            macAddress.text = result.device.address
            signalStrength.text = "${result.rssi} dBm"
            val distance = 10.0.pow((stmTxPower - result.rssi) / (10.0 * stmRSSIN))
            estiDist.text = String.format(Locale.getDefault(), "%.2f m", distance)
            bluetoothIcon.setImageResource(imageResources[randomIndex])
            bluetoothIcon.imageTintList = ContextCompat.getColorStateList(holder.itemView.context, colorResources[randomIndex])

            connectButton.visibility = if (!result.isConnectable) View.GONE else View.VISIBLE

            if (distance < 3) {
                isAudioPlaying = true
                lastPlayTime = System.currentTimeMillis()
                playAudioForDuration(2000) // Play for 2 seconds

            } else {
                shouldStopAudio = true
                // Delay stopping the audio to ensure it plays for at least 2 seconds
                CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
//                    if (shouldStopAudio) stopAudio()
                }
            }

        }
    }

    private fun playAudioForDuration(duration: Long) {
        //changeMediaItemAndPlay(mediaItemList[0])
        audioPlayJob?.cancel()
        isAudioPlaying = false
        if (!player.isPlaying) player.play()
        Timber.tag("Audio").d("Audio Started")
        audioPlayJob = CoroutineScope(Dispatchers.Main).launch {
            delay(duration)
            if(!isAudioPlaying) {
                stopAudio()
            }
        }
    }


    private fun stopAudio() {
        Timber.tag("Audio").d("Audio Stopped")
        shouldStopAudio = false
        player.pause() // or player.stop() depending on your desired behavior
        isAudioPlaying = false
        audioPlayJob?.cancel()
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

    private fun calculateMedian(list: List<Int>): Int {
        val size = list.size
        return if (size % 2 == 0) {
            (list[size / 2 - 1] + list[size / 2]) / 2
        } else {
            list[size / 2]
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveRssiDataToFile(rssiData: List<Pair<Int, Instant>>, address: String) {
        try {
            val file = File(downloadDir, filename)
            val fileOutputStream = FileOutputStream(file, true)

            rssiData.forEach { (rssi, time) ->
                val timestamp = time.toEpochMilli()
                val line = "$timestamp,$rssi,$address\n"
                fileOutputStream.write(line.toByteArray())
            }

            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}