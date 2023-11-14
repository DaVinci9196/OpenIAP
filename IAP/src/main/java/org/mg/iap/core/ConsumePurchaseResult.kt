package org.mg.iap.core

import org.mg.iap.proto.ConsumePurchaseResponse


class ConsumePurchaseResult(
    resultMap: Map<String, Any> = mapOf(
        "RESPONSE_CODE" to 0,
        "DEBUG_MESSAGE" to ""
    )
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(
            consumePurchaseResponse: ConsumePurchaseResponse
        ): ConsumePurchaseResult {
            if (consumePurchaseResponse.hasFailedResponse()) {
                return ConsumePurchaseResult(
                    mapOf(
                        "RESPONSE_CODE" to consumePurchaseResponse.failedResponse.statusCode,
                        "DEBUG_MESSAGE" to consumePurchaseResponse.failedResponse.msg
                    )
                )
            }
            return ConsumePurchaseResult()
        }
    }
}