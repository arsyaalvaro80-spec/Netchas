package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BillingStatus
import com.example.data.CustomerBillingItem
import com.example.data.Payment
import com.example.data.WalletTransfer
import com.example.data.WalletType
import com.example.ui.components.MonthPeriodBar
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.MonoLargeStyle
import com.example.ui.theme.MonoMediumStyle
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.netKasSemanticColors
import com.example.util.AppThemeMode
import com.example.util.Formatters
import com.example.viewmodel.DailyCashbookEntry
import com.example.viewmodel.NetKasDashboardData

@Composable
fun DashboardScreen(
    data: NetKasDashboardData,
    themeMode: AppThemeMode,
    onCycleThemeMode: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddCustomer: () -> Unit,
    onAddTransfer: () -> Unit,
    onGoToBilling: () -> Unit,
    onGoToReports: () -> Unit,
    onSendDueNotification: () -> Unit,
    onSendWhatsAppReminder: (CustomerBillingItem) -> Unit,
    onOpenPaymentEdit: (Payment) -> Unit,
    onOpenReceipt: (Payment) -> Unit,
    onOpenTransferEdit: (WalletTransfer) -> Unit,
) {
    val semantic = netKasSemanticColors()
    var summaryExpanded by remember { mutableStateOf(false) }
    var walletFilter by remember { mutableStateOf<WalletType?>(null) }

    val filteredCashbook = remember(data.cashbookEntries, walletFilter) {
        if (walletFilter == null) data.cashbookEntries
        else data.cashbookEntries.filter { it.wallet == walletFilter || it.secondaryWallet == walletFilter }
    }

    val urgentCustomers = remember(data.billingItems) {
        data.billingItems.filter {
            it.status == BillingStatus.OVERDUE || it.status == BillingStatus.DUE_SOON
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Header with Business Name & Settings
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.settings.businessName.ifBlank { stringResource(R.string.app_name) },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Dashboard Pembukuan & Tagihan Wi-Fi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val themeIcon = when (themeMode) {
                        AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                        AppThemeMode.DARK -> Icons.Rounded.DarkMode
                        AppThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                    }
                    IconButton(
                        onClick = onCycleThemeMode,
                        modifier = Modifier.testTag("quick_theme_toggle_button"),
                    ) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "Mode tampilan: ${themeMode.labelId}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Pengaturan usaha dan kuitansi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Period Selector (supports previous months editing)
        item {
            MonthPeriodBar(
                selectedMonth = data.selectedMonth,
                isHistoricalMonth = data.isHistoricalMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onOpenPicker = onOpenMonthPicker,
                onJumpCurrent = onJumpCurrentMonth,
            )
        }

        // Total Income Hero Card
        item {
            val summary = data.summary
            val totalBilled = summary.paidCount + summary.unpaidCount
            val collectionPct = if (totalBilled > 0) {
                ((summary.paidCount.toDouble() / totalBilled.toDouble()) * 100.0).toInt()
            } else 0

            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { summaryExpanded = !summaryExpanded }
                    .testTag("income_hero_card"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, EmeraldPrimaryDark, SlateSecondary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFFA7F3D0),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.total_income_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFD5F2E7),
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Lunas $collectionPct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (summaryExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = "Rincian pemasukan",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = Formatters.formatRupiah(summary.totalIncome),
                            style = MonoLargeStyle,
                            color = Color.White,
                        )

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            HeroMiniStat(
                                label = "Tunai",
                                value = Formatters.formatRupiahCompact(summary.cashIncome),
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.weight(1f),
                            )
                            HeroMiniStat(
                                label = "Transfer Bank",
                                value = Formatters.formatRupiahCompact(summary.bankIncome),
                                tint = Color(0xFFBAE6FD),
                                modifier = Modifier.weight(1f),
                            )
                            HeroMiniStat(
                                label = "Lunas",
                                value = "${summary.paidCount}/$totalBilled",
                                tint = Color(0xFFA7F3D0),
                                modifier = Modifier.weight(0.75f),
                            )
                        }

                        AnimatedVisibility(visible = summaryExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                                Spacer(Modifier.height(12.dp))
                                IncomeDetailLine("Pemasukan Tunai", Formatters.formatRupiah(summary.cashIncome))
                                IncomeDetailLine("Pemasukan Transfer Bank", Formatters.formatRupiah(summary.bankIncome))
                                if (summary.cashToBankTransfer > 0L) {
                                    IncomeDetailLine("Setor Tunai → Bank", Formatters.formatRupiah(summary.cashToBankTransfer))
                                }
                                if (summary.bankToCashTransfer > 0L) {
                                    IncomeDetailLine("Tarik Bank → Tunai", Formatters.formatRupiah(summary.bankToCashTransfer))
                                }
                                IncomeDetailLine(
                                    "Total Akumulasi Tunai",
                                    Formatters.formatRupiah(summary.cumulativeCashBalance)
                                )
                                IncomeDetailLine(
                                    "Total Akumulasi Transfer Bank",
                                    Formatters.formatRupiah(summary.cumulativeBankBalance)
                                )
                                IncomeDetailLine(
                                    "Potensi Tunggakan (${summary.unpaidCount} org)",
                                    Formatters.formatRupiah(summary.unpaidAmount)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Separate Cash Wallet vs Bank Transfer Bookkeeping Cards + Internal Wallet Transfer Action
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WalletBalanceCard(
                        title = stringResource(R.string.cash_wallet_label),
                        netBalance = data.summary.netCashBalance,
                        cumulativeBalance = data.summary.cumulativeCashBalance,
                        income = data.summary.cashIncome,
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = semantic.cash,
                        dangerColor = semantic.danger,
                        onClick = {
                            walletFilter = if (walletFilter == WalletType.CASH) null else WalletType.CASH
                        },
                        isSelected = walletFilter == WalletType.CASH,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cash_wallet_card"),
                    )
                    WalletBalanceCard(
                        title = stringResource(R.string.bank_wallet_label),
                        netBalance = data.summary.netBankBalance,
                        cumulativeBalance = data.summary.cumulativeBankBalance,
                        income = data.summary.bankIncome,
                        icon = Icons.Rounded.AccountBalance,
                        accentColor = semantic.bank,
                        dangerColor = semantic.danger,
                        onClick = {
                            walletFilter = if (walletFilter == WalletType.BANK_TRANSFER) null else WalletType.BANK_TRANSFER
                        },
                        isSelected = walletFilter == WalletType.BANK_TRANSFER,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bank_wallet_card"),
                    )
                }

                OutlinedButton(
                    onClick = onAddTransfer,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_wallet_transfer_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = semantic.bank,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pindah Saldo (Setor Tunai ke Bank / Tarik Tunai)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // Due-Date Cashflow Reminder Banner
        item {
            DueDateReminderCard(
                urgentItems = urgentCustomers,
                unpaidTotal = data.summary.unpaidAmount,
                unpaidCount = data.summary.unpaidCount,
                onSendNotification = onSendDueNotification,
                onSendWhatsApp = onSendWhatsAppReminder,
                onViewAllBilling = onGoToBilling,
            )
        }

        // Quick Actions Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionTile(
                    icon = Icons.Rounded.PersonAdd,
                    label = "Pelanggan",
                    onClick = onAddCustomer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_customer"),
                )
                QuickActionTile(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    label = "Tagihan",
                    onClick = onGoToBilling,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_go_billing"),
                )
                QuickActionTile(
                    icon = Icons.Rounded.PictureAsPdf,
                    label = "PDF / XLS",
                    onClick = onGoToReports,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_go_reports"),
                )
                QuickActionTile(
                    icon = Icons.Rounded.Settings,
                    label = "Info Usaha",
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_open_settings"),
                )
            }
        }

        // Daily Cashbook Section Header & Wallet Filter Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.recent_transactions_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${filteredCashbook.size} transaksi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = walletFilter == null,
                        onClick = { walletFilter = null },
                        label = { Text("Semua") },
                    )
                    FilterChip(
                        selected = walletFilter == WalletType.CASH,
                        onClick = { walletFilter = WalletType.CASH },
                        label = { Text("Tunai") },
                    )
                    FilterChip(
                        selected = walletFilter == WalletType.BANK_TRANSFER,
                        onClick = { walletFilter = WalletType.BANK_TRANSFER },
                        label = { Text("Transfer Bank") },
                    )
                }
            }
        }

        if (filteredCashbook.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Belum ada catatan transaksi pada periode ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tambahkan pelanggan Wi-Fi atau catat pembayaran di tab Tagihan untuk memulai pembukuan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(filteredCashbook, key = { it.id }) { entry ->
                CashbookTransactionCard(
                    entry = entry,
                    onClick = {
                        when {
                            entry.payment != null -> onOpenPaymentEdit(entry.payment)
                            entry.transfer != null -> onOpenTransferEdit(entry.transfer)
                        }
                    },
                    onOpenReceipt = entry.payment?.let { p -> { onOpenReceipt(p) } },
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun HeroMiniStat(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MonoSmallStyle,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IncomeDetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
        )
        Text(
            text = value,
            style = MonoSmallStyle,
            color = Color.White,
        )
    }
}

@Composable
private fun WalletBalanceCard(
    title: String,
    netBalance: Long,
    cumulativeBalance: Long,
    income: Long,
    icon: ImageVector,
    accentColor: Color,
    dangerColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = Formatters.formatRupiah(netBalance),
                style = MonoMediumStyle,
                color = if (netBalance >= 0) MaterialTheme.colorScheme.onSurface else dangerColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Masuk: +${Formatters.formatRupiahCompact(income)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Total: ${Formatters.formatRupiahCompact(cumulativeBalance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (cumulativeBalance >= 0) accentColor else dangerColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DueDateReminderCard(
    urgentItems: List<CustomerBillingItem>,
    unpaidTotal: Long,
    unpaidCount: Int,
    onSendNotification: () -> Unit,
    onSendWhatsApp: (CustomerBillingItem) -> Unit,
    onViewAllBilling: () -> Unit,
) {
    val semantic = netKasSemanticColors()
    val hasUrgent = urgentItems.isNotEmpty()
    val containerColor = if (hasUrgent) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    }
    val accentColor = if (hasUrgent) semantic.danger else semantic.income
    val buttonContainerColor = if (hasUrgent) CrimsonExpense else EmeraldPrimary

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = if (hasUrgent) Icons.Rounded.WarningAmber else Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (hasUrgent) {
                                "${urgentItems.size} Tagihan Jatuh Tempo / H-Dekat"
                            } else if (unpaidCount > 0) {
                                "$unpaidCount Tagihan Belum Lunas"
                            } else {
                                "Semua Tagihan Bulan Ini Lunas"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (unpaidCount > 0) {
                                "Sisa piutang: ${Formatters.formatRupiah(unpaidTotal)}"
                            } else {
                                "Arus kas pelanggan lancar tanpa tunggakan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Button(
                    onClick = onSendNotification,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainerColor,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.testTag("trigger_due_notification_button"),
                ) {
                    Text(
                        text = "Notif",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            if (urgentItems.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                urgentItems.take(2).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.customer.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val dueLabel = if (item.status == BillingStatus.OVERDUE) {
                                "Terlambat ${-item.daysUntilDue} hari • ${Formatters.formatRupiah(item.customer.monthlyFee)}"
                            } else if (item.daysUntilDue == 0) {
                                "Jatuh tempo HARI INI • ${Formatters.formatRupiah(item.customer.monthlyFee)}"
                            } else {
                                "Jatuh tempo H-${item.daysUntilDue} • ${Formatters.formatRupiah(item.customer.monthlyFee)}"
                            }
                            Text(
                                text = dueLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSendWhatsApp(item) },
                        ) {
                            Text(
                                text = "Ingatkan WA",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.income,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                if (urgentItems.size > 2) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Lihat semua ${urgentItems.size} tagihan mendesak →",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier
                            .clickable(onClick = onViewAllBilling)
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CashbookTransactionCard(
    entry: DailyCashbookEntry,
    onClick: () -> Unit,
    onOpenReceipt: (() -> Unit)?,
) {
    val semantic = netKasSemanticColors()
    val walletAccent = when {
        entry.isTransfer -> semantic.bank
        entry.wallet == WalletType.CASH -> semantic.cash
        else -> semantic.bank
    }
    val leadingBg = if (entry.isTransfer) {
        semantic.bank.copy(alpha = 0.14f)
    } else {
        semantic.income.copy(alpha = 0.14f)
    }
    val leadingTint = if (entry.isTransfer) semantic.bank else semantic.income
    val leadingIcon = if (entry.isTransfer) Icons.Rounded.SwapHoriz else Icons.Rounded.ArrowDownward
    val leadingDesc = if (entry.isTransfer) "Pindah saldo dompet" else "Pemasukan"
    val badgeText = if (entry.isTransfer && entry.secondaryWallet != null) {
        "${entry.wallet.labelId} → ${entry.secondaryWallet.labelId}"
    } else {
        entry.wallet.labelId
    }
    val amountPrefix = if (entry.isTransfer) "⇄ " else "+"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(leadingBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = leadingDesc,
                    tint = leadingTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${entry.subtitle} • ${Formatters.formatDateFull(entry.dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = walletAccent.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = walletAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    if (onOpenReceipt != null) {
                        Text(
                            text = "Kuitansi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onOpenReceipt),
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = amountPrefix + Formatters.formatRupiah(entry.amount),
                style = MonoSmallStyle,
                fontWeight = FontWeight.Bold,
                color = leadingTint,
            )
        }
    }
}
