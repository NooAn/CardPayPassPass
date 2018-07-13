package com.nooan.cardpaypasspass

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_emulator_terminal.*
import android.app.PendingIntent
import android.nfc.tech.NfcA
import android.nfc.tech.IsoDep
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.content.Intent


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class EmulatorTerminalMainFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var nfcAdapter: NfcAdapter                                                              /*!< represents the local NFC adapter */
    private val tag: Tag? = null                                                                            /*!< represents an NFC tag that has been discovered */
    private val tagcomm: IsoDep? = null                                                                     /*!< provides access to ISO-DEP (ISO 14443-4) properties and I/O operations on a Tag */
    private val nfctechfilter = arrayOf(arrayOf(NfcA::class.java.name))      /*!<  NFC tech lists */
    private var nfcintent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_emulator_terminal, container, false)
        initView()
        return view
    }

    private fun initView() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        nfcintent = PendingIntent.getActivity(activity, 0, Intent(activity, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        btnRead.setOnClickListener {
            txReading.visibility = View.VISIBLE
            nfcAdapter.enableForegroundDispatch(activity, nfcintent, null, nfctechfilter)
        }
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String = "", param2: String = "") =
                EmulatorTerminalMainFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
