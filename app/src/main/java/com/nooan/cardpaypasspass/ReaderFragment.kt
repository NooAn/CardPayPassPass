package com.nooan.cardpaypasspass

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_emulator_terminal.*


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ReaderFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null


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
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
    }

    private var statusRead: Boolean = false

    private fun initView() {
        if (statusRead) showStopRead()
        tvLogs.text = Html.fromHtml("<p> ${(activity as MainActivity).log} <p> ")
        btnRead.setOnClickListener {
            txDescription.visibility = View.VISIBLE
            listener?.onClickReadCard(statusRead)
        }
        checkboxChip.setOnClickListener {
            (activity as MainActivity).mChip = checkboxChip.isChecked
        }
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

    @SuppressLint("SetTextI18n")
    fun showStartRead() {
        if (btnRead != null) {
            statusRead = !statusRead
            btnRead.text = "STOP READ"
        }
    }

    @SuppressLint("SetTextI18n")
    fun showStopRead() {
        statusRead = !statusRead
        btnRead.text = "READ"
    }

    fun showLogs(text: String) {
        tvLogs.text = Html.fromHtml("<p> $text <p> ")
    }

    fun showData(cardnumber: String, cardexpiration: String) {
        txDescription.text = "$cardnumber \n$cardexpiration"
    }

    companion object {
        val TAG = "READER"
        @JvmStatic
        fun newInstance(param1: String = "", param2: String = "") =
                ReaderFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
