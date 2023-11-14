package org.mg.iap.core

class GetPurchaseHistoryParams private constructor(
    val apiVersion: Int,
    val type: String,
    val continuationToken: String?,
    val extraParams: Map<String, Any>
) {
    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    private constructor(builder: Builder) : this(
        builder.apiVersion!!,
        builder.type!!,
        builder.continuationToken,
        builder.extraParams
    )

    class Builder {
        var apiVersion: Int? = null
        var type: String? = null
        var continuationToken: String? = null
        var extraParams = emptyMap<String, Any>()

        fun build(): GetPurchaseHistoryParams {
            if (apiVersion == null) {
                throw RuntimeException("apiVersion not set")
            }
            if (type.isNullOrEmpty()) {
                throw RuntimeException("type not set")
            }
            return GetPurchaseHistoryParams(this)
        }
    }
}