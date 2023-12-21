package org.mg.iap

import android.accounts.Account
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.android.vending.billing.IInAppBillingService
import com.google.android.gms.droidguard.DroidGuardClient
import org.mg.iap.core.AcquireParams
import org.mg.iap.core.AcquireResult
import org.mg.iap.core.BuyFlowParams
import org.mg.iap.core.ConsumePurchaseParams
import org.mg.iap.core.GetPurchaseHistoryParams
import org.mg.iap.core.GetSkuDetailsParams
import org.mg.iap.core.IAPCore
import org.mg.iap.ui.SheetUIHostActivity
import org.mg.iap.ui.logic.BuyFlowResult
import org.mg.iap.ui.logic.SheetUIAction

private class BuyFlowCacheEntry(
    var packageName: String,
    var account: Account,
    var buyFlowParams: BuyFlowParams? = null,
    var lastAcquireResult: AcquireResult? = null,
    var droidGuardResult: String = ""
)

private const val EXPIRE_MS = 1 * 60 * 1000

private data class IAPCoreCacheEntry(
    val iapCore: IAPCore,
    val expiredAt: Long
)

object IAPImpl : IInAppBillingService.Stub() {
    private val buyFlowCacheMap = mutableMapOf<String, BuyFlowCacheEntry>()
    private val iapCoreCacheMap = mutableMapOf<String, IAPCoreCacheEntry>()
    private const val requestCode = 10001
    private val typeList = listOf(
        "subs",
        "inapp",
        "first_party",
        "audio_book",
        "book",
        "book_subs",
        "nest_subs",
        "play_pass_subs",
        "stadia_item",
        "stadia_subs",
        "movie",
        "tv_show",
        "tv_episode",
        "tv_season"
    )

    private fun getPreferredAccount(extraParams: Bundle?): Account {
        val name = extraParams?.getString("accountName")
        name?.let {
            extraParams.remove("accountName")
        }
        return getGoogleAccount(name)
            ?: throw RuntimeException("No Google account found.")
    }


    private fun createIAPCore(account: Account, pkgName: String): IAPCore {
        val key = "$pkgName:$account"
        val cacheEntry = iapCoreCacheMap[key]
        if (cacheEntry != null) {
            if (cacheEntry.expiredAt > System.currentTimeMillis())
                return cacheEntry.iapCore
            iapCoreCacheMap.remove(key)
        }
        val authData = AuthManager.getAuthData(account)
            ?: throw RuntimeException("Failed to obtain login token.")
        val deviceEnvInfo = createDeviceEnvInfo()
            ?: throw RuntimeException("Failed to retrieve device information.")
        val clientInfo = createClient(pkgName)
            ?: throw RuntimeException("Failed to retrieve client information.")
        val iapCore = IAPCore(deviceEnvInfo, clientInfo, authData)
        iapCoreCacheMap[key] =
            IAPCoreCacheEntry(iapCore, System.currentTimeMillis() + EXPIRE_MS)
        return iapCore
    }

    private fun isBillingSupported(
        apiVersion: Int,
        type: String,
        packageName: String,
        extraParams: Bundle?
    ): Bundle {
        if (apiVersion < 3 || apiVersion > 17) {
            return resultBundle(
                BillingResult.BILLING_UNAVAILABLE.ordinal,
                "Client does not support the requesting billing API."
            )
        }
        if (extraParams != null && apiVersion < 7) {
            return resultBundle(
                BillingResult.DEVELOPER_ERROR.ordinal,
                "ExtraParams was introduced in API version 7."
            )
        }
        if (type.isNullOrBlank()) {
            return resultBundle(
                BillingResult.DEVELOPER_ERROR.ordinal,
                "SKU type can't be empty."
            )
        }
        if (!typeList.contains(type)) {
            return resultBundle(
                BillingResult.DEVELOPER_ERROR.ordinal,
                "Invalid SKU type: $type"
            )
        }
        if (extraParams != null && !extraParams.isEmpty && extraParams.getBoolean("vr") && type == "subs") {
            return resultBundle(
                BillingResult.BILLING_UNAVAILABLE.ordinal,
                "subscription is not supported in VR Mode."
            )
        }
        return resultBundle(BillingResult.OK.ordinal, "")
    }

