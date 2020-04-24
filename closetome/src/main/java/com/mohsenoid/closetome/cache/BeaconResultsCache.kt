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

package com.mohsenoid.closetome.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mohsenoid.closetome.model.Beacon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

internal class BeaconResultsCache(private val visibilityTimeout: Long) {

    private val _results = MutableLiveData<Map<String, Beacon>>()
    val results: LiveData<Map<String, Beacon>>
        get() = _results

    private var job: Job? = null

    fun start() {
        _results.value = HashMap()

        job = GlobalScope.launch(Dispatchers.IO) {
            while (isActive) {
                val value = _results.value
                value?.filter { it.value.isVisible }?.forEach {
                    if (it.value.lastSeen < System.currentTimeMillis() - visibilityTimeout) {
                        it.value.isVisible = false
                    }
                }
                _results.postValue(value)

                delay(visibilityTimeout)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun add(beacon: Beacon) {
        val beaconWithMinDistance = updateBeaconMinDistance(beacon)
        addResult(beaconWithMinDistance)
    }

    private fun updateBeaconMinDistance(beacon: Beacon): Beacon {
        return _results.value?.get(beacon.userUuid)?.let { existingBeacon ->
            beacon.copy(minDistanceInMeter = min(existingBeacon.minDistanceInMeter, beacon.distanceInMeter))
        } ?: beacon.copy(minDistanceInMeter = beacon.distanceInMeter)
    }

    private fun addResult(beacon: Beacon) {
        _results.value = _results.value?.toMutableMap()?.apply {
            put(beacon.userUuid ?: beacon.address, beacon)
        }
    }
}
