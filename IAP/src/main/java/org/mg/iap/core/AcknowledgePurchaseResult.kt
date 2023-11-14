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
            var purchaseItem: PurchaseItem?
            val purchaseItemData = response.purchaseItem.getPurchaseItemData(0)
            val docId = purchaseItemData.docId
            val (type, _, sku) = splitDocId(docId)
            val (jsonData, signature) = when (type) {
                "inapp" -> purchaseItemData.inAppPurchase.jsonData to purchaseItemData.inAppPurchase.signature
                "subs" -> purchaseItemData.subsPurchase.jsonData to purchaseItemData.subsPurchase.signature
                else -> throw IllegalStateException("unknown sku type $type")
            }
            val jdo = Json.parseToJsonElement(jsonData).jsonObject
            purchaseItem = PurchaseItem(
                type,
                sku,
                jdo["orderId"]!!.jsonPrimitive.content,
                jdo["purchaseToken"]!!.jsonPrimitive.content,
                jsonData,
                signature
            )

            return AcknowledgePurchaseResult(purchaseItem)
        }
    }
}