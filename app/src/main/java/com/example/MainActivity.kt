package com.example

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.signOut
import com.example.data.BillingStatus
import com.example.data.BusinessSettings
import com.example.data.Customer
import com.example.data.NetKasRepository
import com.example.data.Payment
import com.example.data.UiState
import com.example.data.WalletTransfer
import com.example.ui.components.BusinessSettingsDialog
import com.example.ui.components.CustomerFormDialog
import com.example.ui.components.CustomerHistoryDialog
import com.example.ui.components.MonthPickerDialog
import com.example.ui.components.PaymentFormDialog
import com.example.ui.components.WalletTransferDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReceiptFullScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.NetKasTheme
import com.example.ui.theme.resolveDarkTheme
import com.example.util.AppThemeMode
import com.example.util.DueReminderWorker
import com.example.util.ReceiptService
import com.example.util.ReminderNotificationHelper
import com.example.util.ReportExportService
import com.example.util.ThemePreferences
import com.example.viewmodel.NetKasDashboardData
import com.example.viewmodel.NetKasViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderNotificationHelper.ensureChannel(this)
        setContent {
            val themeMode by ThemePreferences
                .themeModeFlow(this@MainActivity)
                .collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val isDark = resolveDarkTheme(themeMode)
            var isReceiptOpen by remember { mutableStateOf(false) }
            val activityScope = rememberCoroutineScope()

            DisposableEffect(isDark, isReceiptOpen) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark || isReceiptOpen) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                )
                onDispose {}
            }

            NetKasTheme(themeMode = themeMode, darkTheme = isDark) {
                AppContent(
                    themeMode = themeMode,
                    onSelectThemeMode = { selected ->
                        activityScope.launch {
                            ThemePreferences.setThemeMode(this@MainActivity, selected)
                        }
                    },
                    onReceiptOpenChanged = { open -> isReceiptOpen = open },
                )
            }
        }
    }
}

private enum class NetKasTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    DASHBOARD(R.string.nav_dashboard, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    BILLING(R.string.nav_billing, Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_billing"),
    REPORTS(R.string.nav_reports, Icons.Rounded.Assessment, Icons.Outlined.Assessment, "nav_reports"),
}

@Composable
private fun AppContent(
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onReceiptOpenChanged: (Boolean) -> Unit,
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        LaunchedEffect(Unit) { onReceiptOpenChanged(false) }
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            NetKasAuthenticatedShell(
                currentUserId = user.uid,
                userEmail = user.email,
                themeMode = themeMode,
                onSelectThemeMode = onSelectThemeMode,
                onReceiptOpenChanged = onReceiptOpenChanged,
            )
        }
    }
}

