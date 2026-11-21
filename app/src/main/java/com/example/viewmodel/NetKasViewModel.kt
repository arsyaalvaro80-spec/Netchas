package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BillingStatus
import com.example.data.BusinessSettings
import com.example.data.Customer
import com.example.data.CustomerBillingItem
import com.example.data.MonthlyFinancialSummary
import com.example.data.NetKasRepository
import com.example.data.Payment
import com.example.data.UiState
import com.example.data.WalletTransfer
import com.example.data.WalletType
import com.example.util.Formatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthlyTrendPoint(
    val yearMonth: String,
    val shortLabel: String,
    val income: Long,
    val cashIncome: Long,
    val bankIncome: Long,
)

data class DailyCashbookEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Long,
    val isIncome: Boolean,
    val isTransfer: Boolean = false,
    val wallet: WalletType,
    val secondaryWallet: WalletType? = null,
    val dateMillis: Long,
    val payment: Payment? = null,
    val transfer: WalletTransfer? = null,
)

data class NetKasDashboardData(
    val selectedMonth: String,
    val isHistoricalMonth: Boolean,
    val settings: BusinessSettings,
    val customers: List<Customer>,
    val inactiveCustomers: List<Customer>,
    val allPayments: List<Payment>,
    val monthPayments: List<Payment>,
    val monthTransfers: List<WalletTransfer>,
    val billingItems: List<CustomerBillingItem>,
    val summary: MonthlyFinancialSummary,
    val trendSixMonths: List<MonthlyTrendPoint>,
    val cashbookEntries: List<DailyCashbookEntry>,
)

