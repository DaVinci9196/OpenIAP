package org.mg.iap.core

import org.mg.iap.proto.PurchaseHistoryResponse

class GetPurchaseHistoryResult(
    val purchaseHistoryList: List<PurchaseHistoryItem>?,
    val continuationToken: String?,
    resultMap: Map<String, Any> = mapOf(
        "RESPONSE_CODE" to 0,
        "DEBUG_MESSAGE" to ""
    )
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(
            response: PurchaseHistoryResponse
        ): GetPurchaseHistoryResult {
            if (response.hasFailedResponse()) {
                return GetPurchaseHistoryResult(
                    null,
                    null,
                    mapOf(
                        "RESPONSE_CODE" to response.failedResponse.statusCode,
                        "DEBUG_MESSAGE" to response.failedResponse.msg
                    )
                )
            }
            if (response.productIdCount != response.purchaseJsonCount || response.purchaseJsonCount != response.signatureCount) {
                throw IllegalStateException("GetPurchaseHistoryResult item count error")
            }
            val purchaseHistoryList = mutableListOf<PurchaseHistoryItem>()
            var continuationToken: String? = null
            for (cnt in 0 until response.productIdCount) {
                purchaseHistoryList.add(
                    PurchaseHistoryItem(
                        response.getProductId(cnt),
                        response.getPurchaseJson(cnt),
                        response.getSignature(cnt)
                    )
                )
            }
            if (!response.continuationToken.isNullOrEmpty()) {
                continuationToken = response.continuationToken
            }

            return GetPurchaseHistoryResult(purchaseHistoryList, continuationToken)
        }
    }

    class PurchaseHistoryItem(val sku: String, val jsonData: String, val signature: String)
}