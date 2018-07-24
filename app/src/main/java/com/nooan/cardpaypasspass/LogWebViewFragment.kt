package com.nooan.cardpaypasspass

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_web_view_log.*

private const val URL = "param1"
private const val ARG_PARAM2 = "param2"

class LogWebViewFragment : Fragment() {
    private var url: String? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            url = it.getString(URL)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_web_view_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // включаем поддержку JavaScript
        // указываем страницу загрузки
        webView.loadUrl("http://www.emvlab.org/tlvutils/?data=$url");

        initWebView()

    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    private fun initWebView() {
        val settings = webView.settings
        settings.domStorageEnabled = true
        settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
                super.onPageStarted(view, url, favicon)

            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

            }

            override fun onReceivedError(view: WebView,
                                         errorCode: Int,
                                         description: String,
                                         failingUrl: String) {
                super.onReceivedError(view, errorCode, description, failingUrl)

            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true;
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.e("LOG", "on Attach");
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
        fun newInstance(url: String = "") =
                LogsFragment().apply {
                    arguments = Bundle().apply {
                        putString(URL, url)
                    }
                }
    }
}
