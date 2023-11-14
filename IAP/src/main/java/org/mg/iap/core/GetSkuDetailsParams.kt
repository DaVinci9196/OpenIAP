package org.mg.iap.core

class GetSkuDetailsParams private constructor(
    val apiVersion: Int,
    val skuType: String,
    val skuIdList: List<String>,
    val skuPkgName: String,
    val sdkVersion: String,
    val multiOfferSkuDetail: Map<String, Any>
) {
    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    private constructor(builder: Builder) : this(
        builder.apiVersion!!,
        builder.skuType,
        builder.skuIdList,
        builder.skuPkgName,
        builder.sdkVersion,
        builder.multiOfferSkuDetail
    )

    class Builder {
        var apiVersion: Int? = null
        var skuType: String = ""
        var skuIdList: List<String> = mutableListOf()
        var skuPkgName: String = ""
        var sdkVersion: String = ""
        var multiOfferSkuDetail: Map<String, Any> = mutableMapOf()

        fun build(): GetSkuDetailsParams {
            if (apiVersion == null) {
                throw RuntimeException("apiVersion not set")
            }
            if (skuType.isEmpty()) {
                throw RuntimeException("skuType not set")
            }
            if (skuIdList.isEmpty()) {
                throw RuntimeException("skuIdList is empty")
            }
            return GetSkuDetailsParams(this)
        }
    }
}