@Composable
private fun NetKasAuthenticatedShell(
    currentUserId: String,
    userEmail: String?,
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onReceiptOpenChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: NetKasViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NetKasViewModel(NetKasRepository(db), currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(NetKasTab.DASHBOARD) }

    // Modal & full-screen receipt states
    var showMonthPicker by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var customerFormTarget by remember { mutableStateOf<Pair<Boolean, Customer?>?>(null) }
    var customerHistoryTargetId by remember { mutableStateOf<String?>(null) }
    var paymentFormTarget by remember { mutableStateOf<Pair<Customer, Payment?>?>(null) }
    var transferFormTarget by remember { mutableStateOf<Pair<Boolean, WalletTransfer?>?>(null) }
    var receiptPreviewTarget by remember { mutableStateOf<Payment?>(null) }
    var hasAutoNotifiedSession by remember { mutableStateOf(false) }

    // Pending action if user grants POST_NOTIFICATIONS runtime permission
    var pendingNotificationData by remember { mutableStateOf<NetKasDashboardData?>(null) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingNotificationData
        pendingNotificationData = null
        if (granted && pending != null) {
            val sent = ReminderNotificationHelper.showDueDateSummaryNotification(
                context = context,
                yearMonth = pending.selectedMonth,
                billingItems = pending.billingItems,
            )
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (sent) "Notifikasi pengingat jatuh tempo dikirim."
                    else "Notifikasi diblokir oleh pengaturan sistem perangkat."
                )
            }
        } else if (!granted) {
            scope.launch { snackbarHostState.showSnackbar("Izin notifikasi ditolak. Aktifkan di Pengaturan perangkat.") }
        }
    }

    // Zero-permission SAF launchers to save PDF & Excel directly to device storage
    var pendingPdfExportData by remember { mutableStateOf<NetKasDashboardData?>(null) }
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val exportData = pendingPdfExportData
        pendingPdfExportData = null
        if (uri != null && exportData != null) {
            ReportExportService.saveMonthlyPdfToUri(
                context = context,
                uri = uri,
                yearMonth = exportData.selectedMonth,
                settings = exportData.settings,
                summary = exportData.summary,
                payments = exportData.monthPayments,
                transfers = exportData.monthTransfers,
                billingItems = exportData.billingItems,
            ).onSuccess {
                scope.launch { snackbarHostState.showSnackbar("Laporan PDF berhasil disimpan ke HP.") }
            }.onFailure { err ->
                scope.launch { snackbarHostState.showSnackbar("Gagal menyimpan PDF: ${err.localizedMessage}") }
            }
        }
    }

    var pendingExcelExportData by remember { mutableStateOf<NetKasDashboardData?>(null) }
    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val exportData = pendingExcelExportData
        pendingExcelExportData = null
        if (uri != null && exportData != null) {
            ReportExportService.saveMonthlyExcelToUri(
                context = context,
                uri = uri,
                yearMonth = exportData.selectedMonth,
                settings = exportData.settings,
                summary = exportData.summary,
                payments = exportData.monthPayments,
                transfers = exportData.monthTransfers,
                billingItems = exportData.billingItems,
            ).onSuccess {
                scope.launch { snackbarHostState.showSnackbar("Laporan Excel (.xlsx) berhasil disimpan ke HP.") }
            }.onFailure { err ->
                scope.launch { snackbarHostState.showSnackbar("Gagal menyimpan Excel: ${err.localizedMessage}") }
            }
        }
    }

    var pendingReceiptSaveTarget by remember { mutableStateOf<Triple<Payment, Customer?, BusinessSettings>?>(null) }
    val saveReceiptPngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val target = pendingReceiptSaveTarget
        pendingReceiptSaveTarget = null
        if (uri != null && target != null) {
            ReceiptService.saveReceiptPngToUri(
                context = context,
                uri = uri,
                payment = target.first,
                customer = target.second,
                settings = target.third,
            ).onSuccess {
                scope.launch { snackbarHostState.showSnackbar("Foto kuitansi berhasil disimpan ke HP.") }
            }.onFailure { err ->
                scope.launch { snackbarHostState.showSnackbar("Gagal menyimpan foto kuitansi: ${err.localizedMessage}") }
            }
        }
    }

    fun triggerDueNotification(data: NetKasDashboardData) {
        if (ReminderNotificationHelper.hasNotificationPermission(context)) {
            val sent = ReminderNotificationHelper.showDueDateSummaryNotification(
                context = context,
                yearMonth = data.selectedMonth,
                billingItems = data.billingItems,
            )
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (sent) "Notifikasi pengingat jatuh tempo dikirim."
                    else "Notifikasi dinonaktifkan di Pengaturan perangkat."
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingNotificationData = data
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Automatic once-per-session due-date notification + daily background WorkManager schedule
    val remindersEnabledState = (uiState as? UiState.Success)?.data?.settings?.remindersEnabled
    LaunchedEffect(remindersEnabledState) {
        when (remindersEnabledState) {
            true -> DueReminderWorker.scheduleDailyCheck(context)
            false -> DueReminderWorker.cancelDailyCheck(context)
            null -> Unit
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (!hasAutoNotifiedSession && state is UiState.Success) {
            val data = state.data
            val hasUrgent = data.billingItems.any {
                it.status == BillingStatus.OVERDUE || it.status == BillingStatus.DUE_SOON
            }
            if (data.settings.remindersEnabled && hasUrgent && ReminderNotificationHelper.hasNotificationPermission(context)) {
                ReminderNotificationHelper.showDueDateSummaryNotification(
                    context = context,
                    yearMonth = data.selectedMonth,
                    billingItems = data.billingItems,
                )
            }
            hasAutoNotifiedSession = true
        }
    }

    // Collect ViewModel toast/snackbar events
    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Full-screen Android Receipt View takes over the entire window cleanly
    val activeReceipt = receiptPreviewTarget
    val successData = (uiState as? UiState.Success)?.data
    val showingReceipt = activeReceipt != null && successData != null
    LaunchedEffect(showingReceipt) {
        onReceiptOpenChanged(showingReceipt)
    }

    if (showingReceipt && activeReceipt != null && successData != null) {
        val customer = successData.customers.firstOrNull { it.id == activeReceipt.customerId }
        val livePayment = successData.allPayments.firstOrNull { it.id == activeReceipt.id } ?: activeReceipt
        ReceiptFullScreen(
            payment = livePayment,
            customer = customer,
            settings = successData.settings,
            snackbarHostState = snackbarHostState,
            onClose = { receiptPreviewTarget = null },
            onPrint = {
                ReceiptService.printReceipt(
                    context = context,
                    payment = livePayment,
                    customer = customer,
                    settings = successData.settings,
                ).onFailure { err ->
                    scope.launch {
                        snackbarHostState.showSnackbar(err.localizedMessage ?: "Gagal membuka layanan cetak.")
                    }
                }
            },
            onShareWhatsApp = {
                ReceiptService.shareReceiptViaWhatsApp(
                    context = context,
                    payment = livePayment,
                    customer = customer,
                    settings = successData.settings,
                ).onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar("Tidak ada aplikasi yang mendukung pengiriman foto kuitansi.")
                    }
                }
            },
            onShareEmail = {
                ReceiptService.shareReceiptViaEmail(
                    context = context,
                    payment = livePayment,
                    customer = customer,
                    settings = successData.settings,
                ).onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar("Tidak ada aplikasi Email yang terpasang di perangkat ini.")
                    }
                }
            },
            onSaveImageToDevice = {
                val safeNo = livePayment.receiptNumber
                    .ifBlank { livePayment.id.ifBlank { "netkas" } }
                    .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                pendingReceiptSaveTarget = Triple(livePayment, customer, successData.settings)
                saveReceiptPngLauncher.launch("Kuitansi_${safeNo}.png")
            },
            onSaveReceiptProfile = { bizName, bizPhone, bankInfo, footerText ->
                viewModel.saveBusinessSettings(
                    businessName = bizName,
                    businessPhone = bizPhone,
                    businessAddress = successData.settings.businessAddress,
                    bankAccountInfo = bankInfo,
                    receiptFooterText = footerText,
                    reminderDaysBefore = successData.settings.reminderDaysBefore,
                    remindersEnabled = successData.settings.remindersEnabled,
                )
            },
        )
        return
    }

    // BackHandler returns secondary tabs to Dashboard
    BackHandler(enabled = activeTab != NetKasTab.DASHBOARD) {
        activeTab = NetKasTab.DASHBOARD
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        NetKasTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        NetKasTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                    ) {
                        when (val state = uiState) {
                            UiState.Loading -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Memuat data pembukuan NetKas...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            is UiState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "Terjadi kendala saat sinkronisasi",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            is UiState.Success -> {
                                val data = state.data

                                when (activeTab) {
                                    NetKasTab.DASHBOARD -> DashboardScreen(
                                        data = data,
                                        themeMode = themeMode,
                                        onCycleThemeMode = {
                                            val next = themeMode.nextMode()
                                            onSelectThemeMode(next)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Mode tampilan: ${next.labelId}")
                                            }
                                        },
                                        onPreviousMonth = viewModel::selectPreviousMonth,
                                        onNextMonth = viewModel::selectNextMonth,
                                        onOpenMonthPicker = { showMonthPicker = true },
                                        onJumpCurrentMonth = viewModel::jumpToCurrentMonth,
                                        onOpenSettings = { showSettingsDialog = true },
                                        onAddCustomer = { customerFormTarget = true to null },
                                        onAddTransfer = { transferFormTarget = true to null },
                                        onGoToBilling = { activeTab = NetKasTab.BILLING },
                                        onGoToReports = { activeTab = NetKasTab.REPORTS },
                                        onSendDueNotification = { triggerDueNotification(data) },
                                        onSendWhatsAppReminder = { item ->
                                            ReceiptService.sendWhatsAppDueReminder(
                                                context = context,
                                                customer = item.customer,
                                                yearMonth = data.selectedMonth,
                                                settings = data.settings,
                                            ).onFailure {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Tidak dapat membuka WhatsApp atau aplikasi pesan.")
                                                }
                                            }
                                        },
                                        onOpenPaymentEdit = { payment ->
                                            val customer = data.customers.firstOrNull { it.id == payment.customerId }
                                                ?: Customer(
                                                    id = payment.customerId,
                                                    name = payment.customerName,
                                                    packageName = payment.packageName,
                                                    monthlyFee = payment.amount,
                                                )
                                            paymentFormTarget = customer to payment
                                        },
                                        onOpenReceipt = { payment -> receiptPreviewTarget = payment },
                                        onOpenTransferEdit = { transfer -> transferFormTarget = true to transfer },
                                    )

                                    NetKasTab.BILLING -> BillingScreen(
                                        data = data,
                                        onPreviousMonth = viewModel::selectPreviousMonth,
                                        onNextMonth = viewModel::selectNextMonth,
                                        onOpenMonthPicker = { showMonthPicker = true },
                                        onJumpCurrentMonth = viewModel::jumpToCurrentMonth,
                                        onAddCustomer = { customerFormTarget = true to null },
                                        onEditCustomer = { c -> customerFormTarget = true to c },
                                        onOpenCustomerHistory = { c -> customerHistoryTargetId = c.id },
                                        onReactivateCustomer = { c -> viewModel.reactivateCustomer(c) },
                                        onRecordPayment = { c -> paymentFormTarget = c to null },
                                        onEditPayment = { c, p -> paymentFormTarget = c to p },
                                        onOpenReceipt = { p -> receiptPreviewTarget = p },
                                        onSendWhatsAppReminder = { item ->
                                            ReceiptService.sendWhatsAppDueReminder(
                                                context = context,
                                                customer = item.customer,
                                                yearMonth = data.selectedMonth,
                                                settings = data.settings,
                                            ).onFailure {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Tidak dapat membuka WhatsApp atau aplikasi pesan.")
                                                }
                                            }
                                        },
                                    )

                                    NetKasTab.REPORTS -> ReportsScreen(
                                        data = data,
                                        userEmail = userEmail,
                                        themeMode = themeMode,
                                        onSelectThemeMode = { selected ->
                                            onSelectThemeMode(selected)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Mode tampilan: ${selected.labelId}")
                                            }
                                        },
                                        onPreviousMonth = viewModel::selectPreviousMonth,
                                        onNextMonth = viewModel::selectNextMonth,
                                        onOpenMonthPicker = { showMonthPicker = true },
                                        onJumpCurrentMonth = viewModel::jumpToCurrentMonth,
                                        onExportPdf = {
                                            ReportExportService.exportMonthlyPdfAndShare(
                                                context = context,
                                                yearMonth = data.selectedMonth,
                                                settings = data.settings,
                                                summary = data.summary,
                                                payments = data.monthPayments,
                                                transfers = data.monthTransfers,
                                                billingItems = data.billingItems,
                                            ).onFailure { err ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Gagal ekspor PDF: ${err.localizedMessage}")
                                                }
                                            }
                                        },
                                        onSavePdfToDevice = {
                                            pendingPdfExportData = data
                                            savePdfLauncher.launch("Laporan_NetKas_${data.selectedMonth}.pdf")
                                        },
                                        onExportExcel = {
                                            ReportExportService.exportMonthlyExcelAndShare(
                                                context = context,
                                                yearMonth = data.selectedMonth,
                                                settings = data.settings,
                                                summary = data.summary,
                                                payments = data.monthPayments,
                                                transfers = data.monthTransfers,
                                                billingItems = data.billingItems,
                                            ).onFailure { err ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Gagal ekspor Excel: ${err.localizedMessage}")
                                                }
                                            }
                                        },
                                        onSaveExcelToDevice = {
                                            pendingExcelExportData = data
                                            saveExcelLauncher.launch("Laporan_NetKas_${data.selectedMonth}.xlsx")
                                        },
                                        onOpenSettings = { showSettingsDialog = true },
                                        onSendDueNotification = { triggerDueNotification(data) },
                                        onSignOut = {
                                            DueReminderWorker.cancelDailyCheck(context)
                                            signOut(credentialManager = credentialManager, scope = scope)
                                        },
                                    )
                                }

                                // --- MODAL DIALOGS ---

                                if (showMonthPicker) {
                                    MonthPickerDialog(
                                        selectedMonth = data.selectedMonth,
                                        onSelect = { ym -> viewModel.selectMonth(ym) },
                                        onDismiss = { showMonthPicker = false },
                                    )
                                }

                                if (showSettingsDialog) {
                                    BusinessSettingsDialog(
                                        settings = data.settings,
                                        themeMode = themeMode,
                                        onSelectThemeMode = onSelectThemeMode,
                                        onDismiss = { showSettingsDialog = false },
                                        onSave = { name, phone, address, bank, receiptFooter, days, enabled, onError ->
                                            viewModel.saveBusinessSettings(
                                                businessName = name,
                                                businessPhone = phone,
                                                businessAddress = address,
                                                bankAccountInfo = bank,
                                                receiptFooterText = receiptFooter,
                                                reminderDaysBefore = days,
                                                remindersEnabled = enabled,
                                                onDone = { showSettingsDialog = false },
                                                onError = onError,
                                            )
                                        },
                                    )
                                }

                                customerFormTarget?.let { (_, existingCustomer) ->
                                    CustomerFormDialog(
                                        existing = existingCustomer,
                                        onDismiss = { customerFormTarget = null },
                                        onSave = { name, phone, email, address, pkg, fee, due, active, onError ->
                                            viewModel.saveCustomer(
                                                existingCustomerId = existingCustomer?.id,
                                                name = name,
                                                phone = phone,
                                                email = email,
                                                address = address,
                                                packageName = pkg,
                                                monthlyFee = fee,
                                                dueDay = due,
                                                active = active,
                                                notes = existingCustomer?.notes ?: "",
                                                onDone = { customerFormTarget = null },
                                                onError = onError,
                                            )
                                        },
                                        onDelete = existingCustomer?.let { c ->
                                            {
                                                viewModel.deleteCustomer(c.id) {
                                                    customerFormTarget = null
                                                }
                                            }
                                        },
                                    )
                                }

                                paymentFormTarget?.let { (customer, existingPayment) ->
                                    val isNewPayment = existingPayment == null
                                    PaymentFormDialog(
                                        customer = customer,
                                        initialYearMonth = data.selectedMonth,
                                        existingPayment = existingPayment,
                                        onDismiss = { paymentFormTarget = null },
                                        onSave = { ym, amount, wallet, bankRef, receiptNo, paidMillis, notes, onError ->
                                            viewModel.recordOrUpdatePayment(
                                                existingPaymentId = existingPayment?.id,
                                                customer = customer,
                                                yearMonth = ym,
                                                amount = amount,
                                                paymentMethod = wallet,
                                                bankReference = bankRef,
                                                receiptNumber = receiptNo,
                                                paidDateMillis = paidMillis,
                                                notes = notes,
                                                onSaved = { savedPayment ->
                                                    paymentFormTarget = null
                                                    if (isNewPayment) {
                                                        receiptPreviewTarget = savedPayment
                                                    }
                                                },
                                                onError = onError,
                                            )
                                        },
                                        onDelete = existingPayment?.let { p ->
                                            {
                                                viewModel.deletePayment(p.id) {
                                                    paymentFormTarget = null
                                                }
                                            }
                                        },
                                    )
                                }

                                transferFormTarget?.let { (_, existingTransfer) ->
                                    WalletTransferDialog(
                                        initialYearMonth = data.selectedMonth,
                                        existing = existingTransfer,
                                        onDismiss = { transferFormTarget = null },
                                        onSave = { ym, fromWallet, toWallet, amount, dateMillis, notes, onError ->
                                            viewModel.saveWalletTransfer(
                                                existingTransferId = existingTransfer?.id,
                                                yearMonth = ym,
                                                fromWallet = fromWallet,
                                                toWallet = toWallet,
                                                amount = amount,
                                                transferDateMillis = dateMillis,
                                                notes = notes,
                                                onDone = { transferFormTarget = null },
                                                onError = onError,
                                            )
                                        },
                                        onDelete = existingTransfer?.let { t ->
                                            {
                                                viewModel.deleteWalletTransfer(t.id) {
                                                    transferFormTarget = null
                                                }
                                            }
                                        },
                                    )
                                }

                                customerHistoryTargetId?.let { histId ->
                                    val histCustomer = data.customers.firstOrNull { it.id == histId }
                                    if (histCustomer == null) {
                                        LaunchedEffect(histId) { customerHistoryTargetId = null }
                                    } else {
                                        CustomerHistoryDialog(
                                            customer = histCustomer,
                                            selectedMonth = data.selectedMonth,
                                            allPayments = data.allPayments,
                                            onDismiss = { customerHistoryTargetId = null },
                                            onEditCustomer = {
                                                customerHistoryTargetId = null
                                                customerFormTarget = true to histCustomer
                                            },
                                            onRecordPaymentForSelectedMonth = {
                                                customerHistoryTargetId = null
                                                paymentFormTarget = histCustomer to null
                                            },
                                            onEditPayment = { payment ->
                                                customerHistoryTargetId = null
                                                paymentFormTarget = histCustomer to payment
                                            },
                                            onOpenReceipt = { payment ->
                                                customerHistoryTargetId = null
                                                receiptPreviewTarget = payment
                                            },
                                            onSendWhatsAppReminder = {
                                                ReceiptService.sendWhatsAppDueReminder(
                                                    context = context,
                                                    customer = histCustomer,
                                                    yearMonth = data.selectedMonth,
                                                    settings = data.settings,
                                                ).onFailure {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Tidak dapat membuka WhatsApp atau aplikasi pesan.")
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
