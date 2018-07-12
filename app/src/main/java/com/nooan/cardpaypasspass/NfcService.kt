package com.nooan.cardpaypasspass

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class NfcService : HostApduService() {
    override fun onDeactivated(reason: Int) {
        Log.e("LOG onDEACTIVATED", reason.toString());

    }

    override fun processCommandApdu(apdu: ByteArray?, bundle: Bundle?): ByteArray {
        Log.e("LOG", apdu.toString())
        return byteArrayOf();
    }

}