package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BusinessSettings
import com.example.data.Customer
import com.example.data.Payment
import com.example.data.WalletType
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MonoLargeStyle
import com.example.util.Formatters
import com.example.util.ReceiptService

private val ReceiptInkBlack = Color(0xFF111111)
private val ReceiptInkMuted = Color(0xFF444444)
private val ReceiptPaperFill = Color(0xFFF5F5F5)
private val ReceiptDashGray = Color(0xFF555555)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptFullScreen(
    payment: Payment,
    customer: Customer?,
    settings: BusinessSettings,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onPrint: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareEmail: () -> Unit,
    onSaveImageToDevice: () -> Unit,
    onSaveReceiptProfile: (
        businessName: String,
        businessPhone: String,
        businessPhone2: String,
        bankAccountInfo: String,
        receiptFooterText: String,
    ) -> Unit,
) {
    var showQuickEditDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("receipt_fullscreen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kuitansi Hitam Putih",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_receipt_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali dari kuitansi",
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(CircleShape)
                            .clickable { showQuickEditDialog = true }
                            .testTag("edit_receipt_info_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit info kuitansi",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Edit Info",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ReceiptInkBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val isDarkTheme = LocalIsDarkTheme.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onShareWhatsApp,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) Color(0xFFF5F5F5) else ReceiptInkBlack,
                                contentColor = if (isDarkTheme) ReceiptInkBlack else Color.White,
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_receipt_whatsapp_button"),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kirim WA", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = onPrint,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("print_receipt_button"),
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cetak Printer", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onSaveImageToDevice,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_receipt_image_button"),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Simpan PNG", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = onShareEmail,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_receipt_email_button"),
                        ) {
                            Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kirim Email", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(Color.White),
        ) {
            // Monochrome Black & White Paper Header (tappable to edit)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showQuickEditDialog = true }
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = settings.businessName.ifBlank { "NetKas Wi-Fi" }.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ReceiptInkBlack,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                if (settings.businessAddress.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = settings.businessAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = ReceiptInkMuted,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        tint = ReceiptInkBlack,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    val combinedPhones = settings.formattedPhones()
                    Text(
                        text = if (combinedPhones.isNotBlank()) {
                            "Telp / WA: $combinedPhones"
                        } else {
                            "Ketuk untuk mengisi Nomor HP / CS Usaha"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = ReceiptInkBlack,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Classic Double Black Ledger Rule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(3.dp)
                    .background(ReceiptInkBlack)
            )
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .background(ReceiptInkBlack)
            )

            // Main Receipt Body (full width monochrome)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Receipt title & Monochrome LUNAS Stamp
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "KUITANSI PEMBAYARAN WI-FI",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReceiptInkBlack,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ReceiptPaperFill,
                        border = BorderStroke(1.5.dp, ReceiptInkBlack),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = ReceiptInkBlack,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "STATUS: LUNAS",
                                style = MaterialTheme.typography.labelLarge,
                                color = ReceiptInkBlack,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }

                DashedReceiptDivider()

                // Transaction Detail Rows
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FullReceiptRow("No. Kuitansi", payment.receiptNumber)
                    FullReceiptRow("Periode Tagihan", Formatters.formatYearMonthFull(payment.yearMonth))
                    FullReceiptRow("Tanggal Bayar", Formatters.formatDateFull(payment.paidDateMillis))
                    FullReceiptRow("Nama Pelanggan", payment.customerName)
                    FullReceiptRow("Paket Internet", payment.packageName)
                    FullReceiptRow("Metode Bayar", payment.wallet.labelId)
                    if (payment.wallet == WalletType.BANK_TRANSFER && payment.bankReference.isNotBlank()) {
                        FullReceiptRow("Referensi Bank", payment.bankReference)
                    }
                    if (!customer?.address.isNullOrBlank()) {
                        FullReceiptRow("Alamat", customer.address)
                    }
                }

                // Monochrome Highlighted Total Amount Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ReceiptPaperFill,
                    border = BorderStroke(2.dp, ReceiptInkBlack),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "TOTAL PEMBAYARAN DITERIMA",
                            style = MaterialTheme.typography.labelMedium,
                            color = ReceiptInkMuted,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatRupiah(payment.amount),
                            style = MonoLargeStyle,
                            color = ReceiptInkBlack,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Monochrome Transfer Bank Account Info Box (tappable to edit)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAFAFA),
                    border = BorderStroke(1.2.dp, ReceiptInkBlack.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showQuickEditDialog = true },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = ReceiptInkBlack,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "REKENING TRANSFER RESMI",
                                style = MaterialTheme.typography.labelSmall,
                                color = ReceiptInkBlack,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = settings.bankAccountInfo.ifBlank {
                                "Belum diisi — ketuk untuk menambahkan rekening transfer"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (settings.bankAccountInfo.isNotBlank()) ReceiptInkBlack else ReceiptInkMuted,
                            fontWeight = if (settings.bankAccountInfo.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Customer delivery targets & notes
                if (payment.notes.isNotBlank()) {
                    Text(
                        text = "Catatan: ${payment.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReceiptInkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val customerPhone = customer?.phone.orEmpty()
                val customerEmail = customer?.email.orEmpty()
                if (customerPhone.isNotBlank() || customerEmail.isNotBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (customerPhone.isNotBlank()) {
                            Text(
                                text = "WA Pelanggan: $customerPhone",
                                style = MaterialTheme.typography.labelSmall,
                                color = ReceiptInkBlack,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        if (customerEmail.isNotBlank()) {
                            Text(
                                text = "Email Pelanggan: $customerEmail",
                                style = MaterialTheme.typography.labelSmall,
                                color = ReceiptInkMuted,
                            )
                        }
                    }
                }

                DashedReceiptDivider()

                // Editable Footer / Closing Words on Receipt
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showQuickEditDialog = true }
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .testTag("receipt_footer_section"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = ReceiptService.effectiveFooterText(settings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ReceiptInkMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = ReceiptPaperFill,
                        border = BorderStroke(1.dp, ReceiptInkBlack.copy(alpha = 0.25f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = ReceiptInkBlack,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Ubah Nama Usaha, No. HP, Rekening & Kata Terakhir",
                                style = MaterialTheme.typography.labelSmall,
                                color = ReceiptInkBlack,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showQuickEditDialog) {
        ReceiptProfileQuickEditDialog(
            settings = settings,
            onDismiss = { showQuickEditDialog = false },
            onSave = { bizName, bizPhone, bizPhone2, bankInfo, footerText ->
                onSaveReceiptProfile(bizName, bizPhone, bizPhone2, bankInfo, footerText)
                showQuickEditDialog = false
            },
        )
    }
}

@Composable
private fun FullReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = ReceiptInkMuted,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = ReceiptInkBlack,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DashedReceiptDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = ReceiptDashGray,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
        )
    }
}

@Composable
private fun ReceiptProfileQuickEditDialog(
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onSave: (
        businessName: String,
        businessPhone: String,
        businessPhone2: String,
        bankAccountInfo: String,
        receiptFooterText: String,
    ) -> Unit,
) {
    var businessName by remember { mutableStateOf(settings.businessName) }
    var businessPhone by remember { mutableStateOf(settings.businessPhone) }
    var businessPhone2 by remember { mutableStateOf(settings.businessPhone2) }
    var bankAccountInfo by remember { mutableStateOf(settings.bankAccountInfo) }
    var receiptFooterText by remember {
        mutableStateOf(
            settings.receiptFooterText.ifBlank { BusinessSettings.DEFAULT_RECEIPT_FOOTER }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Edit Info Kuitansi", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Perubahan langsung tampil di kuitansi hitam putih, kiriman WA, cetak printer, dan Email.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it.take(80) },
                    label = { Text("Nama Usaha") },
                    placeholder = { Text("Contoh: NetKas Wi-Fi") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_edit_business_name"),
                )

                OutlinedTextField(
                    value = businessPhone,
                    onValueChange = { businessPhone = it.take(30) },
                    label = { Text("Nomor HP / WA CS 1") },
                    placeholder = { Text("Contoh: 081234567890") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_edit_business_phone"),
                )

                OutlinedTextField(
                    value = businessPhone2,
                    onValueChange = { businessPhone2 = it.take(30) },
                    label = { Text("Nomor HP / WA CS 2 (opsional)") },
                    placeholder = { Text("Contoh: 085712345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_edit_business_phone2"),
                )

                OutlinedTextField(
                    value = bankAccountInfo,
                    onValueChange = { bankAccountInfo = it.take(180) },
                    label = { Text("Rekening Transfer") },
                    placeholder = { Text("Contoh: BCA 1234567890 a.n. Nama Anda") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_edit_bank_account"),
                )

                OutlinedTextField(
                    value = receiptFooterText,
                    onValueChange = { receiptFooterText = it.take(240) },
                    label = { Text("Kata-kata Terakhir Kuitansi") },
                    placeholder = { Text(BusinessSettings.DEFAULT_RECEIPT_FOOTER) },
                    supportingText = { Text("${receiptFooterText.length}/240 karakter") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_edit_footer_text"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            onSave(
                                businessName,
                                businessPhone,
                                businessPhone2,
                                bankAccountInfo,
                                receiptFooterText,
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("receipt_edit_save_button"),
                    ) {
                        Text("Simpan Kuitansi")
                    }
                }
            }
        }
    }
}
