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

package com.mohsenoid.closetome.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.mohsenoid.closetome.CloseToMe
import com.mohsenoid.closetome.CloseToMeCallback
import com.mohsenoid.closetome.CloseToMeState
import com.mohsenoid.closetome.sample.databinding.MainActivityBinding
import java.util.UUID

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    private val manufacturerUuid = UUID.fromString("01234567-89AB-CD01-2345-67890ABCD012")

    private val userUuid = UUID.randomUUID()

    private var closeToMe: CloseToMe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)

        initUi()

        val anyPermissionsNotGranted =
            permissions.any { permission -> ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED }

        if (anyPermissionsNotGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST)
        } else {
            initCloseToMe()
        }
    }

    private fun initUi() {
        binding.user.text = "User: $userUuid"
        binding.log.movementMethod = ScrollingMovementMethod()
        binding.start.setOnClickListener { onStartClick() }
        binding.stop.setOnClickListener { onStopClick() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                if (grantResults.isEmpty()) {
                    onPermissionNotGranted()
                }

                val isNotGranted = grantResults.any {
                    it != PackageManager.PERMISSION_GRANTED
                }

                if (isNotGranted) {
                    onPermissionNotGranted()
                } else {
                    initCloseToMe()
                }
            }
        }
    }

    private fun onPermissionNotGranted() {
        Toast.makeText(this, R.string.permission_is_required, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun initCloseToMe() {
        closeToMe = CloseToMe.Builder(this, manufacturerUuid)
            .setUserUuid(userUuid)
            .setMajor(1U)
            .setMinor(1U)
            .setVisibilityDistanceMeter(3.0)
            .setVisibilityTimeoutMs(5_000)
            .build().also {

                if (!it.hasBleFeature()) {
                    Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show()
                    finish()
                }

                it.state.observe(this, Observer { state ->
                    log("Beacon state: $state")

                    when (state) {
                        CloseToMeState.STARTED -> {
                            binding.start.isVisible = false
                            binding.stop.isVisible = true
                        }
                        else -> {
                            binding.start.isVisible = true
                            binding.stop.isVisible = false
                        }
                    }
                })

                it.results.observe(this, Observer { beacons ->
                    binding.result.text = beacons.values.joinToString("\n--------------------------------\n") { beacon ->
                        "User: ${beacon.userUuid}\n" +
                            "manufacturer: ${beacon.manufacturerUuid}\n" +
                            "major: ${beacon.major}\n" +
                            "minor: ${beacon.minor}\n" +
                            "MinDistance: ${"%.2f".format(beacon.minDistanceInMeter)}m\n" +
                            "LastDistance: ${"%.2f".format(beacon.distanceInMeter)}m\n" +
                            "isVisible: ${beacon.isVisible}\n" +
                            "isNear: ${beacon.isNear}"
                    }

                    log("Result: $beacons")
                })
            }
    }

    private fun onStartClick() {
        if (closeToMe?.isBluetoothEnabled?.value != true) {
            log("Enabling bluetooth...")

            closeToMe?.enableBluetooth(object : CloseToMeCallback {
                override fun onSuccess() {
                    log("Bluetooth is on now")
                }

                override fun onError(throwable: Throwable) {
                    log(throwable.message ?: throwable.toString())
                }
            })
        } else {
            closeToMe?.start(object : CloseToMeCallback {
                override fun onSuccess() {
                    log("Beacon started successfully!")
                }

                override fun onError(throwable: Throwable) {
                    log(throwable.message ?: throwable.toString())
                }
            })
        }
    }

    private fun onStopClick() {
        closeToMe?.stop(object : CloseToMeCallback {
            override fun onSuccess() {
                log("Beacon stopped successfully!")
            }

            override fun onError(throwable: Throwable) {
                log(throwable.message ?: throwable.toString())
            }
        })
    }

    private fun log(message: String) {
        runOnUiThread {
            binding.log.text = "$message\n" +
                "-----------------------------------\n" +
                "${binding.log.text}"
        }
    }

    override fun onPause() {
        closeToMe?.stop()
        super.onPause()
    }

    companion object {
        const val PERMISSIONS_REQUEST = 1010

        val permissions: Array<String> = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
