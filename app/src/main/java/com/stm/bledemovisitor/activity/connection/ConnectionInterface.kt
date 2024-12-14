package com.stm.bledemovisitor.activity.connection

import android.bluetooth.BluetoothGattCharacteristic

interface ConnectionInterface {
    fun addDiscoveredItems()
    fun valueUpdated(characteristic: BluetoothGattCharacteristic)
    fun finishActivity()
}