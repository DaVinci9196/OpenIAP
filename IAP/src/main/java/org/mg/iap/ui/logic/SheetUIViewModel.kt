package org.mg.iap.ui.logic

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.mg.iap.BillingResult
import org.mg.iap.ContextProvider
import org.mg.iap.IAPImpl
import org.mg.iap.KEY_IAP_SHEET_UI_ACTION
import org.mg.iap.KEY_IAP_SHEET_UI_PARAM
import org.mg.iap.LogUtils
import org.mg.iap.R
import org.mg.iap.SettingsManager
import org.mg.iap.core.toHex
import org.mg.iap.core.ui.ActionType
import org.mg.iap.core.ui.BAction
import org.mg.iap.core.ui.UIType
import org.mg.iap.decodeHex
import org.mg.iap.resultBundle

enum class SheetUIAction {
    LAUNCH_BUY_FLOW,
    SUBMIT_BUY_ACTION,
    UNKNOWN
}

enum class NotificationEventId {
    FINISH,
    OPEN_PAYMENT_METHOD_ACTIVITY
}

data class NotificationEvent(
    val id: NotificationEventId,
    val params: Bundle
)

class SheetUIViewModel : ViewModel() {
    private val _event = Channel<NotificationEvent>()
    val event = _event.receiveAsFlow()
    var startParams: Bundle? = null

    var loadingDialogVisible by mutableStateOf(value = false)
        private set
    var sheetUIViewState by mutableStateOf(
        value = SheetUIViewState(
            onClickAction = ::handleClickAction
        )
    )
        private set
    var passwdInputViewState by mutableStateOf(
        value = PasswdInputViewState(
            onDismissRequest = ::handlePasswdInputViewDismiss,
            onButtonClicked = ::handlePasswdInput,
            onCheckedChange = ::handlePasswdCheckedChange
        )
    )
    private lateinit var lastBuyFlowResult: BuyFlowResult

    private fun finishWithResult(result: Bundle) {
        viewModelScope.launch {
            _event.send(NotificationEvent(NotificationEventId.FINISH, result))
        }
    }

    private fun handlePasswdInputViewDismiss() {
        LogUtils.d("handlePasswdInputViewDismiss called")
        passwdInputViewState = passwdInputViewState.copy(visible = false)
    }

    private suspend fun submitBuyAction(authToken: String? = null) {
        val param = startParams?.getString(KEY_IAP_SHEET_UI_PARAM) ?: return finishWithResult(
            sheetUIViewState.result
        )
        val buyFlowResult =
            IAPImpl.acquireRequest(param, sheetUIViewState.actionContextList, authToken)
        handleBuyFlowResult(buyFlowResult)
    }

    private suspend fun doAcquireRequest() {
        val param = startParams?.getString(KEY_IAP_SHEET_UI_PARAM) ?: return finishWithResult(
            sheetUIViewState.result
        )
        val buyFlowResult = IAPImpl.acquireRequest(param, sheetUIViewState.actionContextList)
        handleBuyFlowResult(buyFlowResult)
    }

    private fun handlePasswdInput(passwd: String) {
        LogUtils.d("handlePasswdInput passwd: $passwd")
        loadingDialogVisible = true
        passwdInputViewState = passwdInputViewState.copy(visible = false)
        sheetUIViewState = sheetUIViewState.copy(visible = false)
        viewModelScope.launch(Dispatchers.IO) {
            val param = startParams!!.getString(KEY_IAP_SHEET_UI_PARAM)
            val (statusCode, encodedRapt) = IAPImpl.requestAuthProofToken(param!!, passwd)
            LogUtils.d("requestAuthProofToken statusCode=$statusCode, encodedRapt=$encodedRapt")
            if (encodedRapt.isNullOrEmpty()) {
                loadingDialogVisible = false
                val errMsg = when (statusCode) {
                    400 -> ContextProvider.context.getString(R.string.error_passwd)
                    else -> ContextProvider.context.getString(R.string.error_network)
                }
                passwdInputViewState =
                    passwdInputViewState.copy(visible = true, hasError = true, errMsg = errMsg)
                sheetUIViewState = sheetUIViewState.copy(visible = true)
                return@launch
            }
            LogUtils.d("handleBuyButtonClicked encodedRapt: $encodedRapt")
            SettingsManager.setAuthStatus(!passwdInputViewState.checked)
            submitBuyAction(authToken = encodedRapt)
        }
    }

