package com.nooan.cardpaypasspass

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_emulator_terminal.*
import kotlinx.android.synthetic.main.fragment_read_file.*
import android.content.Intent
import android.app.Activity
import android.os.Environment
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ReadFileFragment : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_read_file, container, false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
    }

    private fun initView() {
        btnSelectFile.setOnClickListener {
            performFileSearch()
        }
        btnSaveFile.setOnClickListener {
            val filename = if (etNameOfFile.text.isBlank()) "MyCommands.txt" else etNameOfFile.text.toString()
            listener?.writteTextInFile(File(Environment.getExternalStorageDirectory().absolutePath + "/EMV/", filename), etCommandLine.text.toString())
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


    fun performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        intent.type = "*/*"

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        val inputStream = activity?.getContentResolver()?.openInputStream(uri)
        val inputAsString = inputStream?.bufferedReader().use { it?.readText() }
        inputStream?.close()
        return inputAsString.toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            var uri: Uri? = null
            if (resultData != null) {
                uri = resultData.data
                Log.i(TAG, "Uri: " + uri!!.toString())
                //showImage(uri)
                val commandsText = readTextFromUri(uri)
                etCommandLine.text.clear()
                etCommandLine.setText(commandsText)
                etNameOfFile.setText(uri.lastPathSegment)
            }
        }
    }

    companion object {
        val TAG: String = "ReadFile"
        val READ_REQUEST_CODE = 42
        val WRITE_REQUEST_CODE = 43
        val EDIT_REQUEST_CODE = 44


        @JvmStatic
        fun newInstance(param1: String = "", param2: String = "") =
                ReadFileFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
