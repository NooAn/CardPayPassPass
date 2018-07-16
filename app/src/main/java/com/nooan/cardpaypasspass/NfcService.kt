package com.nooan.cardpaypasspass

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class NfcService : HostApduService() {
    companion object {
        val TAG = "Host Card Emulator"
        val STATUS_SUCCESS = "9000"
        val STATUS_FAILED = "6F00"
        val CLA_NOT_SUPPORTED = "6E00"
        val INS_NOT_SUPPORTED = "6D00"
        val AID = "A0000002471001"
        val SELECT_INS = "A4"
        val DEFAULT_CLA = "00"
        val MIN_APDU_LENGTH = 12
    }

    override fun onDeactivated(reason: Int) {
        Log.e("LOG onDEACTIVATED", reason.toString());
    }

    override fun processCommandApdu(apdu: ByteArray?, bundle: Bundle?): ByteArray {
        Log.i(TAG, "Received APDU: $apdu - String Representation: ${if (apdu != null) String(apdu) else "null"}")
        return byteArrayOf();
    }

}

private val HEX_CHARS = "0123456789ABCDEF"

fun String.hexToByteArray2(): ByteArray {

    val result = ByteArray(this.length / 2)

    for (i in 0 until this.length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i]);
        val secondIndex = HEX_CHARS.indexOf(this[i + 1]);

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }
    return result
}

fun String.hexToByteArray(): ByteArray {

    val hexbytes = this.replace(" ", "")
    val result = ByteArray(hexbytes.length / 2)

    for (i in 0 until hexbytes.length step 2) {
        val firstIndex = HEX_CHARS.indexOf(hexbytes[i]);
        val secondIndex = HEX_CHARS.indexOf(hexbytes[i + 1]);

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }
    return result
}

private val HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray()
fun ByteArray.toHex(): String {
    val result = StringBuffer()

    this.forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS_ARRAY[firstIndex])
        result.append(HEX_CHARS_ARRAY[secondIndex])
    }

    return result.toString()
}