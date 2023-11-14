package org.mg.iap.core

data class PurchaseItem(
    val type: String,
    val sku: String,
    val orderId: String,
    val purchaseToken: String,
    val jsonData: String,
    val signature: String
) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is PurchaseItem -> {
                this.orderId == other.orderId && this.purchaseToken == other.purchaseToken
            }

            else -> false
        }
    }
}
