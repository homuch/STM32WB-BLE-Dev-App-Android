package com.stm.bledemo.activity.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.stm.bledemo.R
import com.stm.bledemo.activity.scan.fragment.AdvertisingDataFragment
import com.stm.bledemo.activity.scan.fragment.AlertDialogFragment
import com.stm.bledemo.ble.BLEManager
import com.stm.bledemo.databinding.RowScanResultBinding
import com.stm.bledemo.extension.toHexString
import com.stm.bledemo.filter.KalmanFilter
import com.stm.bledemo.utilities.VibratorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

@SuppressLint("NotifyDataSetChanged", "MissingPermission")
class ScanAdapter(
    private val items: List<ScanResult>,
    private val delegate: Delegate,
    private val context: Context,
    private val whiteListAddress: List<String>
) : RecyclerView.Adapter<ScanAdapter.ViewHolder>() {

    private val itemsCopy: ArrayList<ScanResult> = arrayListOf()
    private val rssiHistory: MutableMap<String, MutableList<Int>> = mutableMapOf()
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
    private val colorResources = listOf(
        R.color.st_pink,
        R.color.teal_200,
        R.color.purple_200,
        R.color.purple_700,
        R.color.green,
        R.color.orange
    )
    private val stmTxPower = -67.82841823
    private val stmRSSIN = 2.680965147
    private val averageFilterSize = 2000
    private var isAudioPlaying = false
    private var audioPlayJob: Job? = null
    private var lastPlayTime = 0L
    private var shouldStopAudio = false
    private var curPlayingId = 0
    private val lastDangerAlertTime = mutableMapOf<String, Long>()

    private val mediaIdCache = mutableMapOf<String, Int>()

    private val kalmanFilterList = MutableList(whiteListAddress.size) { KalmanFilter() }

    private val vibratorService = VibratorService(context)


    private val audioUri_android =
        Uri.parse("android.resource://" + context.packageName + "/" + R.raw.android)
    private val audioUri_haruhi =
        Uri.parse("android.resource://" + context.packageName + "/" + R.raw.haruhi_03)
    private val audioUri_monaLisa =
        Uri.parse("android.resource://" + context.packageName + "/" + R.raw.mona_lisa)
    private val audioUri_starNight =
        Uri.parse("android.resource://" + context.packageName + "/" + R.raw.star_night)

    private val mediaItemList = listOf(
        MediaItem.fromUri(audioUri_android),
        MediaItem.fromUri(audioUri_haruhi),
        MediaItem.fromUri(audioUri_monaLisa),
        MediaItem.fromUri(audioUri_starNight)
    )
    private val mediaKeywordsList = listOf(
        "android",
        "haruhi",
        "mona lisa",
        "star night"
    )


    private val lastDistanceList = mutableListOf<Double>(0.0, 0.0)
    private val weight = 0


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

    companion object DialogListener : AlertDialogFragment.DialogListener {
        override fun onDialogClosed() {
            Timber.tag("Dialog").d("Closed")
//            TODO("Not yet implemented")
        }
    }


    interface Delegate {
        fun onConnectButtonClick(result: ScanResult)
        fun onItemClick(dialog: DialogFragment)
        fun showAlertDialog(title: String, msg: String, dialogListener: AlertDialogFragment.DialogListener)
    }

    inner class ViewHolder(val binding: RowScanResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.connectButton.setOnClickListener {
                val result = items[bindingAdapterPosition]
                delegate.onConnectButtonClick(result)
            }
            itemView.setOnClickListener {
//                val result = items[bindingAdapterPosition]
//                val dialog = AdvertisingDataFragment(result)
//                delegate.onItemClick(dialog)
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

    private fun changeMediaItem(newMediaId: Int) {
        if (curPlayingId == newMediaId) return
        curPlayingId = newMediaId
        player.setMediaItem(mediaItemList[curPlayingId])
        player.prepare()
//        player.play()
        //player.playWhenReady = true
    }

    private fun getMediaId(newMediaName: String): Int {
        return mediaIdCache.getOrPut(newMediaName) {
            mediaKeywordsList.indexOf(
                mediaKeywordsList.minByOrNull {
                    levenshteinDistance(it, newMediaName)
                }
            )
        }
    }

    private fun changeMediaItem(newMediaName: String) {
        val mostSimilarIndex = getMediaId(newMediaName)

        if (mostSimilarIndex != -1 && curPlayingId != mostSimilarIndex) {
            curPlayingId = mostSimilarIndex
            //        player.play()
            //player.playWhenReady = true
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = items[position]
        val address = result.device.address
        val whiteListId = whiteListAddress.indexOf(address)

        // Store RSSI history
        rssiHistory.getOrPut(address) { mutableListOf() }.add(result.rssi)
        if (rssiHistory[address]?.size!! > averageFilterSize) {
            rssiHistory[address]?.remove(0)
        }
        Timber.tag("RSSI History").d("Address: $address, Size: ${rssiHistory[address]?.size}")

//        val filteredValue = kalmanFilter.filter(result.rssi)

        val filteredValue = if (whiteListId < 0) {
            Timber.tag("filter").d("No Whitelist")
            result.rssi.toDouble()
        } else {
            if(kalmanFilterList.size <= whiteListId) {
                kalmanFilterList.addAll(List(whiteListId - kalmanFilterList.size) {
                    KalmanFilter()
                })
            }
            kalmanFilterList[whiteListId].filter(result.rssi)
        }
        // Store recent 2000 RSSI values and their recorded time


        // Generating a random index based on the device address for consistent image and color
        val randomNameIndex = abs(result.device.address.hashCode()) % artworkTitles.size
        val randomIndex = abs(result.device.address.hashCode()) % imageResources.size

        val deviceNameExtended = if (result.scanRecord?.bytes != null) {
            val advBytes = result.scanRecord!!.bytes
            val nameBytes = advBytes.copyOfRange(5, 12) + advBytes.copyOfRange(15, 25)
            String(nameBytes, Charsets.UTF_8)
                .replace(Regex("[^\\x20-\\x7E]"), "") // Remove non-printable characters
                .trim() // Remove leading/trailing spaces
        } else {
            null
        }
        if (deviceNameExtended != null) {
            Timber.tag("Device Name").d(deviceNameExtended)
        }

        val dangerStateChar = result.scanRecord?.bytes?.get(25)?.toInt()?.toChar()
        val currentTime = System.currentTimeMillis()

        val lastAlertTime = lastDangerAlertTime.getOrPut(address) { 0 }

        if (dangerStateChar == 'b' && (currentTime - lastAlertTime) > 1000) { // Check if 5 seconds have passed
            Timber.tag("Danger State").d(dangerStateChar.toString())
            Timber.tag("Danger State").d(result.device.address)
            vibratorService.vibrate(500)
            delegate.showAlertDialog(
                deviceNameExtended ?: result.device.name ?: artworkTitles[randomNameIndex],
                "is in danger!!",
                DialogListener,
            )
            lastDangerAlertTime[address] = currentTime // Update last alert time


        }                                   // to prevent frequent alerts.



        with(holder.binding) {
            if(isAudioPlaying && whiteListId == curPlayingId)
                scanResultRow.background = ColorDrawable(ContextCompat.getColor(context, R.color.green))
            else {
                scanResultRow.background = null
            }

            Timber.tag("audio").d("isAudioPlaying: ${isAudioPlaying}, curPlayingId: $curPlayingId, whiteListId: $whiteListId")

            deviceName.text = deviceNameExtended ?: artworkTitles[randomNameIndex]
            macAddress.text = result.device.address
            signalStrength.text = "${result.rssi} dBm"
//            signalStrength.text = "${filteredValue} dBm"
            val distance = 10.0.pow((stmTxPower - filteredValue) / (10.0 * stmRSSIN))
//            val distance = 10.0.pow((stmTxPower - result.rssi) / (10.0 * stmRSSIN))
            estiDist.text = String.format(Locale.getDefault(), "%.2f m", distance)
            bluetoothIcon.setImageResource(imageResources[randomIndex])
            bluetoothIcon.imageTintList = ContextCompat.getColorStateList(
                holder.itemView.context,
                colorResources[randomIndex]
            )

            connectButton.visibility = if (!result.isConnectable) View.GONE else View.VISIBLE

            dumpFile.visibility = View.VISIBLE
            dumpFile.setOnClickListener {
                val recentRssiValues = rssiHistory[address]?.takeLast(2000) ?: emptyList()
//                saveRssiDataToFile(recentRssiValues, address)
//                vibratorService.vibrate(500) // Vibrate for 500 milliseconds
//                showCustomDialog(context)
                saveRssiDataToFile(recentRssiValues, address)

                delegate.showAlertDialog(
                    "test",
                    rssiHistory[address].toString(),
                    DialogListener)
            }

            if (whiteListId < 0) return
            if (distance < 1.3) {
                if (!isAudioPlaying) {
//                    changeMediaItem(whiteListId)
                    changeMediaItem(deviceNameExtended ?: artworkTitles[randomNameIndex].also { player.setMediaItem(mediaItemList[getMediaId(it)]); player.prepare() })
                }
                isAudioPlaying = true
                lastPlayTime = System.currentTimeMillis()
//                vibratorService.vibrate(500) // Vibrate for 500 milliseconds
                playAudioForDuration(2000) // Play for 2 seconds

            } else {
//                playAudioForDuration(0)
//                shouldStopAudio = true
                if(distance > 3 && curPlayingId == getMediaId(deviceNameExtended ?: artworkTitles[randomNameIndex])){
                    stopAudio()
                }
//                if(System.currentTimeMillis()-lastPlayTime > 3000 && isAudioPlaying){
//                    stopAudio()
//                }
                // Delay stopping the audio to ensure it plays for at least 2 seconds
//                CoroutineScope(Dispatchers.Main).launch {
//                    delay(2000)
////                    if (shouldStopAudio) stopAudio()
//                }
            }

        }
    }

    private fun playAudioForDuration(duration: Long) {
        //changeMediaItemAndPlay(mediaItemList[0])
        audioPlayJob?.cancel()
        if (!player.isPlaying) player.play()
        Timber.tag("Audio").d("Audio Started")
        audioPlayJob = CoroutineScope(Dispatchers.Main).launch {
            delay(duration)
            stopAudio()
        }
    }


    private fun stopAudio() {
        Timber.tag("Audio").d("Audio Stopped")
//        shouldStopAudio = false
        player.pause() // or player.stop() depending on your desired behavior
        isAudioPlaying = false
        audioPlayJob?.cancel()
    }


    override fun getItemCount() = items.size

    // Filter Recycler View by given text
    fun filter(value: String, type: String) {
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
    private fun saveRssiDataToFile(recentRssiValues: List<Int>, address: String) {
        val filename = "rssi_data_${address.replace(":", "_")}.csv"
        val downloadDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        try {
            val file = File(downloadDir, filename)
            val fileOutputStream = FileOutputStream(file, false) // false for overwrite

            recentRssiValues.forEach { rssi ->
                val line = "$rssi,$address\n"
                fileOutputStream.write(line.toByteArray())
            }

            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)
        for (i in 0..s1.length) {
            var lastValue = i;
            for (j in 0..s2.length) {
                if (i == 0) { costs[j] = j; }
                else if (j > 0) { if (s1[i - 1] == s2[j - 1]) { costs[j] = lastValue; } else { val temp = costs[j]; costs[j] = min(min(costs[j - 1], lastValue), temp) + 1; lastValue = temp; }; }; }; };
        return costs[s2.length];
    }



//    private fun showCustomDialog(context: Context) {
//        progressDialog = ProgressDialog(context)
//        progressDialog.setMessage("Downloading...")
//        progressDialog.setCancelable(false)
//        progressDialog.show()
//
//        CoroutineScope(Dispatchers.IO).launch {
//            // Simulate a download task with a delay
//            delay(3000)
//
//            withContext(Dispatchers.Main) {
//                progressDialog.dismiss()
//                Toast.makeText(context, "Download Complete!", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//    }
//    private fun showCustomDialog(context: Context) {
//        val builder = AlertDialog.Builder(context)
//        builder.setTitle("Custom Alert Title")
//        builder.setMessage("This is a custom dialog with additional controls")
//
//        // Inflate the custom layout
//        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        val dialogView = inflater.inflate(R.layout.dialog_layout, null)
//
//        // Set the custom layout to the dialog
//        builder.setView(dialogView)
//
//        // Access and customize elements in the custom layout
//        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)
//        closeButton.setOnClickListener {
//            // Dismiss the dialog
//            builder.create().dismiss()
//        }
//    }
}