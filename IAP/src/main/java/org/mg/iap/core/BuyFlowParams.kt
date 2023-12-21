package org.mg.iap.core

data class BuyFlowParams(
    val apiVersion: Int,
    val sku: String,
    val skuType: String,
    val developerPayload: String,
    val sdkVersion: String,
    val needAuth: Boolean,
    val skuParams: Map<String, Any>,
    val skuSerializedDockIdList: List<String>?,
    val skuOfferIdTokenList: List<String>?,
    val oldSkuPurchaseToken: String?,
    val oldSkuPurchaseId: String?
) {
    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    private constructor(builder: Builder) : this(
        builder.apiVersion!!,
        builder.sku,
        builder.skuType,
        builder.developerPayload,
        builder.sdkVersion,
        builder.needAuth,
        builder.skuParams,
        builder.skuSerializedDockIdList,
        builder.skuOfferIdTokenList,
        builder.oldSkuPurchaseToken,
        builder.oldSkuPurchaseId
    )

    class Builder {
        var apiVersion: Int? = null
        var sku = ""
        var skuType = ""
        var developerPayload = ""
        var sdkVersion = ""
        var skuParams = emptyMap<String, Any>()
        var needAuth = false
        var skuSerializedDockIdList: List<String>? = null
        var skuOfferIdTokenList: List<String>? = null
        var oldSkuPurchaseToken: String? = null
        var oldSkuPurchaseId: String? = null

        fun build(): BuyFlowParams {
            if (apiVersion == null) {
                throw RuntimeException("apiVersion not set")
            }
            if (sku.isEmpty()) {
                throw RuntimeException("sku not set")
            }
            if (skuType.isEmpty()) {
                throw RuntimeException("skuType not set")
            }
            return BuyFlowParams(this)
        }
    }
}