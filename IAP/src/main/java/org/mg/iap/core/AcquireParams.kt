package org.mg.iap.core

data class AcquireParams(
    val buyFlowParams: BuyFlowParams,
    val actionContext: List<ByteArray>,
    val droidGuardResult: String?,
    val authToken: String?,
    var lastAcquireResult: AcquireResult?
) {
    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder()
            .apply(block).build()
    }

    private constructor(builder: Builder) : this(
        builder.buyFlowParams!!,
        builder.actionContext ?: emptyList(),
        builder.droidGuardResult,
        builder.authToken,
        builder.lastAcquireResult
    )

    class Builder {
        var buyFlowParams: BuyFlowParams? = null
        var actionContext: List<ByteArray>? = null
        var droidGuardResult: String? = null
        var authToken: String? = null
        var lastAcquireResult: AcquireResult? = null

        fun build(): AcquireParams {
            buyFlowParams ?: throw RuntimeException("BuyFlowParams not set")
            return AcquireParams(this)
        }
    }
}