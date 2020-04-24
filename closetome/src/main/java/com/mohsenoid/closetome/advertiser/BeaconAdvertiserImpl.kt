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

package com.mohsenoid.closetome.advertiser

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mohsenoid.closetome.CloseToMeCallback
import com.mohsenoid.closetome.CloseToMeState
import com.mohsenoid.closetome.utils.getManufacturerData
import com.mohsenoid.closetome.utils.toParcelUuid

@ExperimentalUnsignedTypes
internal class BeaconAdvertiserImpl(
    private val userUuid: String? = null,
    private val manufacturerId: Int,
    private val manufacturerUuid: String,
    private val major: UShort,
    private val minor: UShort
) : BeaconAdvertiser {

    private val bluetoothAdapter: BluetoothAdapter
        get() = BluetoothAdapter.getDefaultAdapter()

    private val bluetoothAdvertiser: BluetoothLeAdvertiser
        get() = bluetoothAdapter.bluetoothLeAdvertiser

    private val _state = MutableLiveData<CloseToMeState>()
    override val state: LiveData<CloseToMeState>
        get() = _state

    init {
        _state.value = CloseToMeState.STOPPED
    }

    override fun start(callback: CloseToMeCallback?) {

        if (_state.value == CloseToMeState.STARTED) {
            callback?.onSuccess()
            return
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            callback?.onError(Throwable("Multi advertisement is not supported by this phone chip!"))
            return
        }
        this.callback = callback

        bluetoothAdvertiser.startAdvertising(
            getSettings(),
            getAdvertiseData(),
            userUuid?.let { getScanResponse(it) },
            advertiseCallback
        )
    }

    private fun getAdvertiseData(): AdvertiseData {
        return AdvertiseData.Builder()
            .addManufacturerData(manufacturerId, getManufacturerData(manufacturerUuid, major, minor))
            .build()
    }

    private fun getScanResponse(userUuid: String): AdvertiseData {
        return AdvertiseData.Builder()
            .addServiceUuid(userUuid.toParcelUuid())
            .setIncludeTxPowerLevel(true)
            .build()
    }

    private fun getSettings(): AdvertiseSettings? {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
            .setConnectable(false)
            .setTimeout(0)
            .build()
    }

    private var callback: CloseToMeCallback? = null

    private val advertiseCallback = object : AdvertiseCallback() {

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _state.value = CloseToMeState.STARTED
            callback?.onSuccess()

            super.onStartSuccess(settingsInEffect)
        }

        override fun onStartFailure(errorCode: Int) {
            _state.value = CloseToMeState.STOPPED
            callback?.onError(
                Throwable(
                    "Advertising onStartFailure: $errorCode - ${getErrorCodeMessage(
                        errorCode
                    )}"
                )
            )

            super.onStartFailure(errorCode)
        }
    }

    override fun stop(callback: CloseToMeCallback?) {
        if (_state.value == CloseToMeState.STOPPED) {
            callback?.onSuccess()
            return
        }

        bluetoothAdvertiser.stopAdvertising(advertiseCallback)

        _state.value = CloseToMeState.STOPPED
        callback?.onSuccess()
    }

    private fun getErrorCodeMessage(errorCode: Int): String {
        return when (errorCode) {
            AdvertisingSetCallback.ADVERTISE_SUCCESS ->
                "The requested advertising was successful."
            AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE ->
                "Failed to start advertising as the advertise data to be broadcasted is too large."
            AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                "Failed to start advertising because no advertising instance is available."
            AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED ->
                "Failed to start advertising as the advertising is already started."
            AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR ->
                "Operation failed due to an internal error."
            AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                "This feature is not supported on this platform ."
            else ->
                "Unknown error!"
        }
    }
}
