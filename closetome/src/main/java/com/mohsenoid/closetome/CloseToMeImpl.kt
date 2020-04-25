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

package com.mohsenoid.closetome

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.mohsenoid.closetome.advertiser.BeaconAdvertiser
import com.mohsenoid.closetome.advertiser.BeaconAdvertiserImpl
import com.mohsenoid.closetome.bluetooth.BluetoothManager
import com.mohsenoid.closetome.bluetooth.BluetoothManagerImpl
import com.mohsenoid.closetome.cache.BeaconResultsCache
import com.mohsenoid.closetome.model.Beacon
import com.mohsenoid.closetome.scanner.BeaconScanner
import com.mohsenoid.closetome.scanner.BeaconScannerImpl
import com.mohsenoid.closetome.utils.CombinedLiveData

internal class CloseToMeImpl(
    private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val advertiser: BeaconAdvertiser,
    private val scanner: BeaconScanner,
    private val resultsCache: BeaconResultsCache
) : CloseToMe {

    override val state: MediatorLiveData<CloseToMeState> =
        CombinedLiveData(scanner.state, advertiser.state) { scannerState, advertiserState, currentValue ->
            when {
                scannerState == null || advertiserState == null -> null
                scannerState == advertiserState -> scannerState
                else -> CloseToMeState.STOPPED
            }?.takeIf { newValue ->
                currentValue != newValue
            }
        }

    override val results: LiveData<Map<String, Beacon>>
        get() = resultsCache.results

    override val isBluetoothEnabled: LiveData<Boolean>
        get() = bluetoothManager.isEnabled

    init {
        isBluetoothEnabled.observeForever { isActive ->
            if (isActive == false) {
                stop()
            }
        }
    }

    override fun hasBleFeature(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    override fun enableBluetooth(callback: CloseToMeCallback?) {
        bluetoothManager.enableBluetooth(callback)
    }

    override fun start(callback: CloseToMeCallback?) {
        if (!bluetoothManager.isBluetoothEnabled()) {
            callback?.onError(Throwable("Bluetooth is not enable!"))
            return
        }

        advertiser.start(object : CloseToMeCallback {
            override fun onSuccess() {
                scanner.start(object : CloseToMeCallback {
                    override fun onSuccess() {
                        callback?.onSuccess()
                    }

                    override fun onError(throwable: Throwable) {
                        callback?.onError(throwable)
                    }
                })
            }

            override fun onError(throwable: Throwable) {
                callback?.onError(throwable)
            }
        })
    }

    override fun stop(callback: CloseToMeCallback?) {
        advertiser.stop(object : CloseToMeCallback {
            override fun onSuccess() {
                scanner.stop(object : CloseToMeCallback {
                    override fun onSuccess() {
                        callback?.onSuccess()
                    }

                    override fun onError(throwable: Throwable) {
                        callback?.onError(throwable)
                    }
                })
            }

            override fun onError(throwable: Throwable) {
                callback?.onError(throwable)
            }
        })
    }

    companion object {

        @OptIn(ExperimentalUnsignedTypes::class)
        fun newInstance(
            context: Context,
            userUuid: String?,
            manufacturerId: Int,
            manufacturerUuid: String,
            major: UShort,
            minor: UShort,
            visibilityTimeout: Long,
            visibilityDistance: Double
        ): CloseToMe {

            val bluetoothManager = BluetoothManagerImpl(context)

            val advertiser = BeaconAdvertiserImpl(userUuid, manufacturerId, manufacturerUuid, major, minor)

            val resultsCache = BeaconResultsCache(visibilityTimeout)

            val scanner = BeaconScannerImpl(manufacturerId, manufacturerUuid, visibilityDistance, resultsCache)

            return CloseToMeImpl(context, bluetoothManager, advertiser, scanner, resultsCache)
        }
    }
}
