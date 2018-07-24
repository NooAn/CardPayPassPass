package com.nooan.cardpaypasspass

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface.BOLD
import android.graphics.Typeface.ITALIC
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.binaryfoo.RootDecoder
import io.github.binaryfoo.cmdline.DecodedWriter
import kotlinx.android.synthetic.main.fragment_logs.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets


private const val LIST = "param1"
private const val ARG_PARAM2 = "param2"


class LogsFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null

    private var list: java.util.ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            list = it.getStringArrayList(LIST)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    var stringBuilder = StringBuilder()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listener: (text: String) -> Unit = {
            val url = "http://www.emvlab.org/tlvutils/?data=$it";
            val i = Intent(Intent.ACTION_VIEW);
            i.data = Uri.parse(url);
            startActivity(i);
        }
        if (stringBuilder.isEmpty()) {
            list?.let {
                setTextLog(it)
            }
        }
        if (tvLog != null && stringBuilder.isNotEmpty()) {
            var logText = stringBuilder.toString()
            val spannable = SpannableString(logText)
            var start = 0
            var end = 0
            for (i in 0 until logText.length step 1) {
                if (logText[i] == '#') start = i
                if (logText[i] == '\n') end = i
                if (start < end && start != -1) {
                    spannable.setSpan(ForegroundColorSpan(Color.BLUE),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(
                            StyleSpan(ITALIC),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val s = logText.substring(start + 1, end)
                    spannable.setSpan(object : ClickableSpan() {
                        override fun onClick(view: View) {
                            listener(s)
                        }
                    }, start + 1, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    start = -1
                    end = -1
                }
                if (logText[i] == '[') start = i
                if (logText[i] == '(') end = i
                if (start < end && start != -1) {
                    spannable.setSpan(
                            StyleSpan(BOLD),
                            start + 1, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(
                            ForegroundColorSpan(Color.BLACK),
                            start + 1, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    start = -1
                    end = -1
                }
                if (logText[i] == '(') start = i
                if (logText[i] == ')') end = i
                if (start < end && start != -1) {
                    spannable.setSpan(
                            ForegroundColorSpan(Color.BLACK),
                            start, end + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    start = -1
                    end = -1
                }
            }
            tvLog.setMovementMethod(LinkMovementMethod.getInstance());

            tvLog.text = spannable
        }
    }


    fun setTextLog(listText: ArrayList<String>) {
        stringBuilder = StringBuilder()
        val decoder = RootDecoder()
        listText.forEach { text ->
            val decoded = decoder.decode(text, "EMV", "constructed")
            val stream = ByteArrayOutputStream()
            val ps = PrintStream(stream, true, "UTF-8")
            DecodedWriter(ps).write(decoded, "")
            stringBuilder.append("#").append(text).append("\n\n")
            stringBuilder.append(String(stream.toByteArray(), StandardCharsets.UTF_8)).append("\n\n")
            ps.close()
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

    companion object {
        val TAG: String = "LogsFragment"

        @JvmStatic
        fun newInstance(param1: ArrayList<String>) =
                LogsFragment().apply {
                    arguments = Bundle().apply {
                        putStringArrayList(LIST, param1)
                    }
                }
    }
}


