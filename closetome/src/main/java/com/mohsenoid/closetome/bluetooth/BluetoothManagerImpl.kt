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

package com.mohsenoid.closetome.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mohsenoid.closetome.CloseToMeCallback

internal class BluetoothManagerImpl(context: Context) : BluetoothManager {

    private val _isEnabled = MutableLiveData<Boolean>()
    override val isEnabled: LiveData<Boolean>
        get() = _isEnabled

    private val bluetoothAdapter: BluetoothAdapter?
        get() = BluetoothAdapter.getDefaultAdapter()

    init {
        _isEnabled.value = isBluetoothEnabled()
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        _isEnabled.value = false
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                    }
                    BluetoothAdapter.STATE_ON -> {
                        callback?.onSuccess()
                        _isEnabled.value = true
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    private var callback: CloseToMeCallback? = null

    override fun enableBluetooth(callback: CloseToMeCallback?) {

        if (isBluetoothEnabled()) {
            callback?.onSuccess()
            return
        }

        this.callback = callback

        val enabled = bluetoothAdapter?.enable() ?: false

        if (!enabled) {
            callback?.onError(Throwable("Unable to enable Bluetooth!"))
            this.callback = null
        }
    }
}
