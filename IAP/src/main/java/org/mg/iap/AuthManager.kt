package org.mg.iap

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import org.mg.iap.core.AuthData
import java.util.concurrent.TimeUnit

object AuthManager {
    fun getAuthData(account: Account): AuthData? {
        val context = ContextProvider.context
        val deviceCheckInConsistencyToken = CheckinServiceClient.getConsistencyToken(context)
        val gsfId = GServices.getString(context.contentResolver, "android_id", "0")!!.toBigInteger()
            .toString(16)
        LogUtils.d("gsfId: $gsfId, deviceDataVersionInfo: $deviceCheckInConsistencyToken")
        val accountManager: AccountManager = AccountManager.get(context)
        val future = accountManager.getAuthToken(
            account,
            "oauth2:https://www.googleapis.com/auth/googleplay https://www.googleapis.com/auth/accounts.reauth",
            false,
            null,
            null
        )
        val bundle = future.getResult(15, TimeUnit.SECONDS)
        val launch = bundle.getParcelable(AccountManager.KEY_INTENT) as Intent?
        return if (launch != null) {
            // 创建一个 ActivityResultLauncher 对象来处理结果
            LogUtils.e("[getAuthData]need start activity by intent: $launch")
            null
        } else {
            bundle.getString(AccountManager.KEY_AUTHTOKEN)?.let {
                AuthData(account.name, it, gsfId, deviceCheckInConsistencyToken)
            }
        }
    }
}