package org.mg.iap.core

import org.mg.iap.core.ui.BAcquireResult
import org.mg.iap.core.ui.parseAcquireResponse
import org.mg.iap.proto.AcquireRequest
import org.mg.iap.proto.AcquireResponse

class LaunchBuyFlowResult(
    val launchBuyFlowParams: LaunchBuyFlowParams,
    val acquireRequest: AcquireRequest,
    val acquireResponse: AcquireResponse,
    val acquireResult: BAcquireResult,
    resultMap: Map<String, Any> = mapOf("RESPONSE_CODE" to 0, "DEBUG_MESSAGE" to "")
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(
            launchBuyFlowParams: LaunchBuyFlowParams,
            acquireRequest: AcquireRequest,
            acquireResponse: AcquireResponse
        ): LaunchBuyFlowResult {
            val acquireResult = parseAcquireResponse(acquireResponse)
            if (acquireResponse.acquireResult.hasPurchaseResponse()) {
                val resultMap =
                    responseBundleToMap(acquireResponse.acquireResult.purchaseResponse.responseBundle)
                return LaunchBuyFlowResult(
                    launchBuyFlowParams,
                    acquireRequest,
                    acquireResponse,
                    acquireResult,
                    resultMap
                )
            }

            return LaunchBuyFlowResult(
                launchBuyFlowParams,
                acquireRequest,
                acquireResponse,
                acquireResult
            )
        }
    }
}