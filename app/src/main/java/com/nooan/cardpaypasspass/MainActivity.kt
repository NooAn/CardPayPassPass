package com.nooan.cardpaypasspass

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


interface OnFragmentInteractionListener {
    fun onClickReadCard(statusRead: Boolean)
    fun writteTextInFile(filename: File, text: String)
}

class MainActivity : AppCompatActivity(), OnFragmentInteractionListener {

    private var nfcAdapter: NfcAdapter? = null                                                  /*!< represents the local NFC adapter */
    private var tag: Tag? = null                                                                 /*!< represents an NFC tag that has been discovered */
    private lateinit var tagcomm: IsoDep                                                         /*!< provides access to ISO-DEP (ISO 14443-4) properties and I/O operations on a Tag */
    private val nfctechfilter = arrayOf(arrayOf(NfcA::class.java.name))      /*!<  NFC tech lists */
    private var nfcintent: PendingIntent? = null

    override fun onClickReadCard(statusRead: Boolean) {
        changeState(statusRead)
    }

    fun changeState(statusRead: Boolean) {
        if (!statusRead) {
            (supportFragmentManager.findFragmentByTag(ReaderFragment.TAG) as ReaderFragment).startRead()
            nfcAdapter?.enableForegroundDispatch(this, nfcintent, null, nfctechfilter)
        } else {
            finishRead()
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

    var log: String = "";
    private fun showLogs(text: String) {
        log += "\n ${text}"
        val fragment = supportFragmentManager.findFragmentByTag(ReaderFragment.TAG)
        if (fragment != null) {
            (fragment as ReaderFragment).showLogs(log)
        }
    }

    private var error = ""
    //fixme asynchronisly
    private fun cardReading(tag: Tag?) {

        tagcomm = IsoDep.get(tag)
        try {
            tagcomm.connect()
        } catch (e: IOException) {
            Log.e("EMVemulator", "Error tagcomm: " + e.message)
            error = "Reading card data ... Error tagcomm: " + e.message
            Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (mChip)
                readCardMChip()
            else
                readCardMagStripe()
        } catch (e: IOException) {
            Log.e("EMVemulator", "Error tranceive: " + e.message)
            error = "Reading card data ... Error tranceive: " + e.message
            Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            return
        } finally {
            tagcomm.close()
        }

    }


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_emulator_main -> {
                fragmentTransaction(ReaderFragment.newInstance(), ReaderFragment.TAG)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_logs -> {
                fragmentTransaction(LogsFragment.newInstance(), LogsFragment.TAG)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_reads -> {
                fragmentTransaction(ReadFileFragment.newInstance(), ReadFileFragment.TAG)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isStoragePermissionGranted();
        setContentView(R.layout.activity_main)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcintent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        fragmentTransaction(ReaderFragment.newInstance(), ReaderFragment.TAG)
        createDir()
    }

    fun createDir() {
        val emvDir = File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EMV/")
        emvDir.mkdirs()
    }

    fun appendLog(text: String, filename: String) {
        val logFile = File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EMV/", filename)
        writteTextInFile(logFile, text)
    }

    override fun writteTextInFile(filename: File, text: String) {
        if (!filename.exists()) {
            try {
                filename.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            val buf = BufferedWriter(FileWriter(filename, true))
            buf.append(text)
            buf.newLine()
            buf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("LOG", "Permission is granted")
                return true
            } else {

                Log.v("LOG", "Permission is revoked")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                return false
            }
        } else {
            Log.v("LOG", "Permission is granted")
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("LOG", "Permission: " + permissions[0] + "was " + grantResults[0])
        }
    }

    val unixTime = System.currentTimeMillis() / 1000L
    val filename = unixTime.toString() + "-MChip.card"
    var mChip: Boolean = false

    private fun readCardMChip() {
        try {

            appendLog("MChip", filename)

            var response = execute(Commands.SELECT_PPSE) //Get PPSE

            response = execute(Commands.SELECT_APPLICATION.apply {
                Nc = response.toHex().substring(80, 102)
                SW1WS2 = "00"
            })

            response = execute(Commands.GET_PROCESSING_OPTIONS)  //Get Processing Options

            response = execute(Commands.READ_RECORD_1)   //Read Record1

            response = execute(Commands.READ_RECORD_2)   //Read Record2

            response = execute(Commands.READ_RECORD_3)   //Read Record3

            response = execute(Commands.READ_RECORD_4)   //Read Record4

            response = execute(Command().apply {
                CLA = "80"
                INS = "AE"
                P1 = "50"
                P2 = "00"
                Lc = "2B"
                Nc = "00 00 00 00 00 00 00 00 00 00 00 00 00 56 00 00 00 00 00 09 78 17 06 21 00 51 33 05 49 22 00 00 00 00 00 00 00 00 00 00 1F 03 02 00"
            })   //Generate ACs
        } catch (e: IOException) {
            Log.i("EMVemulator", "Error readCard: " + e.message)
            error = "Reading card data ... Error readCard: " + e.message
        }

    }

    protected fun toMagStripeMode() = "77 0A 82 02 00 00 94 04 08 01 01 00 90 00"

    private fun readCardMagStripe() {

        try {
            val unixTime = System.currentTimeMillis() / 1000L
            val filename = unixTime.toString() + "MagStripe.card"
            appendLog("MagStripe", filename)

            var response = execute(Commands.SELECT_PPSE)

            val select = Commands.SELECT_APPLICATION.apply {
                Nc = response.toHex().substring(52, 68)
                SW1WS2 = "00"
            }
            val cardtype: String = getTypeCard(select.split())

            response = execute(select)
            appendLog(toMagStripeMode(), filename)
            response = execute(Commands.READ_RECORD_1.apply {
                P2 = "0C"
                Lc = "00"
                Le = ""
                Nc = ""
            })

            if (cardtype === "MasterCard") {

                cardnumber = "Card number: ${response.getCards()}"
                cardexpiration = "Card expiration: ${response.getExpired()}"

                showData()

                for (i in 0..3) {
                    response = execute(Commands.GET_PROCESSING_OPTIONS, false)
                    response = execute(Commands.COMPUTE_CRYPTOGRAPHIC_CHECKSUM.apply {
                        Lc = "04"
                        Nc = "00000${String.format("%03d", i)}".replace("..(?!$)".toRegex(), "$0 ")
                    })

                    appendLog((response.toHex()), filename)
                    Log.i("EMVemulator", "Count:" + i.toString())
                    if (i % 1 == 0) {
                    }
                }
                showLogs("<b><b></b></b>")
            }
            if (cardtype === "Visa" || cardtype === "Visa Electron") {
                cardnumber = "Card number: " + (response.toHex()).substring(12, 36).replace(" ", "")
                cardexpiration = "Card expiration: " + (response.toHex()).substring(40, 43).replace(" ", "") + "/" + response.toHex().substring(37, 40).replace(" ", "")
            }

            Log.i("EMVemulator", "Done!")
            finishRead()

        } catch (e: IOException) {
            Log.i("EMVemulator", "Error readCard: " + e.message)
            error = "Reading card data ... Error readCard: " + e.message
        }

    }

    private fun finishRead() {
        (supportFragmentManager.findFragmentByTag(ReaderFragment.TAG) as ReaderFragment).stopRead()
        nfcAdapter?.disableReaderMode(this)
    }

    var cardnumber: String = "";
    var cardexpiration = ""

    private fun showData() {
        val fragment = supportFragmentManager.findFragmentByTag(ReaderFragment.TAG)
        if (fragment != null) {
            (fragment as ReaderFragment).showData(cardnumber, cardexpiration)
        }
    }

    private fun getTypeCard(response: ByteArray): String {
        var cardtype: String = "";
        if (response.toHex().matches("00A4040007A000000004101000".toRegex()))
            cardtype = "MasterCard"
        if (response.toHex().matches("00A4040007A000000003201000".toRegex()))
            cardtype = "Visa Electron"
        if (response.toHex().matches("00A4040007A000000003101000".toRegex()))
            cardtype = "Visa"
        return cardtype
    }

    @Throws(IOException::class)
    protected fun execute(command: Command, log: Boolean = true): ByteArray {
        val bytes = command.split()
        val sending = "<h3><font color=#cc0029>Sent:</font></h3> " + bytes.toHex().makePair()
        Log.i("EMVemulator", sending)
        showLogs(sending)

        val recv = tagcomm.transceive(bytes)
        val received = "<h3><font color=#cc0029>Received:</font></h3> " + recv.toHex().makePair()
        Log.i("EMVemulator", received)
        showLogs(received)
        if (log) appendLog((recv.toHex()), filename)
        return recv
    }

    private var oldScreen = ""
    private fun fragmentTransaction(newFragment: Fragment, tag: String? = null) {
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        val transaction = createDetachTransaction(oldScreen)
        oldScreen = tag ?: ""
        if (fragment == null)
            supportFragmentManager.beginTransaction()
                    .add(R.id.mainFrame, newFragment, tag)
                    .addToBackStack(tag)
                    .commit()
        else {
            transaction.attach(fragment)
            transaction.commit()
        }
    }

    private fun createDetachTransaction(tag: String?): FragmentTransaction {
        var fragmentTransaction = supportFragmentManager.beginTransaction()
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        if (fragment != null) {
            fragmentTransaction = fragmentTransaction.detach(fragment)
        }
        return fragmentTransaction
    }
}

private fun ByteArray.getCards() = this.toHex().substring(190, 206)

private fun ByteArray.getExpired() = this.toHex().substring(209, 213)

fun String.makePair(): String? {
    var stringBuilder = StringBuilder()
    for (i in 1..this.length - 1 step 2) {
        stringBuilder.append(this.get(i - 1)).append(this.get(i)).append(" ")

    }
    return stringBuilder.toString()
}

