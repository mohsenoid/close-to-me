/*
 * Copyright 2020 Mohsen Mirhoseini
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mohsenoid.closetome.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mohsenoid.closetome.CloseToMeCallback
import com.mohsenoid.closetome.CloseToMeState
import com.mohsenoid.closetome.cache.BeaconResultsCache
import com.mohsenoid.closetome.model.Beacon
import com.mohsenoid.closetome.utils.getManufacturerData
import com.mohsenoid.closetome.utils.getManufacturerDataMask
import com.mohsenoid.closetome.utils.toUUID
import kotlin.math.pow

@ExperimentalUnsignedTypes
internal class BeaconScannerImpl(
    private val manufacturerId: Int,
    private val manufacturerUuid: String,
    private val visibilityDistance: Double,
    private val resultsCache: BeaconResultsCache
) : BeaconScanner {

    private val bluetoothAdapter: BluetoothAdapter
        get() = BluetoothAdapter.getDefaultAdapter()

    private val bluetoothScanner: BluetoothLeScanner
        get() = bluetoothAdapter.bluetoothLeScanner

    private val _state = MutableLiveData<CloseToMeState>()
    override val state: LiveData<CloseToMeState>
        get() = _state

    init {
        _state.value = CloseToMeState.STOPPED
    }

    private var callback: CloseToMeCallback? = null

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            val userUuid: String? = result?.getUserUuid()
            val major: UShort? = result?.getMajor()
            val minor: UShort? = result?.getMinor()
            val manufacturerUuid: String? = result?.getManufacturerUuid()

            if (result == null || result.device == null || major == null || minor == null || manufacturerUuid == null) return

            resultsCache.add(mapScanResultToBeacon(result, manufacturerUuid, major, minor, userUuid))
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val error = getErrorCodeMessage(errorCode)
            callback?.onError(Throwable("Discovery onScanFailed: $error"))
        }
    }

    private fun mapScanResultToBeacon(result: ScanResult, manufacturerUuid: String, major: UShort, minor: UShort, userUuid: String?): Beacon {
        val txPower = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) result.txPower else 0
        val distance = calculateDistanceInMeter(result.rssi, txPower)

        return Beacon(
            address = result.device.address,
            manufacturerUuid = manufacturerUuid,
            major = major,
            minor = minor,
            userUuid = userUuid,
            lastSeen = System.currentTimeMillis(),
            distanceInMeter = distance,
            isNear = distance < visibilityDistance,
            isVisible = true
        )
    }

    private fun calculateDistanceInMeter(rssi: Int, txPower: Int): Double {
        if (rssi == 0) {
            return -1.0 // if we cannot determine accuracy, return -1.
        }
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            0.89976 * ratio.pow(7.7095) + 0.111
        } * 100 // fix to show correct meter!
    }

    private fun ScanResult.getUserUuid(): String? {
        val serviceUuids: List<ParcelUuid>? = scanRecord?.serviceUuids
        return if (serviceUuids != null && serviceUuids.isNotEmpty()) serviceUuids[0].toString() else null
    }

    private fun ScanResult.getMajor(): UShort? {
        var major: UShort? = null

        try {
            val majorFirstByte = scanRecord?.bytes?.get(22)
            val majorSecondByte = scanRecord?.bytes?.get(23)

            if (majorFirstByte != null && majorSecondByte != null) {
                major = ((majorFirstByte.toInt() shl 8) + majorSecondByte.toInt()).toUShort()
            }
        } finally {
            return major
        }
    }

    private fun ScanResult.getMinor(): UShort? {
        var minor: UShort? = null

        try {
            val minorFirstByte = scanRecord?.bytes?.get(24)
            val minorSecondByte = scanRecord?.bytes?.get(25)

            if (minorFirstByte != null && minorSecondByte != null) {
                minor = ((minorFirstByte.toInt() shl 8) + minorSecondByte.toInt()).toUShort()
            }
        } finally {
            return minor
        }
    }

    private fun ScanResult.getManufacturerUuid(): String? {
        var manufacturerUuid: String? = null

        try {
            val bytes = scanRecord?.bytes?.slice(6..21)

            if (bytes != null) {
                manufacturerUuid = bytes.toByteArray().toUUID().toString()
            }
        } finally {
            return manufacturerUuid
        }
    }

    override fun start(callback: CloseToMeCallback?) {

        if (_state.value == CloseToMeState.STARTED) {
            callback?.onSuccess()
            return
        }

        this.callback = callback

        bluetoothScanner.startScan(getScanFilters(), getSettings(), scanCallback)
        resultsCache.start()

        _state.value = CloseToMeState.STARTED
        callback?.onSuccess()
    }

    private fun getScanFilters(): List<ScanFilter> {
        return listOf(
            ScanFilter.Builder().apply {
                setManufacturerData(manufacturerId, getManufacturerData(manufacturerUuid), getManufacturerDataMask())
            }.build()
        )
    }

    private fun getSettings(): ScanSettings? {
        return ScanSettings.Builder().apply {
            setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            setReportDelay(0)
        }.build()
    }

    override fun stop(callback: CloseToMeCallback?) {
        if (_state.value == CloseToMeState.STOPPED) {
            callback?.onSuccess()
            return
        }

        bluetoothScanner.stopScan(scanCallback)
        resultsCache.stop()

        _state.value = CloseToMeState.STOPPED
        callback?.onSuccess()
    }

    private fun getErrorCodeMessage(errorCode: Int): String {
        return when (errorCode) {
            NO_ERROR ->
                "The requested scanning was successful."
            ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
                "Fails to start scan as BLE scan with the same settings is already started by the app."
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
                "Fails to start scan as app cannot be registered."
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
                "Fails to start scan due an internal error"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
                "Fails to start power optimized scan as this feature is not supported."
            SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ->
                "Fails to start scan as it is out of hardware resources."
            SCAN_FAILED_SCANNING_TOO_FREQUENTLY ->
                "Fails to start scan as application tries to scan too frequently."
            else ->
                "Unknown error!"
        }
    }

    companion object {
        const val NO_ERROR = 0
        const val SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5
        const val SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6
    }
}