    private fun handlePasswdCheckedChange(checked: Boolean) {
        LogUtils.d("handlePasswdCheckedChange checked: $checked")
        passwdInputViewState = passwdInputViewState.copy(checked = checked)
    }

    private fun handleBuyButtonClicked(action: BAction) {
        val nextShowScreen = sheetUIViewState.screenMap[action.screenId]
            ?: return finishWithResult(sheetUIViewState.result)
        when (val uiType = nextShowScreen.uiInfo?.uiType) {
            UIType.LOADING_SPINNER -> {
                showLoading()
                sheetUIViewState.actionContextList.addAll(action.actionContext)
                viewModelScope.launch(Dispatchers.IO) {
                    submitBuyAction()
                }
            }

            UIType.PURCHASE_AUTH_SCREEN -> {
                LogUtils.d("handleBuyButtonClicked need auth")
                val account =
                    lastBuyFlowResult.account ?: return finishWithResult(sheetUIViewState.result)
                sheetUIViewState.actionContextList.add("ea010408011001b80301".decodeHex())
                sheetUIViewState.actionContextList.add("0a020802b80301".decodeHex())
                passwdInputViewState = passwdInputViewState.copy(
                    visible = true,
                    label = account.name
                )
            }

            else -> {
                LogUtils.d("handleBuyButtonClicked unknown next uiType: $uiType")
                finishWithResult(sheetUIViewState.result)
            }
        }
    }

    private fun showLoading() {
        loadingDialogVisible = true
        passwdInputViewState = passwdInputViewState.copy(visible = false)
        sheetUIViewState = sheetUIViewState.copy(visible = false)
    }

    private fun handleClickAction(action: BAction?) {
        LogUtils.d("handleClickAction action: $action")
        when (action?.type) {
            ActionType.SHOW -> {
                when (action.uiInfo?.uiType) {
                    UIType.PURCHASE_CART_BUY_BUTTON -> handleBuyButtonClicked(action)
                    UIType.PURCHASE_CHANGE_SUBSCRIPTION_CONTINUE_BUTTON,
                    UIType.PURCHASE_PAYMENT_DECLINED_CONTINUE_BUTTON,
                    UIType.PURCHASE_CART_PAYMENT_OPTIONS_LINK,
                    UIType.PURCHASE_CART_CONTINUE_BUTTON,
                    UIType.BILLING_PROFILE_SCREEN_ABANDON -> {
                        if (action.screenId?.isNotBlank() == true) {
                            if (showScreen(action.screenId!!)) {
                                LogUtils.d("showScreen ${action.screenId} success")
                                return
                            }
                            LogUtils.d("showScreen ${action.screenId} false")
                        }
                        finishWithResult(sheetUIViewState.result)
                    }

                    UIType.BILLING_PROFILE_OPTION_CREATE_INSTRUMENT,
                    UIType.BILLING_PROFILE_OPTION_ADD_PLAY_CREDIT,
                    UIType.BILLING_PROFILE_OPTION_REDEEM_CODE -> {
                        viewModelScope.launch(Dispatchers.IO) {
                            showPaymentMethodPage("action")
                        }
                    }

                    else -> finishWithResult(sheetUIViewState.result)
                }
            }

            ActionType.DELAY -> {
                viewModelScope.launch {
                    delay(action.delay!!.toLong())
                    finishWithResult(sheetUIViewState.result)
                }
            }

            else -> {
                when (action?.uiInfo?.uiType) {
                    UIType.PURCHASE_CART_CONTINUE_BUTTON -> viewModelScope.launch(Dispatchers.IO) {
                        submitBuyAction()
                    }

                    UIType.PURCHASE_SUCCESS_SCREEN_WITH_AUTH_CHOICES -> {
                        viewModelScope.launch {
                            delay(3000)
                            finishWithResult(sheetUIViewState.result)
                        }
                    }

                    UIType.BILLING_PROFILE_EXISTING_INSTRUMENT -> {
                        LogUtils.d("switch payment method context: ${action.actionContext[0].toHex()}")
                        showLoading()
                        sheetUIViewState.actionContextList.addAll(action.actionContext)
                        viewModelScope.launch(Dispatchers.IO) {
                            doAcquireRequest()
                        }
                    }

                    UIType.PURCHASE_CART_PAYMENT_OPTIONS_LINK -> {
                        LogUtils.d("open payment option page context: ${action.actionContext[0].toHex()}")
                        showLoading()
                        sheetUIViewState.actionContextList.addAll(action.actionContext)
                        viewModelScope.launch(Dispatchers.IO) {
                            doAcquireRequest()
                        }
                    }

                    else -> finishWithResult(sheetUIViewState.result)
                }
            }
        }
    }

