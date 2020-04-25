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

interface CloseToMe {

    /**
     * Get CloseToMe current status as LiveData
     */
    val state: LiveData<CloseToMeState>

    /**
     * Get CloseToMe scan results as LiveData
     */
    val results: LiveData<Map<String, Beacon>>

    /**
     * Get device bluetooth status as LiveData
     */
    val isBluetoothEnabled: LiveData<Boolean>

    /**
     * Check if current device has BluetoothLE feature
     */
    fun hasBleFeature(): Boolean

    /**
     * Enable bluetooth if it is off
     *
     * @param callback Informs the result of bluetooth enabling
     */
    fun enableBluetooth(callback: CloseToMeCallback? = null)

    /**
     * Starts CloseToMe which includes advertising and scanning at the same time to get close devices info.
     * To get the scan results you have to Observe [results]
     *
     * @param callback Informs the result of starting advertiser and scanner
     */
    fun start(callback: CloseToMeCallback? = null)

    /**
     * Stops CloseToMe which includes advertising and scanning at the same time
     *
     * @param callback Informs the result of stopping advertiser and scanner
     */
    fun stop(callback: CloseToMeCallback? = null)

    /**
     * CloseToMe Builder
     *
     * @param context Application context
     * @param manufacturerUuid Use your own Manufacturer UUID to distinguish your clients
     */
    class Builder(private val context: Context, private val manufacturerUuid: UUID) {

        private var userUuid: UUID? = null

        /**
         * Sets User UUID which allows you to distinguish your clients from each other.
         * Note: if you do not set this value then you have to use other clients Bluetooth MacAddress
         * which changes every time for privacy reasons.
         *
         * @param value User UUID value
         * @return CloseToMe Builder
         */
        fun setUserUuid(value: UUID?): Builder {
            userUuid = value
            return this
        }

        private var manufacturerId: Int = APPLE_MANUFACTURER_ID

        /**
         * You can change Beacon Manufacturer ID if you need to.
         *
         * @param value Manufacturer ID value
         * @return CloseToMe Builder
         */
        fun setManufacturerId(value: Int): Builder {
            manufacturerId = value
            return this
        }

        private var visibilityTimeout: Long = BEACON_VISIBILITY_TIMEOUT

        /**
         * When a client is not visible more than this timeout it sets as hidden by this field [Beacon.isVisible].
         * Visibility timeout is being controlled by a timer and Beacon last visibility time.
         *
         * @param value Visibility Timeout in milliseconds
         * @return CloseToMe Builder
         */
        fun setVisibilityTimeoutMs(value: Long): Builder {
            visibilityTimeout = value
            return this
        }

        private var visibilityDistance: Double = BEACON_VISIBILITY_DISTANCE

        /**
         * When a client is not closer than this distance it sets as far by this field [Beacon.isNear].
         * Distance is being calculated based on Bluetooth signal strength.
         *
         * @param value Visibility Distance in meter
         * @return CloseToMe Builder
         */
        fun setVisibilityDistanceMeter(value: Double): Builder {
            visibilityDistance = value
            return this
        }

        @ExperimentalUnsignedTypes
        private var major: UShort = BEACON_MAJOR

        /**
         * Sets Beacon Major value which can be used to distinguish clients from each other.
         *
         * @param value Major unsigned short value between 1 and 65535
         * @return CloseToMe Builder
         */
        @ExperimentalUnsignedTypes
        fun setMajor(value: UShort): Builder {
            if (value == 0u.toUShort()) throw IllegalArgumentException("Major should be between 1 and 65535")
            major = value
            return this
        }

        @ExperimentalUnsignedTypes
        private var minor: UShort = BEACON_MINOR

        /**
         * Sets Beacon Minor value which can be used to distinguish clients from each other.
         *
         * @param value Minor unsigned short value between 1 and 65535
         * @return CloseToMe Builder
         */
        @ExperimentalUnsignedTypes
        fun setMinor(value: UShort): Builder {
            if (value == 0u.toUShort()) throw IllegalArgumentException("Minor should be between 1 and 65535")
            minor = value
            return this
        }

        /**
         * Builds CloseToMe
         *
         * @return CloseToMe
         */
        fun build(): CloseToMe {
            return CloseToMeImpl.newInstance(
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
            const val BEACON_MAJOR: UShort = 1u

            @OptIn(ExperimentalUnsignedTypes::class)
            const val BEACON_MINOR: UShort = 1u
        }
    }
}
