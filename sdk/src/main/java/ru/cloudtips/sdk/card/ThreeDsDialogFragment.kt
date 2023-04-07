package ru.cloudtips.sdk.card

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.gson.JsonParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.cloudtips.sdk.amplitude
import ru.cloudtips.sdk.databinding.DialogCpsdkThreeDsBinding
import ru.cloudtips.sdk.helpers.PayType
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

class ThreeDsDialogFragment : DialogFragment() {
    interface ThreeDSDialogListener {
        fun onAuthorizationCompleted(md: String, paRes: String, payType: PayType)
        fun onAuthorizationFailed(error: String?, payType: PayType)
    }

    companion object {
        private const val POST_BACK_URL = "https://demo.cloudpayments.ru/WebFormPost/GetWebViewData"
        private const val ARG_ACS_URL = "acs_url"
        private const val ARG_MD = "md"
        private const val ARG_PA_REQ = "pa_req"
        private const val ARG_PAY_TYPE = "pay_type"

        fun newInstance(acsUrl: String, paReq: String, md: String, payType: PayType) = ThreeDsDialogFragment().apply {
            arguments = Bundle().also {
                it.putString(ARG_ACS_URL, acsUrl)
                it.putString(ARG_MD, md)
                it.putString(ARG_PA_REQ, paReq)
                it.putSerializable(ARG_PAY_TYPE, payType)
            }
        }
    }

    private val binding: DialogCpsdkThreeDsBinding by viewBinding()

    private val acsUrl by lazy {
        requireArguments().getString(ARG_ACS_URL) ?: ""
    }

    private val md by lazy {
        requireArguments().getString(ARG_MD) ?: ""
    }

    private val paReq by lazy {
        requireArguments().getString(ARG_PA_REQ) ?: ""
    }

    private val payType by lazy {
        (requireArguments().getParcelable(ARG_PAY_TYPE) as? PayType) ?: PayType.CARD
    }

    private var listener: ThreeDSDialogListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        binding.webView.webViewClient = ThreeDsWebViewClient()
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.webView.addJavascriptInterface(ThreeDsJavaScriptInterface(), "JavaScriptThreeDs")

        try {
            val params = StringBuilder()
                .append("PaReq=").append(URLEncoder.encode(paReq, "UTF-8"))
                .append("&MD=").append(URLEncoder.encode(md, "UTF-8"))
                .append("&TermUrl=").append(URLEncoder.encode(POST_BACK_URL, "UTF-8"))
                .toString()
            binding.webView.postUrl(acsUrl, params.toByteArray())
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        binding.icClose.setOnClickListener {
            dismiss()
        }
        amplitude.track3dsOpen(payType)
    }

    override fun onStart() {
        super.onStart()
        val window = dialog!!.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private inner class ThreeDsWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            if (url.toLowerCase(Locale.getDefault()) == POST_BACK_URL.toLowerCase(Locale.getDefault())) {
                view.isGone = true
                view.loadUrl("javascript:window.JavaScriptThreeDs.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');")
            }
        }
    }

    internal inner class ThreeDsJavaScriptInterface {
        @JavascriptInterface
        fun processHTML(html: String?) {
            val doc: Document = Jsoup.parse(html)
            val element: Element = doc.select("body").first()
            val jsonObject = JsonParser().parse(element.ownText()).asJsonObject
            val paRes = jsonObject["PaRes"].asString
            requireActivity().runOnUiThread {
                if (!paRes.isNullOrEmpty()) {
                    listener?.onAuthorizationCompleted(md, paRes, payType)
                } else {
                    listener?.onAuthorizationFailed(html ?: "", payType)
                }
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = targetFragment as? ThreeDSDialogListener
        if (listener == null) {
            listener = context as? ThreeDSDialogListener
        }
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

        listener = targetFragment as? ThreeDSDialogListener
        if (listener == null) {
            listener = activity as? ThreeDSDialogListener
        }
    }
}