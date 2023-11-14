package org.mg.iap.ui

import android.accounts.Account
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import org.mg.iap.LogUtils

private val ALLOWED_WEB_PREFIXES = setOf(
    "https://play.google.com/accounts",
    "https://play.google.com/store/paymentmethods",
    "https://play.google.com/store/paymentmethods",
    "https://pay.google.com/payments",
    "https://payments.google.com/payments"
)

enum class WebViewAction {
    ADD_PAYMENT_METHOD,
    UNKNOWN
}

const val KEY_WEBVIEW_ACTION = "key_webview_action"
const val KEY_WEBVIEW_OPEN_URL = "key_webview_open_url"
const val KEY_WEBVIEW_ACCOUNT = "key_webview_account"

class PlayWebView : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var openUrl: String
    private lateinit var action: WebViewAction
    private var account: Account? = null

    private fun parseParams(): Boolean {
        val actionStr = intent.getStringExtra(KEY_WEBVIEW_ACTION) ?: return false
        this.action = try {
            WebViewAction.valueOf(actionStr)
        } catch (e: Exception) {
            LogUtils.d("", e)
            return false
        }
        LogUtils.d("PlayWebView action:$action")
        this.openUrl = intent.getStringExtra(KEY_WEBVIEW_OPEN_URL) ?: return false
        if (openUrl.isBlank())
            return false
        this.account = intent.getParcelableExtra(KEY_WEBVIEW_ACCOUNT)
        return true
    }

    private fun doAction() {
        when (action) {
            WebViewAction.ADD_PAYMENT_METHOD -> createWebView()
            else -> {
                LogUtils.d("PlayWebView unknown action:$action")
                finish()
            }
        }
    }

    private fun createWebView() {
        val layout = RelativeLayout(this)
        layout.addView(ProgressBar(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            isIndeterminate = true
        })
        webView = WebView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.INVISIBLE
        }
        layout.addView(webView)
        setContentView(layout)
        WebViewHelper(this, webView, ALLOWED_WEB_PREFIXES).openWebView(openUrl, account)
        setResult(RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val extras = intent?.extras?.also { it.keySet() }
        LogUtils.d("Invoked with ${intent.action} and extras $extras")
        super.onCreate(savedInstanceState)
        if (!parseParams()) {
            finish()
            return
        }
        doAction()
    }
}