class NetKasViewModel(
    private val repository: NetKasRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Formatters.currentYearMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val transactionsFlow = combine(
        repository.observePayments(currentUserId),
        repository.observeWalletTransfers(currentUserId),
    ) { allPayments, allTransfers ->
        allPayments to allTransfers
    }

    val uiState: StateFlow<UiState<NetKasDashboardData>> = combine(
        _selectedMonth,
        repository.observeSettings(currentUserId),
        repository.observeCustomers(currentUserId),
        transactionsFlow,
    ) { month, settings, customers, (allPayments, allTransfers) ->
        buildDashboardData(
            month = month,
            settings = settings,
            customers = customers,
            allPayments = allPayments,
            allTransfers = allTransfers,
        )
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing NetKas data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat data pembukuan."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    private fun buildDashboardData(
        month: String,
        settings: BusinessSettings,
        customers: List<Customer>,
        allPayments: List<Payment>,
        allTransfers: List<WalletTransfer>,
    ): UiState<NetKasDashboardData> {
        val currentMonth = Formatters.currentYearMonth()
        val isHistorical = month < currentMonth

        val monthPayments = allPayments.filter { it.yearMonth == month }
        val monthTransfers = allTransfers.filter { it.yearMonth == month }
        val inactiveCustomers = customers.filter { !it.active }.sortedBy { it.name.lowercase() }

        val paymentByCustomerId = monthPayments.associateBy { it.customerId }
        val reminderWindow = settings.reminderDaysBefore.toInt().coerceIn(0, 15)

        val billingItems = customers
            .filter { it.active || paymentByCustomerId.containsKey(it.id) }
            .map { customer ->
                val payment = paymentByCustomerId[customer.id]
                val daysUntilDue = Formatters.daysUntilDueDate(month, customer.dueDay.toInt())
                val status = when {
                    payment != null -> BillingStatus.PAID
                    daysUntilDue < 0 -> BillingStatus.OVERDUE
                    daysUntilDue <= reminderWindow -> BillingStatus.DUE_SOON
                    else -> BillingStatus.UNPAID
                }
                CustomerBillingItem(
                    customer = customer,
                    payment = payment,
                    status = status,
                    daysUntilDue = daysUntilDue,
                )
            }
            .sortedWith(
                compareBy<CustomerBillingItem> {
                    when (it.status) {
                        BillingStatus.OVERDUE -> 0
                        BillingStatus.DUE_SOON -> 1
                        BillingStatus.UNPAID -> 2
                        BillingStatus.PAID -> 3
                    }
                }.thenBy { it.customer.dueDay }
            )

        val cashIncome = monthPayments.filter { it.wallet == WalletType.CASH }.sumOf { it.amount }
        val bankIncome = monthPayments.filter { it.wallet == WalletType.BANK_TRANSFER }.sumOf { it.amount }
        val totalIncome = cashIncome + bankIncome

        val cashToBankTransfer = monthTransfers
            .filter { it.source == WalletType.CASH && it.destination == WalletType.BANK_TRANSFER }
            .sumOf { it.amount }
        val bankToCashTransfer = monthTransfers
            .filter { it.source == WalletType.BANK_TRANSFER && it.destination == WalletType.CASH }
            .sumOf { it.amount }

        // Cumulative wallet balances across all recorded periods up to selectedMonth
        val paymentsUpTo = allPayments.filter { it.yearMonth <= month }
        val transfersUpTo = allTransfers.filter { it.yearMonth <= month }
        val cumCashIn = paymentsUpTo.filter { it.wallet == WalletType.CASH }.sumOf { it.amount }
        val cumBankIn = paymentsUpTo.filter { it.wallet == WalletType.BANK_TRANSFER }.sumOf { it.amount }
        val cumCashToBank = transfersUpTo
            .filter { it.source == WalletType.CASH && it.destination == WalletType.BANK_TRANSFER }
            .sumOf { it.amount }
        val cumBankToCash = transfersUpTo
            .filter { it.source == WalletType.BANK_TRANSFER && it.destination == WalletType.CASH }
            .sumOf { it.amount }

        val unpaidItems = billingItems.filter { it.payment == null }
        val summary = MonthlyFinancialSummary(
            yearMonth = month,
            totalIncome = totalIncome,
            cashIncome = cashIncome,
            bankIncome = bankIncome,
            cashToBankTransfer = cashToBankTransfer,
            bankToCashTransfer = bankToCashTransfer,
            netCashBalance = cashIncome - cashToBankTransfer + bankToCashTransfer,
            netBankBalance = bankIncome + cashToBankTransfer - bankToCashTransfer,
            cumulativeCashBalance = cumCashIn - cumCashToBank + cumBankToCash,
            cumulativeBankBalance = cumBankIn + cumCashToBank - cumBankToCash,
            unpaidAmount = unpaidItems.sumOf { it.customer.monthlyFee },
            paidCount = billingItems.count { it.payment != null },
            unpaidCount = unpaidItems.size,
            overdueCount = billingItems.count { it.status == BillingStatus.OVERDUE },
            dueSoonCount = billingItems.count { it.status == BillingStatus.DUE_SOON },
        )

        val sixMonths = Formatters.lastSixMonthsEndingAt(month)
        val trendSixMonths = sixMonths.map { ym ->
            val ymPayments = allPayments.filter { it.yearMonth == ym }
            val cashInc = ymPayments.filter { it.wallet == WalletType.CASH }.sumOf { it.amount }
            val bankInc = ymPayments.filter { it.wallet == WalletType.BANK_TRANSFER }.sumOf { it.amount }
            MonthlyTrendPoint(
                yearMonth = ym,
                shortLabel = Formatters.formatYearMonthShort(ym),
                income = cashInc + bankInc,
                cashIncome = cashInc,
                bankIncome = bankInc,
            )
        }

        val cashbookEntries = buildList {
            monthPayments.forEach { p ->
                add(
                    DailyCashbookEntry(
                        id = "pay_${p.id}",
                        title = p.customerName,
                        subtitle = "${p.packageName} • ${p.receiptNumber}",
                        amount = p.amount,
                        isIncome = true,
                        wallet = p.wallet,
                        dateMillis = p.paidDateMillis,
                        payment = p,
                    )
                )
            }
            monthTransfers.forEach { t ->
                val isCashToBank = t.source == WalletType.CASH
                add(
                    DailyCashbookEntry(
                        id = "trf_${t.id}",
                        title = if (isCashToBank) "Setor Tunai ke Bank" else "Tarik Tunai dari Bank",
                        subtitle = t.notes.ifBlank { "${t.source.labelId} → ${t.destination.labelId}" },
                        amount = t.amount,
                        isIncome = false,
                        isTransfer = true,
                        wallet = t.source,
                        secondaryWallet = t.destination,
                        dateMillis = t.transferDateMillis,
                        transfer = t,
                    )
                )
            }
        }.sortedByDescending { it.dateMillis }

        return UiState.Success(
            NetKasDashboardData(
                selectedMonth = month,
                isHistoricalMonth = isHistorical,
                settings = settings,
                customers = customers,
                inactiveCustomers = inactiveCustomers,
                allPayments = allPayments,
                monthPayments = monthPayments,
                monthTransfers = monthTransfers,
                billingItems = billingItems,
                summary = summary,
                trendSixMonths = trendSixMonths,
                cashbookEntries = cashbookEntries,
            )
        )
    }

    fun selectPreviousMonth() {
        _selectedMonth.value = Formatters.shiftYearMonth(_selectedMonth.value, -1)
    }

    fun selectNextMonth() {
        _selectedMonth.value = Formatters.shiftYearMonth(_selectedMonth.value, 1)
    }

    fun selectMonth(yearMonth: String) {
        _selectedMonth.value = yearMonth
    }

    fun jumpToCurrentMonth() {
        _selectedMonth.value = Formatters.currentYearMonth()
    }

    fun saveCustomer(
        existingCustomerId: String?,
        name: String,
        phone: String,
        email: String,
        address: String,
        packageName: String,
        monthlyFee: Long,
        dueDay: Long,
        active: Boolean,
        notes: String = "",
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = repository.saveCustomer(
                userId = currentUserId,
                existingCustomerId = existingCustomerId,
                name = name,
                phone = phone,
                email = email,
                address = address,
                packageName = packageName,
                monthlyFee = monthlyFee,
                dueDay = dueDay,
                active = active,
                notes = notes,
            )
            result.onSuccess {
                _events.tryEmit(
                    if (existingCustomerId == null) "Pelanggan berhasil ditambahkan."
                    else "Data pelanggan diperbarui."
                )
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Gagal menyimpan pelanggan.")
            }
        }
    }

    fun reactivateCustomer(customer: Customer) {
        saveCustomer(
            existingCustomerId = customer.id,
            name = customer.name,
            phone = customer.phone,
            email = customer.email,
            address = customer.address,
            packageName = customer.packageName,
            monthlyFee = customer.monthlyFee,
            dueDay = customer.dueDay,
            active = true,
            notes = "",
        )
    }

    fun deleteCustomer(customerId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCustomer(currentUserId, customerId)
                .onSuccess {
                    _events.tryEmit("Pelanggan dihapus.")
                    onDone()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Gagal menghapus pelanggan.")
                }
        }
    }

    fun recordOrUpdatePayment(
        existingPaymentId: String?,
        customer: Customer,
        yearMonth: String,
        amount: Long,
        paymentMethod: WalletType,
        bankReference: String,
        receiptNumber: String,
        paidDateMillis: Long,
        notes: String,
        onSaved: (Payment) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = repository.recordOrUpdatePayment(
                userId = currentUserId,
                existingPaymentId = existingPaymentId,
                customerId = customer.id,
                customerName = customer.name,
                packageName = customer.packageName,
                yearMonth = yearMonth,
                amount = amount,
                paymentMethod = paymentMethod,
                bankReference = bankReference,
                receiptNumber = receiptNumber,
                paidDateMillis = paidDateMillis,
                notes = notes,
            )
            result.onSuccess { savedPayment ->
                _events.tryEmit(
                    if (existingPaymentId == null) "Pembayaran dicatat ke ${paymentMethod.labelId}."
                    else "Data pembayaran bulan ${Formatters.formatYearMonthShort(yearMonth)} diperbarui."
                )
                onSaved(savedPayment)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Gagal menyimpan pembayaran.")
            }
        }
    }

    fun deletePayment(paymentId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePayment(currentUserId, paymentId)
                .onSuccess {
                    _events.tryEmit("Pembayaran dibatalkan / dihapus.")
                    onDone()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Gagal menghapus pembayaran.")
                }
        }
    }

    fun saveWalletTransfer(
        existingTransferId: String?,
        yearMonth: String,
        fromWallet: WalletType,
        toWallet: WalletType,
        amount: Long,
        transferDateMillis: Long,
        notes: String,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = repository.saveWalletTransfer(
                userId = currentUserId,
                existingTransferId = existingTransferId,
                yearMonth = yearMonth,
                fromWallet = fromWallet,
                toWallet = toWallet,
                amount = amount,
                transferDateMillis = transferDateMillis,
                notes = notes,
            )
            result.onSuccess {
                val label = if (fromWallet == WalletType.CASH) "Setor tunai ke bank" else "Tarik tunai dari bank"
                _events.tryEmit(
                    if (existingTransferId == null) "$label berhasil dicatat."
                    else "Catatan pindah saldo diperbarui."
                )
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Gagal menyimpan pindah saldo.")
            }
        }
    }

    fun deleteWalletTransfer(transferId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteWalletTransfer(currentUserId, transferId)
                .onSuccess {
                    _events.tryEmit("Catatan pindah saldo dihapus.")
                    onDone()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Gagal menghapus pindah saldo.")
                }
        }
    }

    fun saveBusinessSettings(
        businessName: String,
        businessPhone: String,
        businessAddress: String,
        bankAccountInfo: String,
        receiptFooterText: String,
        reminderDaysBefore: Long,
        remindersEnabled: Boolean,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = repository.saveBusinessSettings(
                userId = currentUserId,
                businessName = businessName,
                businessPhone = businessPhone,
                businessAddress = businessAddress,
                bankAccountInfo = bankAccountInfo,
                receiptFooterText = receiptFooterText,
                reminderDaysBefore = reminderDaysBefore,
                remindersEnabled = remindersEnabled,
            )
            result.onSuccess {
                _events.tryEmit("Pengaturan usaha & kuitansi tersimpan.")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Gagal menyimpan pengaturan.")
            }
        }
    }

    private companion object {
        const val TAG = "NetKasVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
