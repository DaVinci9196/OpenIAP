package org.mg.iap.core

import org.mg.iap.proto.DocId
import org.mg.iap.proto.SkuDetailsResponse


class GetSkuDetailsResult private constructor(
    val skuDetailsList: List<SkuDetailsItem>,
    resultMap: Map<String, Any> = mapOf("RESPONSE_CODE" to 0, "DEBUG_MESSAGE" to "")
) : IAPResult(resultMap) {
    companion object {
        fun parseFrom(skuDetailsResponse: SkuDetailsResponse): GetSkuDetailsResult {
            if (skuDetailsResponse.hasFailedResponse()) {
                return GetSkuDetailsResult(
                    emptyList(),
                    mapOf(
                        "RESPONSE_CODE" to skuDetailsResponse.failedResponse.statusCode,
                        "DEBUG_MESSAGE" to skuDetailsResponse.failedResponse.msg
                    )
                )
            }
            val skuDetailsList =
                skuDetailsResponse.detailsList.filter { it.skuDetails.isNotBlank() }
                    .map { skuDetails ->
                        SkuDetailsItem(
                            skuDetails.skuDetails,
                            skuDetails.skuInfo.skuItemList.associate { it.token to it.docId }
                        )
                    }
            return GetSkuDetailsResult(skuDetailsList)
        }
    }


    data class SkuDetailsItem(
        val jsonDetails: String,
        val docIdMap: Map<String, DocId>
    )
}