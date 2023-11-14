package org.mg.iap.core

import org.mg.iap.proto.AcquireResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mg.iap.core.ui.BAcquireResult
import org.mg.iap.core.ui.parseAcquireResponse

class SubmitBuyActionResult(
    val acquireResponse: AcquireResponse,
    val acquireResult: BAcquireResult,
    val purchaseItem: PurchaseItem?,
    resultMap: Map<String, Any> = mapOf("RESPONSE_CODE" to 0, "DEBUG_MESSAGE" to "")
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(
            params: SubmitBuyActionParams,
            acquireResponse: AcquireResponse
        ): SubmitBuyActionResult {
            val acquireResult = parseAcquireResponse(acquireResponse)
            var purchaseItem: PurchaseItem? = null
            if (acquireResponse.acquireResult.hasPurchaseResponse()) {
                val resultMap =
                    responseBundleToMap(acquireResponse.acquireResult.purchaseResponse.responseBundle)
                val pd = resultMap["INAPP_PURCHASE_DATA"] as String?
                val ps = resultMap["INAPP_DATA_SIGNATURE"] as String?
                val code = resultMap["RESPONSE_CODE"] as Int?
                if (pd != null && ps != null && code == 0) {
                    val pdj = Json.parseToJsonElement(pd).jsonObject
                    purchaseItem = PurchaseItem(
                        params.launchBuyFlowResult.launchBuyFlowParams.skuType,
                        params.launchBuyFlowResult.launchBuyFlowParams.sku,
                        pdj["orderId"]!!.jsonPrimitive.content,
                        pdj["purchaseToken"]!!.jsonPrimitive.content,
                        pd, ps
                    )
                }
                return SubmitBuyActionResult(
                    acquireResponse,
                    acquireResult,
                    purchaseItem,
                    resultMap
                )
            }

            return SubmitBuyActionResult(acquireResponse, acquireResult, null)
        }
    }
}