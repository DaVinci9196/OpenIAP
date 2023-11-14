package org.mg.iap.ui.logic

import android.accounts.Account
import android.os.Bundle
import org.mg.iap.SettingsManager
import org.mg.iap.core.ui.BAcquireResult
import org.mg.iap.core.ui.BAction
import org.mg.iap.core.ui.BScreen

data class SheetUIViewState(
    val visible: Boolean = false,
    val screenMap: Map<String, BScreen> = emptyMap(),
    val showScreen: BScreen = BScreen(),
    val result: Bundle = Bundle.EMPTY,
    val onClickAction: (BAction?) -> Unit
)

data class PasswdInputViewState(
    var visible: Boolean = false,
    var label: String = "",
    var hasError: Boolean = false,
    var errMsg: String = "",
    var bAction: BAction? = null,
    var checked: Boolean = !SettingsManager.getAuthStatus(),
    val onButtonClicked: (passwd: String) -> Unit,
    val onCheckedChange: (value: Boolean) -> Unit,
    val onDismissRequest: () -> Unit
)

data class BuyFlowResult(
    val acquireResult: BAcquireResult?,
    val account: Account?,
    val result: Bundle
)