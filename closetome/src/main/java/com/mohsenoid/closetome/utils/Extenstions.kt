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

package com.mohsenoid.closetome.utils

import android.os.ParcelUuid
import java.nio.ByteBuffer
import java.util.UUID

fun String.toParcelUuid(): ParcelUuid? =
    ParcelUuid.fromString(this)

fun String.toUuid(): UUID? =
    UUID.fromString(this)

fun UUID.toBytes(): ByteArray {
    val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(mostSignificantBits)
    bb.putLong(leastSignificantBits)
    return bb.array()
}

fun ByteArray.toUUID(): UUID? {
    val byteBuffer = ByteBuffer.wrap(this)
    val high = byteBuffer.long
    val low = byteBuffer.long
    return UUID(high, low)
}

@OptIn(ExperimentalUnsignedTypes::class)
internal fun getManufacturerData(manufacturerUuid: String, major: UShort? = null, minor: UShort? = null): ByteArray {
    val manufacturerData: ByteBuffer = ByteBuffer.allocate(23)
    manufacturerData.put(0, 0x02.toByte()) // Beacon Identifier
    manufacturerData.put(1, 0x15.toByte()) // Beacon Identifier

    val uuidBytes: ByteArray = manufacturerUuid.toUuid()?.toBytes() ?: byteArrayOf()
    for (i in 2..17) {
        manufacturerData.put(i, uuidBytes[i - 2]) // adding the UUID
    }

    var majorFirstByte = 0x00.toByte()
    var majorSecondByte = 0x00.toByte()
    if (major != null) {
        majorFirstByte = ((major.toInt() shr 8).toUShort() and 0xff.toUShort()).toByte()
        majorSecondByte = (major and 0xff.toUShort()).toByte()
    }

    var minorFirstByte = 0x00.toByte()
    var minorSecondByte = 0x00.toByte()
    if (minor != null) {
        minorFirstByte = ((minor.toInt() shr 8).toUShort() and 0xff.toUShort()).toByte()
        minorSecondByte = (minor and 0xff.toUShort()).toByte()
    }

    manufacturerData.put(18, majorFirstByte) // first byte of Major
    manufacturerData.put(19, majorSecondByte) // second byte of Major
    manufacturerData.put(20, minorFirstByte) // first byte of minor
    manufacturerData.put(21, minorSecondByte) // second byte of minor
    manufacturerData.put(22, 0xB5.toByte()) // txPower

    return manufacturerData.array()
}

internal fun getManufacturerDataMask(): ByteArray {
    val manufacturerDataMask = ByteBuffer.allocate(23)

    for (i in 0..17) {
        manufacturerDataMask.put(0x01.toByte())
    }

    return manufacturerDataMask.array()
}
