package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.BusinessSettings
import com.example.data.Customer
import com.example.data.Payment
import com.example.data.WalletType
import java.io.File
import java.io.FileOutputStream

object ReceiptService {

    fun effectiveFooterText(settings: BusinessSettings): String =
        settings.receiptFooterText.trim().ifEmpty { BusinessSettings.DEFAULT_RECEIPT_FOOTER }

    fun renderReceiptBitmap(
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Bitmap {
        val rows = mutableListOf(
            "No. Kuitansi" to payment.receiptNumber,
            "Periode Tagihan" to Formatters.formatYearMonthFull(payment.yearMonth),
            "Tanggal Bayar" to Formatters.formatDateFull(payment.paidDateMillis),
            "Nama Pelanggan" to payment.customerName,
            "Paket Internet" to payment.packageName,
            "Metode Bayar" to payment.wallet.labelId,
        )
        if (payment.wallet == WalletType.BANK_TRANSFER && payment.bankReference.isNotBlank()) {
            rows.add("Referensi Bank" to payment.bankReference.trim())
        }
        if (!customer?.address.isNullOrBlank()) {
            rows.add("Alamat" to customer.address.trim())
        }

        val width = 1080
        val frameInset = 16f
        val marginX = 36f
        val contentWidth = width - (marginX * 2f)
        val centerX = width / 2f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222222")
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        val thickRulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 6f
        }
        val thinRulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 2.5f
        }
        val kuitansiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3F4F6")
        }
        val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#444444")
            strokeWidth = 3.5f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(16f, 10f), 0f)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            textSize = 37f
            textAlign = Paint.Align.LEFT
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 39f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val amountBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F5F5")
        }
        val amountBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
        }
        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222222")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val formattedTotal = Formatters.formatRupiah(payment.amount)
        val totalValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 76f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val maxTotalTextWidth = contentWidth - 48f
        while (totalValPaint.textSize > 42f && totalValPaint.measureText(formattedTotal) > maxTotalTextWidth) {
            totalValPaint.textSize -= 2f
        }

        val bankBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFAFA")
        }
        val bankBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222222")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val bankHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222222")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222222")
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }

        // Pre-wrap all dynamic text blocks to span full width cleanly with zero truncation
        val businessTitleLines = wrapText(
            settings.businessName.ifBlank { "NetKas Wi-Fi" }.uppercase(),
            titlePaint,
            contentWidth,
        )
        val businessAddressLines = if (settings.businessAddress.isNotBlank()) {
            wrapText(settings.businessAddress.trim(), subtitlePaint, contentWidth)
        } else {
            emptyList()
        }
        val businessPhoneLines = if (settings.businessPhone.isNotBlank()) {
            wrapText("Telp / WA: ${settings.businessPhone.trim()}", subtitlePaint, contentWidth)
        } else {
            emptyList()
        }
        val wrappedRows = rows.map { (label, rawValue) ->
            val labelWidth = labelPaint.measureText(label)
            val maxValueWidth = (contentWidth - labelWidth - 28f).coerceAtLeast(contentWidth * 0.48f)
            val lines = wrapText(rawValue.ifBlank { "-" }, valuePaint, maxValueWidth)
            label to lines.ifEmpty { listOf("-") }
        }
        val bankLines = if (settings.bankAccountInfo.isNotBlank()) {
            wrapText(settings.bankAccountInfo.trim(), bankPaint, contentWidth - 48f)
        } else {
            emptyList()
        }
        val noteLines = if (payment.notes.isNotBlank()) {
            wrapText("Catatan: ${payment.notes.trim()}", notePaint, contentWidth)
        } else {
            emptyList()
        }
        val footerLines = wrapText(effectiveFooterText(settings), footerPaint, contentWidth)

        fun layoutAndDraw(canvas: Canvas?): Float {
            var y = frameInset + 48f

            // Monochrome Header Block
            val titleFm = titlePaint.fontMetrics
            val titleStep = (titleFm.descent - titleFm.ascent) + 8f
            businessTitleLines.forEach { line ->
                canvas?.drawText(line, centerX, y - titleFm.ascent, titlePaint)
                y += titleStep
            }

            val subFm = subtitlePaint.fontMetrics
            val subStep = (subFm.descent - subFm.ascent) + 8f
            if (businessAddressLines.isNotEmpty()) {
                y += 8f
                businessAddressLines.forEach { line ->
                    canvas?.drawText(line, centerX, y - subFm.ascent, subtitlePaint)
                    y += subStep
                }
            }
            if (businessPhoneLines.isNotEmpty()) {
                y += 6f
                businessPhoneLines.forEach { line ->
                    canvas?.drawText(line, centerX, y - subFm.ascent, subtitlePaint)
                    y += subStep
                }
            }

            // Classic double black horizontal divider under header
            y += 26f
            canvas?.drawLine(marginX, y, width - marginX, y, thickRulePaint)
            canvas?.drawLine(marginX, y + 12f, width - marginX, y + 12f, thinRulePaint)
            y += 42f

            // Receipt Title
            val kuitansiFm = kuitansiLabelPaint.fontMetrics
            canvas?.drawText("KUITANSI PEMBAYARAN WI-FI", centerX, y - kuitansiFm.ascent, kuitansiLabelPaint)
            y += (kuitansiFm.descent - kuitansiFm.ascent) + 22f

            // High-contrast Monochrome LUNAS Stamp
            val badgeWidth = 440f
            val badgeHeight = 78f
            val badgeRect = RectF(centerX - badgeWidth / 2f, y, centerX + badgeWidth / 2f, y + badgeHeight)
            if (canvas != null) {
                canvas.drawRoundRect(badgeRect, 14f, 14f, badgeBg)
                canvas.drawRoundRect(badgeRect, 14f, 14f, badgeBorder)
                val badgeFm = badgeTextPaint.fontMetrics
                val badgeBaseline = badgeRect.centerY() - (badgeFm.ascent + badgeFm.descent) / 2f
                canvas.drawText("STATUS: LUNAS", centerX, badgeBaseline, badgeTextPaint)
            }
            y += badgeHeight + 34f

            // Monochrome Dashed Divider
            canvas?.drawLine(marginX, y, width - marginX, y, dashPaint)
            y += 32f

            // Full-width Detail Rows with clean multi-line right-aligned wrapping
            val labelFm = labelPaint.fontMetrics
            val valueFm = valuePaint.fontMetrics
            val topToBaseline = maxOf(-labelFm.ascent, -valueFm.ascent)
            val rowLineStep = (valueFm.descent - valueFm.ascent) + 8f
            val rowGap = 22f

            wrappedRows.forEach { (label, valueLines) ->
                val firstBaseline = y + topToBaseline
                canvas?.drawText(label, marginX, firstBaseline, labelPaint)
                valueLines.forEachIndexed { idx, line ->
                    canvas?.drawText(line, width - marginX, firstBaseline + (idx * rowLineStep), valuePaint)
                }
                y += (valueLines.size * rowLineStep) + rowGap
            }

            // Monochrome Amount Highlight Box (full content width)
            y += 10f
            val amountBoxHeight = 200f
            val amountBoxRect = RectF(marginX, y, width - marginX, y + amountBoxHeight)
            if (canvas != null) {
                canvas.drawRoundRect(amountBoxRect, 18f, 18f, amountBoxBg)
                canvas.drawRoundRect(amountBoxRect, 18f, 18f, amountBoxBorder)
                val totalLabelFm = totalLabelPaint.fontMetrics
                val totalValFm = totalValPaint.fontMetrics
                canvas.drawText(
                    "TOTAL PEMBAYARAN DITERIMA",
                    centerX,
                    y + 30f - totalLabelFm.ascent,
                    totalLabelPaint,
                )
                val valCenterY = y + 132f
                val valBaseline = valCenterY - (totalValFm.ascent + totalValFm.descent) / 2f
                canvas.drawText(formattedTotal, centerX, valBaseline, totalValPaint)
            }
            y += amountBoxHeight + 32f

            // Monochrome Bank Account Transfer Info Box (if configured)
            if (bankLines.isNotEmpty()) {
                val bankHeaderFm = bankHeaderPaint.fontMetrics
                val bankFm = bankPaint.fontMetrics
                val bankHeaderH = bankHeaderFm.descent - bankHeaderFm.ascent
                val bankLineStep = (bankFm.descent - bankFm.ascent) + 8f
                val bankBoxHeight = 26f + bankHeaderH + 14f + (bankLines.size * bankLineStep) + 22f
                val bankRect = RectF(marginX, y, width - marginX, y + bankBoxHeight)
                if (canvas != null) {
                    canvas.drawRoundRect(bankRect, 16f, 16f, bankBg)
                    canvas.drawRoundRect(bankRect, 16f, 16f, bankBorder)
                    canvas.drawText(
                        "REKENING TRANSFER RESMI",
                        centerX,
                        y + 26f - bankHeaderFm.ascent,
                        bankHeaderPaint,
                    )
                    var bY = y + 26f + bankHeaderH + 14f
                    bankLines.forEach { line ->
                        canvas.drawText(line, centerX, bY - bankFm.ascent, bankPaint)
                        bY += bankLineStep
                    }
                }
                y += bankBoxHeight + 28f
            }

            // Payment notes (wrapped full width)
            if (noteLines.isNotEmpty()) {
                val noteFm = notePaint.fontMetrics
                val noteStep = (noteFm.descent - noteFm.ascent) + 8f
                noteLines.forEach { line ->
                    canvas?.drawText(line, centerX, y - noteFm.ascent, notePaint)
                    y += noteStep
                }
                y += 20f
            }

            // Custom Footer Closing Message
            canvas?.drawLine(marginX, y, width - marginX, y, dashPaint)
            y += 32f
            val footerFm = footerPaint.fontMetrics
            val footerStep = (footerFm.descent - footerFm.ascent) + 8f
            footerLines.forEach { line ->
                canvas?.drawText(line, centerX, y - footerFm.ascent, footerPaint)
                y += footerStep
            }

            return y + 40f + frameInset
        }

        // Pass 1: Measure exact height (zero clipping and zero wasted bottom whitespace)
        val height = layoutAndDraw(null).toInt().coerceAtLeast(800)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Pure white paper background
        canvas.drawColor(Color.WHITE)

        // Outer crisp monochrome frame (stays distinct in both WhatsApp Light & Dark mode)
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRect(frameInset, frameInset, width - frameInset, height - frameInset, framePaint)

        // Pass 2: Draw full receipt
        layoutAndDraw(canvas)

        return bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        text.split("\n").forEach { paragraph ->
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) return@forEach
            val words = trimmed.split(Regex("\\s+"))
            var current = ""
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) {
                        result.add(current)
                        current = ""
                    }
                    if (paint.measureText(word) <= maxWidth) {
                        current = word
                    } else {
                        // Break extra-long unbroken tokens across lines instead of cutting off with ellipsis
                        val broken = breakLongWord(word, paint, maxWidth)
                        if (broken.size > 1) {
                            result.addAll(broken.dropLast(1))
                        }
                        current = broken.lastOrNull().orEmpty()
                    }
                }
            }
            if (current.isNotEmpty()) result.add(current)
        }
        return result
    }

    private fun breakLongWord(word: String, paint: Paint, maxWidth: Float): List<String> {
        if (word.isEmpty() || paint.measureText(word) <= maxWidth) return listOf(word)
        val parts = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = start + 1
            while (end < word.length && paint.measureText(word, start, end + 1) <= maxWidth) {
                end++
            }
            parts.add(word.substring(start, end))
            start = end
        }
        return parts
    }

    fun saveReceiptPngToCache(
        context: Context,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Uri {
        val bitmap = renderReceiptBitmap(payment, customer, settings)
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val safeReceipt = payment.receiptNumber
            .ifBlank { payment.id.ifBlank { "netkas" } }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(dir, "kuitansi_${safeReceipt}.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } finally {
            bitmap.recycle()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun saveReceiptPngToUri(
        context: Context,
        uri: Uri,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Result<Unit> = runCatching {
        val bitmap = renderReceiptBitmap(payment, customer, settings)
        try {
            val out = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Tidak dapat menyimpan foto kuitansi ke lokasi tujuan.")
            out.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("Gagal mengompresi foto kuitansi.")
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun printReceipt(
        context: Context,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Result<Unit> = runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: throw IllegalStateException("Layanan cetak (Print) tidak tersedia di perangkat ini.")
        val bitmap = renderReceiptBitmap(payment, customer, settings)
        val jobName = "Kuitansi_${payment.receiptNumber.ifBlank { payment.id }}"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                val pdfDoc = PdfDocument()
                try {
                    val pageWidth = 420
                    val pageHeight = ((bitmap.height.toFloat() / bitmap.width.toFloat()) * pageWidth)
                        .toInt()
                        .coerceAtLeast(1)
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val destRect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                    page.canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                    pdfDoc.finishPage(page)

                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.localizedMessage)
                } finally {
                    pdfDoc.close()
                }
            }

            override fun onFinish() {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    fun shareReceiptViaWhatsApp(
        context: Context,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Result<Unit> = runCatching {
        val uri = saveReceiptPngToCache(context, payment, customer, settings)
        val caption = buildString {
            append("Halo ${payment.customerName},\n")
            append("Berikut kuitansi pembayaran Wi-Fi *${settings.businessName.ifBlank { "NetKas Wi-Fi" }}*:\n")
            append("• Periode: ${Formatters.formatYearMonthFull(payment.yearMonth)}\n")
            append("• Paket: ${payment.packageName}\n")
            append("• Nominal: ${Formatters.formatRupiah(payment.amount)}\n")
            append("• Metode: ${payment.wallet.labelId}\n")
            append("• No. Kuitansi: ${payment.receiptNumber}\n")
            if (settings.bankAccountInfo.isNotBlank()) {
                append("• Rekening Transfer: ${settings.bankAccountInfo}\n")
            }
            if (settings.businessPhone.isNotBlank()) {
                append("• CS / WA: ${settings.businessPhone}\n")
            }
            append("\n${effectiveFooterText(settings)}")
        }

        val pm = context.packageManager
        val waPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }

        val normalizedPhone = Formatters.normalizeIndonesianPhoneForWhatsApp(customer?.phone.orEmpty())

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = android.content.ClipData.newRawUri("Kuitansi", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (waPackage != null) {
                setPackage(waPackage)
                if (normalizedPhone.isNotBlank()) {
                    putExtra("jid", "$normalizedPhone@s.whatsapp.net")
                }
            }
        }
        try {
            context.startActivity(
                if (waPackage != null) shareIntent else Intent.createChooser(shareIntent, "Kirim Foto Kuitansi")
            )
        } catch (_: Exception) {
            shareIntent.setPackage(null)
            shareIntent.removeExtra("jid")
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Foto Kuitansi"))
        }
    }

    fun shareReceiptViaEmail(
        context: Context,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Result<Unit> = runCatching {
        val uri = saveReceiptPngToCache(context, payment, customer, settings)
        val subject = "Kuitansi Pembayaran Wi-Fi ${Formatters.formatYearMonthFull(payment.yearMonth)} - ${payment.receiptNumber}"
        val body = buildString {
            append("Yth. ${payment.customerName},\n\n")
            append("Terlampir foto kuitansi pembayaran layanan Wi-Fi ${settings.businessName.ifBlank { "NetKas Wi-Fi" }}.\n")
            append("Periode: ${Formatters.formatYearMonthFull(payment.yearMonth)}\n")
            append("Paket: ${payment.packageName}\n")
            append("Nominal: ${Formatters.formatRupiah(payment.amount)}\n")
            append("Metode: ${payment.wallet.labelId}\n")
            append("Tanggal Bayar: ${Formatters.formatDateFull(payment.paidDateMillis)}\n")
            if (settings.bankAccountInfo.isNotBlank()) {
                append("Rekening Transfer: ${settings.bankAccountInfo}\n")
            }
            if (settings.businessPhone.isNotBlank()) {
                append("CS / WA: ${settings.businessPhone}\n")
            }
            append("\n${effectiveFooterText(settings)}")
        }

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            if (!customer?.email.isNullOrBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(customer.email.trim()))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("Kuitansi", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(emailIntent, "Kirim Kuitansi via Email"))
    }

    fun sendWhatsAppDueReminder(
        context: Context,
        customer: Customer,
        yearMonth: String,
        settings: BusinessSettings,
    ): Result<Unit> = runCatching {
        val biz = settings.businessName.ifBlank { "NetKas Wi-Fi" }
        val message = buildString {
            append("Halo ${customer.name},\n")
            append("Pengingat tagihan Wi-Fi *$biz* untuk periode *${Formatters.formatYearMonthFull(yearMonth)}*:\n")
            append("• Paket: ${customer.packageName}\n")
            append("• Tagihan: ${Formatters.formatRupiah(customer.monthlyFee)}\n")
            append("• Jatuh Tempo: Tanggal ${customer.dueDay}\n")
            if (settings.bankAccountInfo.isNotBlank()) {
                append("\nPembayaran transfer dapat dikirim ke:\n${settings.bankAccountInfo}\n")
            }
            if (settings.businessPhone.isNotBlank()) {
                append("\nHubungi CS / WA: ${settings.businessPhone}\n")
            }
            append("\nMohon konfirmasi setelah melakukan pembayaran. Terima kasih!")
        }

        val normalizedPhone = Formatters.normalizeIndonesianPhoneForWhatsApp(customer.phone)
        val uri = if (normalizedPhone.isNotBlank()) {
            Uri.parse("https://wa.me/$normalizedPhone?text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://wa.me/?text=${Uri.encode(message)}")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(fallback, "Kirim Pengingat Tagihan"))
        }
    }
}
