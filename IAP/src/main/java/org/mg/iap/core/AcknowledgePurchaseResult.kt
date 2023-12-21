package org.mg.iap.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mg.iap.proto.AcknowledgePurchaseResponse

class AcknowledgePurchaseResult(
    val purchaseItem: PurchaseItem? = null,
    resultMap: Map<String, Any> = mapOf(
        "RESPONSE_CODE" to 0,
        "DEBUG_MESSAGE" to ""
    )
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(
            response: AcknowledgePurchaseResponse
        ): AcknowledgePurchaseResult {
            if (response.hasFailedResponse()) {
                return AcknowledgePurchaseResult(
                    null,
                    mapOf(
                        "RESPONSE_CODE" to response.failedResponse.statusCode,
                        "DEBUG_MESSAGE" to response.failedResponse.msg
                    )
                )
            }
            if (response.purchaseItem.purchaseItemDataCount != 1)
                throw IllegalStateException("AcknowledgePurchaseResult purchase item count != 1")
            return AcknowledgePurchaseResult(parsePurchaseItem(response.purchaseItem).getOrNull(0))
        }
    }
}