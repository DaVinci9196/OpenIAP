package org.mg.iap.ui

import android.accounts.Account
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mg.iap.ADD_PAYMENT_METHOD_URL
import org.mg.iap.BillingResult
import org.mg.iap.ContextProvider
import org.mg.iap.LogUtils
import org.mg.iap.ui.logic.NotificationEventId
import org.mg.iap.ui.logic.SheetUIViewModel

private const val ADD_PAYMENT_REQUEST_CODE = 30002

class SheetUIHostActivity : ComponentActivity() {
    private val sheetUiViewModel by viewModels<SheetUIViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtils.d("SheetUiBuilderHostActivity.onCreate")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            initEventHandler()
        }
        loadView(savedInstanceState != null)
    }

    private fun loadView(isRebuild: Boolean) {
        if (!isRebuild) {
            sheetUiViewModel.startParams = intent.extras
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    sheetUiViewModel.loadSheetUIData()
                } catch (e: Exception) {
                    LogUtils.d("loadSheetUIViewData", e)
                    withContext(Dispatchers.Main) {
                        finishWithResult(
                            bundleOf(
                                "RESPONSE_CODE" to BillingResult.DEVELOPER_ERROR.ordinal,
                                "DEBUG_MESSAGE" to "init ui failed"
                            )
                        )
                    }
                }
            }
        }
        initWindow()
        setContent { SheetUIPage(viewModel = sheetUiViewModel) }
    }

    private suspend fun initEventHandler() {
        sheetUiViewModel.event.collect {
            when (it.id) {
                NotificationEventId.FINISH -> finishWithResult(it.params)
                NotificationEventId.OPEN_PAYMENT_METHOD_ACTIVITY -> {
                    val account = it.params.getParcelable<Account>("account")
                    val src = it.params.getString("src")
                    openPaymentMethodActivity(src, account)
                }
            }
        }
    }

    private fun initWindow() {
        val lp = window.attributes
        lp.width = getWindowWidth()
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        window.attributes = lp
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (ContextProvider.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }

    private fun openPaymentMethodActivity(src: String?, account: Account?) {
        val intent = Intent(this, PlayWebView::class.java)
        intent.putExtra(KEY_WEBVIEW_ACTION, WebViewAction.ADD_PAYMENT_METHOD.toString())
        intent.putExtra(KEY_WEBVIEW_OPEN_URL, ADD_PAYMENT_METHOD_URL)
        account?.let {
            intent.putExtra(KEY_WEBVIEW_ACCOUNT, account)
        }
        startActivityForResult(intent, ADD_PAYMENT_REQUEST_CODE)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_UP -> {
                val r = Rect(0, 0, 0, 0)
                this.window.decorView.getHitRect(r)
                val intersects: Boolean = r.contains(event.x.toInt(), event.y.toInt())
                if (!intersects) {
                    sheetUiViewModel.close()
                    return true
                }
                super.onTouchEvent(event)
            }

            else -> super.onTouchEvent(event)
        }
    }

    private fun finishWithResult(result: Bundle) {
        LogUtils.d("SheetUIHostActivity.finishWithResult $result")
        val resultIntent = Intent()
        resultIntent.putExtras(result)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ADD_PAYMENT_REQUEST_CODE -> {
                LogUtils.d("add payment method resultCode: $resultCode, data: $data")
                loadView(false)
            }

            else -> {
                super.onActivityResult(requestCode, resultCode, data)
                finishWithResult(
                    bundleOf(
                        "RESPONSE_CODE" to BillingResult.USER_CANCELED.ordinal
                    )
                )
            }
        }
    }
}