    override fun isBillingSupported(apiVersion: Int, packageName: String?, type: String?): Int {
        val result = isBillingSupported(apiVersion, type!!, packageName!!, null)
        LogUtils.d("isBillingSupported(apiVersion=$apiVersion, packageName=$packageName, type=$type)=$result")
        return result.getInt("RESPONSE_CODE")
    }

    override fun getSkuDetails(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        skusBundle: Bundle?
    ): Bundle {
        LogUtils.d("getSkuDetails(apiVersion=$apiVersion, packageName=$packageName, type=$type, skusBundle=$skusBundle)")
//        throw UnsupportedOperationException("getSkuDetails not yet implemented")
        return resultBundle(
            BillingResult.BILLING_UNAVAILABLE.ordinal,
            "Not yet implemented"
        )
    }

    override fun getBuyIntent(
        apiVersion: Int,
        packageName: String?,
        sku: String?,
        type: String?,
        developerPayload: String?
    ): Bundle {
        LogUtils.d("getBuyIntent(apiVersion=$apiVersion, packageName=$packageName, sku=$sku, type=$type, developerPayload=$developerPayload)")
//        throw UnsupportedOperationException("getBuyIntent not yet implemented")
        return resultBundle(
            BillingResult.BILLING_UNAVAILABLE.ordinal,
            "Not yet implemented"
        )
    }

    override fun getPurchases(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        continuationToken: String?
    ): Bundle {
        LogUtils.d("getPurchases(apiVersion=$apiVersion, packageName=$packageName, type=$type, continuationToken=$continuationToken)")
//        throw UnsupportedOperationException("getPurchases not yet implemented")
        return resultBundle(
            BillingResult.BILLING_UNAVAILABLE.ordinal,
            "Not yet implemented"
        )
    }

    override fun consumePurchase(
        apiVersion: Int,
        packageName: String?,
        purchaseToken: String?
    ): Int {
        LogUtils.d("consumePurchase(apiVersion=$apiVersion, packageName=$packageName, purchaseToken=$purchaseToken)")
//        throw UnsupportedOperationException("consumePurchase not yet implemented")
        return BillingResult.BILLING_UNAVAILABLE.ordinal
    }

    override fun isPromoEligible(apiVersion: Int, packageName: String?, type: String?): Int {
        LogUtils.d("isPromoEligible(apiVersion=$apiVersion, packageName=$packageName, type=$type)")
//        throw UnsupportedOperationException("isPromoEligible not yet implemented")
        return BillingResult.BILLING_UNAVAILABLE.ordinal
    }

    override fun getBuyIntentToReplaceSkus(
        apiVersion: Int,
        packageName: String?,
        oldSkus: MutableList<String>?,
        newSku: String?,
        type: String?,
        developerPayload: String?
    ): Bundle {
        LogUtils.d("getBuyIntentToReplaceSkus(apiVersion=$apiVersion, packageName=$packageName, oldSkus=$oldSkus, newSku=$newSku, type=$type, developerPayload=$developerPayload)")
//        throw UnsupportedOperationException("getBuyIntentToReplaceSkus not yet implemented")
        return resultBundle(
            BillingResult.BILLING_UNAVAILABLE.ordinal,
            "Not yet implemented"
        )
    }

