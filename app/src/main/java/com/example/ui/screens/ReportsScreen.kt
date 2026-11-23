package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.MonthPeriodBar
import com.example.ui.components.SixMonthPerformanceChart
import com.example.ui.components.ThemeModeSelector
import com.example.ui.components.WalletSplitBar
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoMediumStyle
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.netKasSemanticColors
import com.example.util.AppThemeMode
import com.example.util.Formatters
import com.example.viewmodel.NetKasDashboardData

@Composable
fun ReportsScreen(
    data: NetKasDashboardData,
    userEmail: String?,
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onExportPdf: () -> Unit,
    onSavePdfToDevice: () -> Unit,
    onExportExcel: () -> Unit,
    onSaveExcelToDevice: () -> Unit,
    onOpenSettings: () -> Unit,
    onSendDueNotification: () -> Unit,
    onSignOut: () -> Unit,
) {
    val semantic = netKasSemanticColors()
    val summary = data.summary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Laporan & Ekspor Pemasukan",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Analisis pemasukan 6 bulan, proporsi dompet, dan ekspor otomatis PDF / Excel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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

        // Automatic PDF & Excel Export Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ekspor Laporan Bulanan Otomatis",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Buat laporan lengkap periode ${Formatters.formatYearMonthFull(data.selectedMonth)} (Total Pemasukan, Dompet Tunai vs Transfer Bank, Pembayaran & Belum Bayar).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))

                    // PDF Export Row (Share & Save to Device)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onExportPdf,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonExpense,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("export_pdf_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bagikan PDF",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        OutlinedButton(
                            onClick = onSavePdfToDevice,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.danger),
                            border = BorderStroke(1.dp, semantic.danger.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_pdf_device_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Simpan PDF",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Excel Export Row (Share & Save to Device)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onExportExcel,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("export_excel_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bagikan Excel",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        OutlinedButton(
                            onClick = onSaveExcelToDevice,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.income),
                            border = BorderStroke(1.dp, semantic.income.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_excel_device_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Simpan Excel",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        // 6-Month Income Performance Chart
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tren Pemasukan 6 Bulan Terakhir",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Ketuk label bulan di bawah grafik untuk melihat detail Tunai vs Transfer Bank",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    SixMonthPerformanceChart(points = data.trendSixMonths)
                }
            }
        }

        // Income & Wallet Breakdown Summary
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan Pemasukan & Dompet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    ReportStatRow("Pemasukan Tunai", Formatters.formatRupiah(summary.cashIncome), semantic.cash)
                    ReportStatRow("Pemasukan Transfer Bank", Formatters.formatRupiah(summary.bankIncome), semantic.bank)
                    ReportStatRow("Tunggakan (${summary.unpaidCount} org)", Formatters.formatRupiah(summary.unpaidAmount), semantic.danger)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Total Pemasukan",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = Formatters.formatRupiah(summary.totalIncome),
                            style = MonoMediumStyle,
                            color = semantic.income,
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Proporsi Pemasukan Pelanggan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    WalletSplitBar(
                        cashAmount = summary.cashIncome,
                        bankAmount = summary.bankIncome,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (summary.cashToBankTransfer > 0L) {
                        ReportStatRow("Setor Tunai → Bank", Formatters.formatRupiah(summary.cashToBankTransfer))
                    }
                    if (summary.bankToCashTransfer > 0L) {
                        ReportStatRow("Tarik Bank → Tunai", Formatters.formatRupiah(summary.bankToCashTransfer))
                    }
                    ReportStatRow("Saldo Dompet Tunai (Bulan Ini)", Formatters.formatRupiah(summary.netCashBalance))
                    ReportStatRow("Saldo Transfer Bank (Bulan Ini)", Formatters.formatRupiah(summary.netBankBalance))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ReportStatRow(
                        "Total Akumulasi Dompet Tunai",
                        Formatters.formatRupiah(summary.cumulativeCashBalance),
                        if (summary.cumulativeCashBalance >= 0) semantic.income else semantic.danger,
                    )
                    ReportStatRow(
                        "Total Akumulasi Transfer Bank",
                        Formatters.formatRupiah(summary.cumulativeBankBalance),
                        if (summary.cumulativeBankBalance >= 0) semantic.income else semantic.danger,
                    )
                }
            }
        }

        // Business Settings, Reminders & Account Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Profil Usaha, Tampilan & Akun",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = "Akun Cloud Aktif: $userEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    ThemeModeSelector(
                        selectedMode = themeMode,
                        onSelectMode = onSelectThemeMode,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reports_settings_button"),
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Profil & Kuitansi")
                        }
                        OutlinedButton(
                            onClick = onSendDueNotification,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cek Pengingat")
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.danger),
                        border = BorderStroke(1.dp, semantic.danger.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_sign_out))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ReportStatRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MonoSmallStyle,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}
