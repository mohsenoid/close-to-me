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
import androidx.lifecycle.LiveData
import com.mohsenoid.closetome.model.Beacon
import java.util.UUID

interface CloseToMeManager {

    val state: LiveData<CloseToMeState>

    val results: LiveData<Map<String, Beacon>>

    val isBluetoothEnabled: LiveData<Boolean>

    fun hasBleFeature(): Boolean

    fun enableBluetooth(callback: CloseToMeCallback? = null)

    fun start(callback: CloseToMeCallback? = null)

    fun stop(callback: CloseToMeCallback? = null)

    class Builder(private val context: Context, private val manufacturerUuid: UUID) {

        private var userUuid: UUID? = null

        fun setUserUuid(value: UUID?): Builder {
            userUuid = value
            return this
        }

        private var manufacturerId: Int = APPLE_MANUFACTURER_ID

        fun setManufacturerId(value: Int): Builder {
            manufacturerId = value
            return this
        }

        private var visibilityTimeout: Long = BEACON_VISIBILITY_TIMEOUT

        fun setVisibilityTimeoutMs(value: Long): Builder {
            visibilityTimeout = value
            return this
        }

        private var visibilityDistance: Double = BEACON_VISIBILITY_DISTANCE

        fun setVisibilityDistanceMeter(value: Double): Builder {
            visibilityDistance = value
            return this
        }

        @ExperimentalUnsignedTypes
        private var major: UShort = BEACON_MAJOR

        @ExperimentalUnsignedTypes
        fun setMajor(value: UShort): Builder {
            major = value
            return this
        }

        @ExperimentalUnsignedTypes
        private var minor: UShort = BEACON_MINOR

        @ExperimentalUnsignedTypes
        fun setMinor(value: UShort): Builder {
            minor = value
            return this
        }

        fun build(): CloseToMeManager {
            return CloseToMeManagerImpl.newInstance(
                context,
                userUuid?.toString(),
                manufacturerId,
                manufacturerUuid.toString(),
                major,
                minor,
                visibilityTimeout,
                visibilityDistance
            )
        }

        companion object {

            const val BEACON_VISIBILITY_TIMEOUT: Long = 10_000

            const val BEACON_VISIBILITY_DISTANCE: Double = 1.0

            const val APPLE_MANUFACTURER_ID = 0x4c00

            @OptIn(ExperimentalUnsignedTypes::class)
            const val BEACON_MAJOR: UShort = UShort.MIN_VALUE

            @OptIn(ExperimentalUnsignedTypes::class)
            const val BEACON_MINOR: UShort = UShort.MIN_VALUE
        }
    }
}
