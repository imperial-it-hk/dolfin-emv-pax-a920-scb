package com.pax.pay.extensions

import android.util.Base64

const val byteArray_availableHexCharacters = "0123456789ABCDEF"

fun ByteArray.toHexString(): String {
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(byteArray_availableHexCharacters[firstIndex])
        result.append(byteArray_availableHexCharacters[secondIndex])
    }

    return result.toString()
}

fun ByteArray.encodeToBase64String(): String {
    return Base64.encodeToString(this, Base64.DEFAULT)
}