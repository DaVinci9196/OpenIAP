package org.mg.iap.core

import org.mg.iap.core.ui.AcquireParsedResult
import org.mg.iap.core.ui.parseAcquireResponse
import org.mg.iap.proto.AcquireRequest
import org.mg.iap.proto.AcquireResponse

data class AcquireResult(
    val acquireParsedResult: AcquireParsedResult,
    val acquireRequest: AcquireRequest,
    val acquireResponse: AcquireResponse,
) {
    companion object {
        fun parseFrom(
            acquireParams: AcquireParams,
            acquireRequest: AcquireRequest,
            acquireResponse: AcquireResponse
        ): AcquireResult {
            return AcquireResult(
                parseAcquireResponse(acquireParams, acquireResponse),
                acquireRequest,
                acquireResponse
            )
        }
    }
}