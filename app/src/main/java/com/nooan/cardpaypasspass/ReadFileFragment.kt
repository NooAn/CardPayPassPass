package com.nooan.cardpaypasspass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_read_file.*
import java.io.File
import java.io.IOException


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
        return inflater.inflate(R.layout.fragment_read_file, container, false)

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
            listener?.writteTextInFile(File(Environment.getExternalStorageDirectory().absolutePath + "/EMV/", filename), etCommandLine.text.toString(), false)
            Toast.makeText(activity, "Saved", Toast.LENGTH_SHORT).show()
        }
        btnSend.setOnClickListener {
            val commands = getCommands(etCommandLine.text.toString(), this::detailText)
            if (commands.isNotEmpty()) {
                listener?.openReaderFragment()
                listener?.setNewCommands(commands = commands)
            }
        }
    }

    private fun detailText(text: String, it: String) {
        val wordtoSpan = SpannableString(text)
        wordtoSpan.setSpan(ForegroundColorSpan(Color.YELLOW), text.indexOf(it), text.indexOf(it) + it.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        etCommandLine.setText(wordtoSpan)
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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    var uri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                uri = resultData.data
                Log.i(TAG, "Uri: " + uri!!.toString())
                val commandsText = uri!!.readTextFromUri(activity)
                etCommandLine.text.clear()
                etCommandLine.setText(commandsText)
                etNameOfFile.setText(getFileName(uri!!))
            }
        }
    }

    fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = activity?.contentResolver?.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    companion object {
        val TAG: String = "ReadFile"
        val READ_REQUEST_CODE = 42

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

private fun String.commandTransform(): Command {
    val command = Command()
    command.CLA = "${this[0]}${this[1]}"
    command.INS = "${this[2]}${this[3]}"
    command.P1 = "${this[4]}${this[5]}"
    command.P2 = "${this[6]}${this[7]}"
    command.Lc = "${this[8]}${this[9]}"
    command.Nc = this.substring(10, length)
    return command
}

@Throws(IOException::class)
fun Uri.readTextFromUri(context: Context?): String {
    val inputStream = context?.contentResolver?.openInputStream(this)
    val inputAsString = inputStream?.bufferedReader().use { it?.readText() }
    inputStream?.close()
    return inputAsString.toString()
}

fun getCommands(text: String, func: (t: String, s: String) -> Unit): List<Command> {
    val listLine = text.split("\n")
    val listCommand: ArrayList<Command> = arrayListOf()
    listLine.forEach {
        if (!it.startsWith("#")) {
            var line = it.replace("\\s".toRegex(), "").toUpperCase()
            line.map { char ->
                if (char !in HEX_CHARS_ARRAY) {
                    func(text, it)
                    return arrayListOf()
                }
            }
            if (line.length < 9 && line != "") {
                func(text, it)
                return arrayListOf()
            }
            if (line != "")
                listCommand.add(line.commandTransform())
        }
    }
    return listCommand
}

