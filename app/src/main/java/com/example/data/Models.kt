package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class WalletType(val code: String, val labelId: String) {
    CASH("CASH", "Dompet Tunai"),
    BANK_TRANSFER("BANK_TRANSFER", "Transfer Bank");

    companion object {
        fun fromCode(raw: String?): WalletType =
            entries.firstOrNull { it.code == raw } ?: CASH
    }
}

@Keep
@IgnoreExtraProperties
data class BusinessSettings(
    @DocumentId val id: String = "profile",
    val userId: String = "",
    val businessName: String = "NetKas Wi-Fi",
    val businessPhone: String = "",
    val businessAddress: String = "",
    val bankAccountInfo: String = "",
    val receiptFooterText: String = DEFAULT_RECEIPT_FOOTER,
    val reminderDaysBefore: Long = 3L,
    val remindersEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    companion object {
        const val DEFAULT_RECEIPT_FOOTER =
            "Terima kasih atas pembayaran tepat waktu Anda.\nSimpan kuitansi ini sebagai bukti pembayaran sah."
    }
}

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val packageName: String = "20 Mbps Home",
    val monthlyFee: Long = 150_000L,
    val dueDay: Long = 10L,
    val active: Boolean = true,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val packageName: String = "",
    val yearMonth: String = "",
    val amount: Long = 0L,
    val paymentMethod: String = WalletType.CASH.code,
    val bankReference: String = "",
    val receiptNumber: String = "",
    val paidDateMillis: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val wallet: WalletType
        get() = WalletType.fromCode(paymentMethod)
}

@Keep
@IgnoreExtraProperties
data class WalletTransfer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val yearMonth: String = "",
    val fromWallet: String = WalletType.CASH.code,
    val toWallet: String = WalletType.BANK_TRANSFER.code,
    val amount: Long = 0L,
    val transferDateMillis: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val source: WalletType
        get() = WalletType.fromCode(fromWallet)

    val destination: WalletType
        get() = WalletType.fromCode(toWallet)
}

enum class BillingStatus {
    PAID,
    DUE_SOON,
    OVERDUE,
    UNPAID,
}

data class CustomerBillingItem(
    val customer: Customer,
    val payment: Payment?,
    val status: BillingStatus,
    val daysUntilDue: Int,
)

data class MonthlyFinancialSummary(
    val yearMonth: String,
    val totalIncome: Long,
    val cashIncome: Long,
    val bankIncome: Long,
    val cashToBankTransfer: Long,
    val bankToCashTransfer: Long,
    val netCashBalance: Long,
    val netBankBalance: Long,
    val cumulativeCashBalance: Long,
    val cumulativeBankBalance: Long,
    val unpaidAmount: Long,
    val paidCount: Int,
    val unpaidCount: Int,
    val overdueCount: Int,
    val dueSoonCount: Int,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
