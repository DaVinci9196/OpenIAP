package org.mg.iap.core

import org.mg.iap.core.ui.BAction

class SubmitBuyActionParams private constructor(
    val launchBuyFlowResult: LaunchBuyFlowResult,
    val droidGuardResult: String?,
    val action: BAction?,
    val authToken: String?
) {
    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    private constructor(builder: Builder) : this(
        builder.launchBuyFlowResult!!,
        builder.droidGuardResult,
        builder.bAction,
        builder.authToken
    )

    class Builder {
        var launchBuyFlowResult: LaunchBuyFlowResult? = null
        var droidGuardResult: String? = null
        var bAction: BAction? = null
        var authToken: String? = null

        fun build(): SubmitBuyActionParams {
            launchBuyFlowResult ?: throw RuntimeException("launchBuyFlowResult not set")
            return SubmitBuyActionParams(this)
        }
    }
}