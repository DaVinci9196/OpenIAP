package org.mg.iap.ui.logic

import android.accounts.Account
import android.os.Bundle
import org.mg.iap.SettingsManager
import org.mg.iap.core.ui.AcquireParsedResult
import org.mg.iap.core.ui.BAction
import org.mg.iap.core.ui.BScreen

data class SheetUIViewState(
    val visible: Boolean = false,
    val screenMap: MutableMap<String, BScreen> = mutableMapOf(),
    val showScreen: BScreen = BScreen(),
    val result: Bundle = Bundle.EMPTY,
    val actionContextList: MutableList<ByteArray> = mutableListOf(),
    val onClickAction: (BAction?) -> Unit
)

data class PasswdInputViewState(
    var visible: Boolean = false,
    var label: String = "",
    var hasError: Boolean = false,
    var errMsg: String = "",
    var checked: Boolean = !SettingsManager.getAuthStatus(),
    val onButtonClicked: (passwd: String) -> Unit,
    val onCheckedChange: (value: Boolean) -> Unit,
    val onDismissRequest: () -> Unit
)

data class BuyFlowResult(
    val acquireResult: AcquireParsedResult?,
    val account: Account?,
    val result: Bundle
)