    override fun getBuyIntentExtraParams(
        apiVersion: Int,
        packageName: String,
        sku: String,
        type: String,
        developerPayload: String?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        LogUtils.d("getBuyIntentExtraParams(apiVersion=$apiVersion, packageName=$packageName, sku=$sku, type=$type, developerPayload=$developerPayload, extraParams=$extraParams)")


        val skuSerializedDocIdList =
            extraParams?.getStringArrayList("SKU_SERIALIZED_DOCID_LIST")
        skuSerializedDocIdList?.forEach {
            LogUtils.d("serializedDocId=$it")
        }
        val skuOfferTypeList = extraParams?.getIntegerArrayList("SKU_OFFER_TYPE_LIST")
        skuOfferTypeList?.forEach {
            LogUtils.d("skuOfferType=$it")
        }
        val skuOfferIdTokenList = extraParams?.getStringArrayList("SKU_OFFER_ID_TOKEN_LIST")
        skuOfferIdTokenList?.forEach {
            LogUtils.d("skuOfferIdToken=$it")
        }
        val accountName = extraParams?.getString("accountName")?.also {
            LogUtils.d("accountName=$it")
        }
        val oldSkuPurchaseToken = extraParams?.getString("oldSkuPurchaseToken")?.also {
            LogUtils.d("oldSkuPurchaseToken=$it")
        }
        val oldSkuPurchaseId = extraParams?.getString("oldSkuPurchaseId")?.also {
            LogUtils.d("oldSkuPurchaseId=$it")
        }

        extraParams?.let {
            it.remove("skusToReplace")
            it.remove("oldSkuPurchaseToken")
            it.remove("vr")
            it.remove("isDynamicSku")
            it.remove("rewardToken")
            it.remove("childDirected")
            it.remove("underAgeOfConsent")
            it.remove("additionalSkus")
            it.remove("additionalSkuTypes")
            it.remove("SKU_OFFER_ID_TOKEN_LIST")
            it.remove("SKU_OFFER_ID_LIST")
            it.remove("SKU_OFFER_TYPE_LIST")
            it.remove("SKU_SERIALIZED_DOCID_LIST")
            it.remove("oldSkuPurchaseId")
        }

        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.BILLING_UNAVAILABLE.ordinal, e.message)
        }
        val params = BuyFlowParams.build {
            this.apiVersion = apiVersion
            this.sku = sku
            this.skuType = type
            this.developerPayload = developerPayload ?: ""
            this.skuParams = bundleToMap(extraParams)
            this.needAuth = SettingsManager.getAuthStatus()
            this.skuSerializedDockIdList = skuSerializedDocIdList
            this.skuOfferIdTokenList = skuOfferIdTokenList
            this.oldSkuPurchaseId = oldSkuPurchaseId
            this.oldSkuPurchaseToken = oldSkuPurchaseToken
        }
        val cacheEntryKey = "${packageName}:${account.name}"
        buyFlowCacheMap[cacheEntryKey] =
            BuyFlowCacheEntry(packageName, account, buyFlowParams = params)
        val intent = Intent(ContextProvider.context, SheetUIHostActivity::class.java)
        intent.putExtra(KEY_IAP_SHEET_UI_ACTION, SheetUIAction.LAUNCH_BUY_FLOW.toString())
        intent.putExtra(KEY_IAP_SHEET_UI_PARAM, cacheEntryKey)
        val buyFlowPendingIntent = PendingIntent.getActivity(
            ContextProvider.context,
            requestCode,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return resultBundle(
            BillingResult.OK.ordinal,
            "",
            bundleOf("BUY_INTENT" to buyFlowPendingIntent)
        )
    }

    fun acquireRequest(
        cacheKey: String,
        actionContexts: List<ByteArray> = emptyList(),
        authToken: String? = null,
        firstRequest: Boolean = false
    ): BuyFlowResult {
        LogUtils.d("acquireRequest(cacheKey=$cacheKey, actionContexts=$actionContexts, authToken=$authToken)")
        val buyFlowCacheEntry = buyFlowCacheMap[cacheKey] ?: return BuyFlowResult(
            null, null, resultBundle(
                BillingResult.DEVELOPER_ERROR.ordinal,
                "Parameter check error."
            )
        )
        val buyFlowParams = buyFlowCacheEntry.buyFlowParams ?: return BuyFlowResult(
            null, buyFlowCacheEntry.account, resultBundle(
                BillingResult.DEVELOPER_ERROR.ordinal,
                "Parameter check error."
            )
        )
        val params = AcquireParams.build {
            this.buyFlowParams = buyFlowParams
            this.actionContext = actionContexts
            this.authToken = authToken
            if (!firstRequest) {
                this.droidGuardResult = buyFlowCacheEntry.droidGuardResult
                this.lastAcquireResult = buyFlowCacheEntry.lastAcquireResult
            }
        }

        val coreResult = try {
            createIAPCore(
                buyFlowCacheEntry.account,
                buyFlowCacheEntry.packageName
            ).doAcquireRequest(
                params
            )
        } catch (e: RuntimeException) {
            LogUtils.d("acquireRequest", e)
            return BuyFlowResult(
                null,
                buyFlowCacheEntry.account,
                resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, e.message)
            )
        } catch (e: Exception) {
            LogUtils.d("acquireRequest", e)
            return BuyFlowResult(
                null,
                buyFlowCacheEntry.account,
                resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, "Internal error.")
            )
        }
        LogUtils.d("acquireRequest acquireParsedResult: ${coreResult.acquireParsedResult}")
        buyFlowCacheEntry.lastAcquireResult = coreResult
        if (coreResult.acquireParsedResult.action?.droidGuardMap?.isNotEmpty() == true) {
            DroidGuardClient.getResults(
                ContextProvider.context,
                "phonesky_acquire_flow",
                coreResult.acquireParsedResult.action!!.droidGuardMap
            ).addOnCompleteListener { task ->
                buyFlowCacheEntry.droidGuardResult = task.result
            }
        }
        coreResult.acquireParsedResult.purchaseItems.forEach {
            PurchaseManager.getPurchaseListMgr(
                buyFlowCacheEntry.account,
                buyFlowCacheEntry.packageName
            ).addItem(it)
        }
        return BuyFlowResult(
            coreResult.acquireParsedResult,
            buyFlowCacheEntry.account,
            coreResult.acquireParsedResult.result.toBundle()
        )
    }

    fun requestAuthProofToken(cacheKey: String, password: String): Pair<Int?, String?> {
        val buyFlowCacheEntry = buyFlowCacheMap[cacheKey] ?: return null to null
        return try {
            createIAPCore(
                buyFlowCacheEntry.account,
                buyFlowCacheEntry.packageName
            ).requestAuthProofToken(password)
        } catch (e: Exception) {
            LogUtils.d("requestAuthProofToken", e)
            null to null
        }
    }

    override fun getPurchaseHistory(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        continuationToken: String?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        LogUtils.d("getPurchaseHistory(apiVersion=$apiVersion, packageName=$packageName, type=$type, continuationToken=$continuationToken, extraParams=$extraParams)")
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.BILLING_UNAVAILABLE.ordinal, e.message)
        }
        val params = GetPurchaseHistoryParams.build {
            this.apiVersion = apiVersion
            this.type = type
            this.continuationToken = continuationToken
            this.extraParams = bundleToMap(extraParams)
        }
        val coreResult = try {
            createIAPCore(account, packageName!!).getPurchaseHistory(params)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, e.message)
        } catch (e: Exception) {
            LogUtils.e("getPurchaseHistory", e)
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, "Internal error.")
        }
        if (coreResult.getCode() == BillingResult.OK.ordinal) {
            val itemList = ArrayList<String>()
            val dataList = ArrayList<String>()
            val signatureList = ArrayList<String>()
            coreResult.purchaseHistoryList?.forEach {
                itemList.add(it.sku)
                dataList.add(it.jsonData)
                signatureList.add(it.signature)
            }
            val result = Bundle()
            result.putStringArrayList("INAPP_PURCHASE_ITEM_LIST", itemList)
            result.putStringArrayList("INAPP_PURCHASE_DATA_LIST", dataList)
            result.putStringArrayList("INAPP_DATA_SIGNATURE_LIST", signatureList)
            if (!coreResult.continuationToken.isNullOrEmpty()) {
                result.putString("INAPP_CONTINUATION_TOKEN", coreResult.continuationToken)
            }
            return resultBundle(BillingResult.OK.ordinal, "", result)
        }
        return coreResult.resultMap.toBundle()
    }

    override fun isBillingSupportedExtraParams(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        extraParams: Bundle?
    ): Int {
        extraParams?.size()
        val result = isBillingSupported(apiVersion, type!!, packageName!!, extraParams)
        LogUtils.d("isBillingSupportedExtraParams(apiVersion=$apiVersion, packageName=$packageName, type=$type, extraParams=$extraParams)=$result")
        return result.getInt("RESPONSE_CODE")
    }

    override fun getPurchasesExtraParams(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        continuationToken: String?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        LogUtils.d("getPurchasesExtraParams(apiVersion=$apiVersion, packageName=$packageName, type=$type, continuationToken=$continuationToken, extraParams=$extraParams)")
        if (apiVersion < 7 && extraParams != null) {
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, "Parameter check error.")
        }
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.BILLING_UNAVAILABLE.ordinal, e.message)
        }
        val itemList = ArrayList<String>()
        val dataList = ArrayList<String>()
        val signatureList = ArrayList<String>()
        PurchaseManager.getPurchaseListMgr(account, packageName!!)
            .getPurchaseByType(type!!)
            .forEach {
                itemList.add(it.sku)
                dataList.add(it.jsonData)
                signatureList.add(it.signature)
            }
        val result = Bundle()
        result.putStringArrayList("INAPP_PURCHASE_ITEM_LIST", itemList)
        result.putStringArrayList("INAPP_PURCHASE_DATA_LIST", dataList)
        result.putStringArrayList("INAPP_DATA_SIGNATURE_LIST", signatureList)
        return resultBundle(BillingResult.OK.ordinal, "", result)
    }

    override fun consumePurchaseExtraParams(
        apiVersion: Int,
        packageName: String?,
        purchaseToken: String,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        LogUtils.d("consumePurchaseExtraParams(apiVersion=$apiVersion, packageName=$packageName, purchaseToken=$purchaseToken, extraParams=$extraParams)")
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.BILLING_UNAVAILABLE.ordinal, e.message)
        }
        val params = ConsumePurchaseParams.build {
            this.apiVersion = apiVersion
            this.purchaseToken = purchaseToken
            this.extraParams = bundleToMap(extraParams)
        }
        val coreResult = try {
            val coreResult = createIAPCore(account, packageName!!).consumePurchase(params)
            if (coreResult.getCode() == BillingResult.OK.ordinal) {
                PurchaseManager.getPurchaseListMgr(account, packageName)
                    .removeItem(purchaseToken)
            }
            coreResult
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, e.message)
        } catch (e: Exception) {
            LogUtils.e("consumePurchaseExtraParams", e)
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, "Internal error.")
        }

        return coreResult.resultMap.toBundle()
    }

    override fun getPriceChangeConfirmationIntent(
        apiVersion: Int,
        packageName: String?,
        sku: String?,
        type: String?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        LogUtils.d("getPriceChangeConfirmationIntent(apiVersion=$apiVersion, packageName=$packageName, sku=$sku, type=$type, extraParams=$extraParams)")
//        throw UnsupportedOperationException("getPriceChangeConfirmationIntent not yet implemented")
        return resultBundle(
            BillingResult.BILLING_UNAVAILABLE.ordinal,
            "Not yet implemented"
        )
    }

    fun getBillingVersionString(bundle: Bundle?): String? {
        if (bundle == null) {
            return null
        }
        return if (bundle.containsKey("playBillingLibraryVersion")) bundle.getString("playBillingLibraryVersion") else bundle.getString(
            "libraryVersion"
        )
    }

    override fun getSkuDetailsExtraParams(
        apiVersion: Int,
        packageName: String?,
        type: String?,
        skuBundle: Bundle?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        skuBundle?.size()
        LogUtils.d("getSkuDetailsExtraParams(apiVersion=$apiVersion, packageName=$packageName, type=$type, skusBundle=$skuBundle, extraParams=$extraParams)")
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.BILLING_UNAVAILABLE.ordinal, e.message)
        }
        val idList = skuBundle?.getStringArrayList("ITEM_ID_LIST")
        val dynamicPriceTokensList = skuBundle?.getStringArrayList("DYNAMIC_PRICE_TOKENS_LIST")
        if (idList.isNullOrEmpty()) {
            LogUtils.e("Input Error: skusBundle must contain an array associated with key ITEM_ID_LIST.")
            return resultBundle(
                BillingResult.DEVELOPER_ERROR.ordinal,
                "SKU bundle must contain sku list"
            )
        }
        idList.sort()
        if (dynamicPriceTokensList != null && dynamicPriceTokensList.isEmpty()) {
            LogUtils.e("Input Error: skusBundle array associated with key ITEM_ID_LIST or key DYNAMIC_PRICE_TOKENS_LIST cannot be empty.")
            return resultBundle(
                BillingResult.DEVELOPER_ERROR.ordinal,
                "SKU bundle must contain sku list"
            )
        }
        if (apiVersion < 9 && extraParams?.isEmpty == false) {
            return resultBundle(
                BillingResult.DEVELOPER_ERROR.ordinal,
                "Must specify an API version >= 9 to use this API."
            )
        }
        val params = GetSkuDetailsParams.build {
            this.apiVersion = apiVersion
            this.skuType = type!!
            this.skuIdList = idList
            extraParams?.getString("SKU_PACKAGE_NAME")?.let {
                this.skuPkgName = it
                extraParams.remove("SKU_PACKAGE_NAME")
            }
            extraParams?.getString("playBillingLibraryVersion")?.let {
                this.sdkVersion = it
            }
            extraParams?.let {
                this.multiOfferSkuDetail = bundleToMap(it)
            }
        }

        val coreResult = try {
            createIAPCore(account, packageName!!).getSkuDetails(params)
        } catch (e: RuntimeException) {
            LogUtils.e("getSkuDetailsExtraParams", e)
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, e.message)
        } catch (e: Exception) {
            LogUtils.e("getSkuDetailsExtraParams", e)
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, "Internal error.")
        }

        coreResult.let { detailsResult ->
            val details = ArrayList(detailsResult.skuDetailsList.map { it.jsonDetails })
            if (detailsResult.getCode() == BillingResult.OK.ordinal) {
                return resultBundle(
                    BillingResult.OK.ordinal,
                    "",
                    bundleOf("DETAILS_LIST" to details)
                )
            } else {
                return resultBundle(detailsResult.getCode(), detailsResult.getMessage())
            }
        }
    }

    override fun acknowledgePurchase(
        apiVersion: Int,
        packageName: String?,
        purchaseToken: String?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        LogUtils.d("acknowledgePurchase(apiVersion=$apiVersion, packageName=$packageName, purchaseToken=$purchaseToken, extraParams=$extraParams)")
        val account = try {
            getPreferredAccount(extraParams)
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.BILLING_UNAVAILABLE.ordinal, e.message)
        }
        val params = org.mg.iap.core.AcknowledgePurchaseParams.build {
            this.apiVersion = apiVersion
            this.purchaseToken = purchaseToken!!
            this.extraParams = bundleToMap(extraParams)
        }
        val coreResult = try {
            val coreResult = createIAPCore(account, packageName!!).acknowledgePurchase(params)
            if (coreResult.getCode() == BillingResult.OK.ordinal && coreResult.purchaseItem != null) {
                PurchaseManager.getPurchaseListMgr(account, packageName)
                    .updateItem(coreResult.purchaseItem!!)
            }
            coreResult
        } catch (e: RuntimeException) {
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, e.message)
        } catch (e: Exception) {
            LogUtils.e("acknowledgePurchase", e)
            return resultBundle(BillingResult.DEVELOPER_ERROR.ordinal, "Internal error.")
        }

        return coreResult.resultMap.toBundle()
    }

    override fun o(
        apiVersion: Int,
        packageName: String?,
        arg3: String?,
        extraParams: Bundle?
    ): Bundle {
        extraParams?.size()
        LogUtils.d("o(apiVersion=$apiVersion, packageName=$packageName, arg3=$arg3, extraParams=$extraParams)")
//        throw UnsupportedOperationException("o not yet implemented")
        return resultBundle(
            BillingResult.BILLING_UNAVAILABLE.ordinal,
            "Not yet implemented"
        )
    }
}