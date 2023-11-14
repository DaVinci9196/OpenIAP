package org.mg.iap.core

class AcknowledgePurchaseParams private constructor(
    val apiVersion: Int,
    val purchaseToken: String,
    val extraParams: Map<String, Any>
) {
    companion object {
        inline fun build(block: org.mg.iap.core.AcknowledgePurchaseParams.Builder.() -> Unit) = org.mg.iap.core.AcknowledgePurchaseParams.Builder()
            .apply(block).build()
    }

    private constructor(builder: org.mg.iap.core.AcknowledgePurchaseParams.Builder) : this(
        builder.apiVersion!!,
        builder.purchaseToken,
        builder.extraParams
    )

    class Builder {
        var apiVersion: Int? = null
        var purchaseToken = ""
        var extraParams = emptyMap<String, Any>()

        fun build(): org.mg.iap.core.AcknowledgePurchaseParams {
            if (apiVersion == null) {
                throw RuntimeException("apiVersion not set")
            }
            if (purchaseToken.isEmpty()) {
                throw RuntimeException("purchaseToken not set")
            }
            return org.mg.iap.core.AcknowledgePurchaseParams(this)
        }
    }
}