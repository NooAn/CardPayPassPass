package com.nooan.cardpaypasspass

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Environment
import java.io.IOException
import java.util.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


interface OnFragmentInteractionListener {
    fun onClickReadCard(statusRead: Boolean)
}

class MainActivity : AppCompatActivity(), OnFragmentInteractionListener {
    private lateinit var nfcAdapter: NfcAdapter                                                  /*!< represents the local NFC adapter */
    private var tag: Tag? = null                                                                 /*!< represents an NFC tag that has been discovered */
    private lateinit var tagcomm: IsoDep                                                         /*!< provides access to ISO-DEP (ISO 14443-4) properties and I/O operations on a Tag */
    private val nfctechfilter = arrayOf(arrayOf(NfcA::class.java.name))      /*!<  NFC tech lists */
    private var nfcintent: PendingIntent? = null

    override fun onClickReadCard(statusRead: Boolean) {
        nfcAdapter.enableForegroundDispatch(this, nfcintent, null, nfctechfilter)
        changeState(statusRead)
    }

    fun changeState(statusRead: Boolean) {
        if (!statusRead) {
            //readCard.setText("Stop read")
            nfcAdapter.enableForegroundDispatch(this, nfcintent, null, nfctechfilter)
        } else {
            // readCard.setText("Read card")
            nfcAdapter.disableReaderMode(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (true/* is working*/) {
            super.onNewIntent(intent)
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            Log.i("EMVemulator", "Tag detected")
            cardReading(tag)
        }
    }

    private var error = "Error"
    //fixme asynchronisly
    private fun cardReading(tag: Tag?) {

        tagcomm = IsoDep.get(tag)
        tagcomm.connect()
        try {
            tagcomm.connect()
        } catch (e: IOException) {
            Log.e("EMVemulator", "Error tagcomm: " + e.message)
            error = "Reading card data ... Error tagcomm: " + e.message
            return
        }

        try {
            readCardMagStripe()
            tagcomm.close()
        } catch (e: IOException) {
            Log.e("EMVemulator", "Error tranceive: " + e.message)
            error = "Reading card data ... Error tranceive: " + e.message
            return
        }

    }


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_emulator_main -> {
                startFragmentTransaction(EmulatorTerminalMainFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_logs -> {
                startFragmentTransaction(LogsFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_reads -> {
                startFragmentTransaction(ReadFileFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcintent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

    fun appendLog(text: String, filename: String) {
        val logFile = File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EMV/", filename)
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            val buf = BufferedWriter(FileWriter(logFile, true))
            buf.append(text)
            buf.newLine()
            buf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun readCardMChip() {
        try {
            var temp: String

            val unixTime = System.currentTimeMillis() / 1000L
            val filename = unixTime.toString() + "-MChip.card"

            var response = executor(Commands.SELECT_PPSE) //Get PPSE
            appendLog("MChip", filename)
            appendLog((response.toHex()), filename)

            temp = "00 A4 04 00 07"
            temp += (response.toHex()).substring(80, 102)
            temp += "00"

            response = executor(temp)  //SELECT Command
            appendLog((response.toHex()), filename)


            response = executor("80 A8 00 00 02 83 00 00")  //Get Processing Options
            appendLog((response.toHex()), filename)

            response = executor("00 B2 01 14 00 00")   //Read Record1
            appendLog((response.toHex()), filename)

            response = executor("00 B2 01 1C 00 00")   //Read Record2
            appendLog((response.toHex()), filename)

            response = executor("00 B2 01 24 00 00")   //Read Record3
            appendLog((response.toHex()), filename)

            response = executor("00 B2 02 24 00 00")   //Read Record4
            appendLog((response.toHex()), filename)


            response = executor("80 AE 50 00 2B 00 00 00 00 00 00 00 00 00 00 00 00 00 56 00 00 00 00 00 09 78 17 06 21 00 51 33 05 49 22 00 00 00 00 00 00 00 00 00 00 1F 03 02 00")   //Generate AC
            appendLog((response.toHex()), filename)


            Log.i("EMVemulator", "Done!")

        } catch (e: IOException) {
            Log.i("EMVemulator", "Error readCard: " + e.message)
            error = "Reading card data ... Error readCard: " + e.message
        }

    }

    private fun readCardMagStripe() {

        try {
            var temp: String

            val unixTime = System.currentTimeMillis() / 1000L
            val filename = unixTime.toString() + "MagStripe.card"

            var recv = executor("00 A4 04 00 0E 32 50 41 59 2E 53 59 53 2E 44 44 46 30 31 00")
            appendLog("MagStripe", filename)
            appendLog(recv.toHex(), filename)
            temp = "00 A4 04 00 07"
            temp += recv.toHex().substring(80, 102)
            temp += "00"
            var cardtype: String = "";
            if (temp.matches("00 A4 04 00 07 A0 00 00 00 04 10 10 00".toRegex()))
                cardtype = "MasterCard"
            if (temp.matches("00 A4 04 00 07 A0 00 00 00 03 20 10 00".toRegex()))
                cardtype = "Visa Electron"
            if (temp.matches("00 A4 04 00 07 A0 00 00 00 03 10 10 00".toRegex()))
                cardtype = "Visa"
            recv = executor(temp)

            appendLog((recv.toHex()), filename)

            // appendLog(toMagStripeMode(), filename)
            recv = executor("00 B2 01 0C 00")

            appendLog((recv.toHex()), filename)
            var cardnumber: String = "";
            var cardexpiration = ""
            if (cardtype === "MasterCard") {
                cardnumber = "Card number: " + String(Arrays.copyOfRange(recv, 28, 44))
                cardexpiration = "Card expiration: " + String(Arrays.copyOfRange(recv, 50, 52)) + "/" + String(Arrays.copyOfRange(recv, 48, 50))

                for (i in 0..999) {
                    recv = executor("80 A8 00 00 02 83 00 00")
                    temp = "802A8E800400000"
                    temp += String.format("%03d", i)
                    temp += "00"
                    temp = temp.replace("..(?!$)".toRegex(), "$0 ")
                    recv = executor(temp)

                    appendLog((recv.toHex()), filename)
                    if (i % 1 == 0) {
                        Log.i("EMVemulator", "Count:" + i.toString())
                    }
                }
            }
            if (cardtype === "Visa" || cardtype === "Visa Electron") {
                cardnumber = "Card number: " + (recv.toHex()).substring(12, 36).replace(" ", "")
                cardexpiration = "Card expiration: " + (recv.toHex()).substring(40, 43).replace(" ", "") + "/" + recv.toHex().substring(37, 40).replace(" ", "")
            }

            Log.i("EMVemulator", "Done!")

        } catch (e: IOException) {
            Log.i("EMVemulator", "Error readCard: " + e.message)
            error = "Reading card data ... Error readCard: " + e.message
        }

    }

    @Throws(IOException::class)
    protected fun executor(command: Command): ByteArray {
        val bytes = command.split()
        Log.i("EMVemulator", "Send: " + (bytes.toHex()))
        val recv = tagcomm.transceive(bytes)
        Log.i("EMVemulator", "Received: " + (recv.toHex()))
        return recv
    }

    private fun startFragmentTransaction(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, fragment)
                .commit()
    }
}
