package com.example

import com.example.util.AppThemeMode
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class NetKasLogicTest {

    @Test
    fun `normalizeIndonesianPhoneForWhatsApp handles common local and +62 08xx formats`() {
        assertEquals("628123456789", Formatters.normalizeIndonesianPhoneForWhatsApp("0812-3456-789"))
        assertEquals("628123456789", Formatters.normalizeIndonesianPhoneForWhatsApp("+62 812 3456 789"))
        assertEquals("628123456789", Formatters.normalizeIndonesianPhoneForWhatsApp("+62 0812-3456-789"))
        assertEquals("628123456789", Formatters.normalizeIndonesianPhoneForWhatsApp("8123456789"))
        assertEquals("", Formatters.normalizeIndonesianPhoneForWhatsApp("   "))
    }

    @Test
    fun `shiftYearMonth crosses year boundaries accurately`() {
        assertEquals("2025-12", Formatters.shiftYearMonth("2026-01", -1))
        assertEquals("2027-01", Formatters.shiftYearMonth("2026-12", 1))
        assertEquals(
            listOf("2025-09", "2025-10", "2025-11", "2025-12", "2026-01", "2026-02"),
            Formatters.lastSixMonthsEndingAt("2026-02")
        )
    }

    @Test
    fun `formatRupiah formats positive, zero, and negative amounts`() {
        assertEquals("Rp 150.000", Formatters.formatRupiah(150_000L))
        assertEquals("Rp 0", Formatters.formatRupiah(0L))
        assertEquals("-Rp 25.000", Formatters.formatRupiah(-25_000L))
    }

    @Test
    fun `AppThemeMode cycles and deserializes safely`() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.SYSTEM.nextMode())
        assertEquals(AppThemeMode.DARK, AppThemeMode.LIGHT.nextMode())
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.DARK.nextMode())

        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromCode("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromCode("dark"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromCode("system"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromCode(null))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromCode("unknown"))
    }

    @Test
    fun `BusinessSettings formattedPhones combines one or two CS numbers cleanly`() {
        assertEquals(
            "",
            com.example.data.BusinessSettings(businessPhone = "", businessPhone2 = "").formattedPhones()
        )
        assertEquals(
            "081234567890",
            com.example.data.BusinessSettings(businessPhone = " 081234567890 ", businessPhone2 = "").formattedPhones()
        )
        assertEquals(
            "085712345678",
            com.example.data.BusinessSettings(businessPhone = "", businessPhone2 = "085712345678").formattedPhones()
        )
        assertEquals(
            "081234567890 / 085712345678",
            com.example.data.BusinessSettings(
                businessPhone = "081234567890",
                businessPhone2 = "085712345678",
            ).formattedPhones()
        )
    }

    @Test
    fun `formatPoliteRecipient prepends Bapak Ibu unless honorific already exists`() {
        assertEquals("Bapak/Ibu Budi", com.example.util.ReceiptService.formatPoliteRecipient("Budi"))
        assertEquals("Bapak/Ibu", com.example.util.ReceiptService.formatPoliteRecipient("   "))
        assertEquals("Bapak Budi", com.example.util.ReceiptService.formatPoliteRecipient("Bapak Budi"))
        assertEquals("Ibu Siti", com.example.util.ReceiptService.formatPoliteRecipient("Ibu Siti"))
        assertEquals("Pak Joko", com.example.util.ReceiptService.formatPoliteRecipient("Pak Joko"))
        assertEquals("Bpk. Andi", com.example.util.ReceiptService.formatPoliteRecipient("Bpk. Andi"))
        assertEquals("Bapak/Ibu Rina", com.example.util.ReceiptService.formatPoliteRecipient("Bapak/Ibu Rina"))
        assertEquals("Bapak/Ibu Putra", com.example.util.ReceiptService.formatPoliteRecipient("Putra"))
        assertEquals("Bapak/Ibu Budiman", com.example.util.ReceiptService.formatPoliteRecipient("Budiman"))
    }
}