    private fun showScreen(screenId: String): Boolean {
        val showScreen = sheetUIViewState.screenMap[screenId] ?: return false
        sheetUIViewState = sheetUIViewState.copy(
            showScreen = showScreen,
            visible = true
        )
        loadingDialogVisible = false
        return true
    }

    private suspend fun showPaymentMethodPage(src: String): Boolean {
        _event.send(
            NotificationEvent(
                NotificationEventId.OPEN_PAYMENT_METHOD_ACTIVITY,
                bundleOf("account" to lastBuyFlowResult.account, "src" to src)
            )
        )
        return true
    }

    private suspend fun handleBuyFlowResult(buyFlowResult: BuyFlowResult) {
        val failAction = suspend {
            _event.send(NotificationEvent(NotificationEventId.FINISH, buyFlowResult.result))
        }
        val action = buyFlowResult.acquireResult?.action ?: return failAction()
        val screenMap = buyFlowResult.acquireResult.screenMap
        val showScreen = screenMap[action.screenId] ?: return failAction()
        LogUtils.d("handleAcquireResult, showScreen:$showScreen result:${buyFlowResult.acquireResult}")
        if (action.type != ActionType.SHOW) return failAction()
        lastBuyFlowResult = buyFlowResult
        sheetUIViewState.screenMap.putAll(screenMap)
        sheetUIViewState = sheetUIViewState.copy(
            showScreen = showScreen,
            result = buyFlowResult.result,
            actionContextList = action.actionContext,
            visible = true
        )
        loadingDialogVisible = false
    }

    private suspend fun doLoadSheetUIAction(sheetUIAction: SheetUIAction, param: String) {
        when (sheetUIAction) {
            SheetUIAction.LAUNCH_BUY_FLOW -> {
                val buyFlowResult = IAPImpl.acquireRequest(param, firstRequest = true)
                handleBuyFlowResult(buyFlowResult)
            }

            else -> {
                throw IllegalStateException("unknown sheet ui action")
            }
        }
    }

    suspend fun loadSheetUIData() {
        val actionStr = startParams?.getString(KEY_IAP_SHEET_UI_ACTION)
            ?: throw RuntimeException("get action string failed")
        LogUtils.d("loadSheetUIData actionStr:$actionStr")
        val action = SheetUIAction.valueOf(actionStr)
        val param = startParams?.getString(KEY_IAP_SHEET_UI_PARAM)
            ?: throw RuntimeException("get action param failed")
        LogUtils.d("loadSheetUIData param:$param")
        showLoading()
        doLoadSheetUIAction(action, param)
    }

    fun close() {
        val result = if (sheetUIViewState.result.containsKey("INAPP_PURCHASE_DATA"))
            sheetUIViewState.result
        else
            resultBundle(BillingResult.USER_CANCELED.ordinal, "")
        finishWithResult(result)
    }
}