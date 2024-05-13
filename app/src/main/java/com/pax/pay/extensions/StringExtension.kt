package com.pax.pay.extensions

import android.util.Base64

const val string_availableHexCharacters = "0123456789ABCDEF"

fun String.toByteArrayOfHexString(): ByteArray {
    val newLength = if (length % 2 == 0) { length } else { length + 1 }
    val paddedValue = this.padStart(newLength, '0')
    val result = ByteArray(newLength / 2)

    for (i in result.indices) {
        val firstIndex = string_availableHexCharacters.indexOf(paddedValue[i * 2].toUpperCase());
        val secondIndex = string_availableHexCharacters.indexOf(paddedValue[(i * 2) + 1].toUpperCase());

        val octet = firstIndex.shl(4).or(secondIndex)
        result[i] = octet.toByte()
    }

    return result
}

fun String.decodeToByteArray(): ByteArray {
    return Base64.decode(this, Base64.DEFAULT)
}

