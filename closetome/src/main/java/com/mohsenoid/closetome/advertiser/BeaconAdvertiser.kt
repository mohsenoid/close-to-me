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

import androidx.lifecycle.LiveData
import com.mohsenoid.closetome.CloseToMeCallback
import com.mohsenoid.closetome.CloseToMeState

internal interface BeaconAdvertiser {

    val state: LiveData<CloseToMeState>

    fun start(callback: CloseToMeCallback? = null)

    fun stop(callback: CloseToMeCallback? = null)
}
