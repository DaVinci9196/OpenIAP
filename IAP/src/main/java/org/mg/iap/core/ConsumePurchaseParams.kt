package org.mg.iap.core

class ConsumePurchaseParams private constructor(
    val apiVersion: Int,
    val purchaseToken: String,
    val extraParams: Map<String, Any>
) {
    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    private constructor(builder: Builder) : this(
        builder.apiVersion!!,
        builder.purchaseToken,
        builder.extraParams
    )

    class Builder {
        var apiVersion: Int? = null
        var purchaseToken = ""
        var extraParams = emptyMap<String, Any>()

        fun build(): ConsumePurchaseParams {
            if (apiVersion == null) {
                throw RuntimeException("apiVersion not set")
            }
            if (purchaseToken.isEmpty()) {
                throw RuntimeException("purchaseToken not set")
            }
            return ConsumePurchaseParams(this)
        }
    }
}