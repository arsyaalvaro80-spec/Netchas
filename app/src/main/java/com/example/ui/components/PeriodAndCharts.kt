package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.netKasSemanticColors
import com.example.util.Formatters
import com.example.viewmodel.MonthlyTrendPoint
import kotlin.math.max

@Composable
fun MonthPeriodBar(
    selectedMonth: String,
    isHistoricalMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPicker: () -> Unit,
    onJumpCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.testTag("prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Bulan sebelumnya",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenPicker)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("month_picker_trigger"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Formatters.formatYearMonthFull(selectedMonth),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("next_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Bulan berikutnya",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (isHistoricalMonth) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.historical_mode_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onJumpCurrent)
                            .testTag("jump_current_month_button"),
                    ) {
                        Text(
                            text = "Bulan Ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SixMonthPerformanceChart(
    points: List<MonthlyTrendPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    var selectedIdx by remember(points) { mutableIntStateOf(points.lastIndex) }
    val selected = points.getOrElse(selectedIdx) { points.last() }

    val semantic = netKasSemanticColors()
    val cashColor = semantic.cash
    val bankColor = semantic.bank
    val totalLineColor = semantic.income
    val dotHaloColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected month readout card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = Formatters.formatYearMonthFull(selected.yearMonth),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Total: ${Formatters.formatRupiah(selected.income)}",
                        style = MonoSmallStyle,
                        fontWeight = FontWeight.Bold,
                        color = totalLineColor,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tunai: ${Formatters.formatRupiahCompact(selected.cashIncome)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = cashColor,
                    )
                    Text(
                        text = "Bank: ${Formatters.formatRupiahCompact(selected.bankIncome)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = bankColor,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Canvas Grouped Bar (Cash vs Bank) + Total Income Line
        val maxVal = max(
            1L,
            points.maxOf { max(it.income, max(it.cashIncome, it.bankIncome)) }
        ).toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val chartHeight = size.height - 12.dp.toPx()
            val slotWidth = size.width / points.size
            val barWidth = (slotWidth * 0.28f).coerceAtMost(24.dp.toPx())
            val gap = 4.dp.toPx()

            // Horizontal reference lines
            for (i in 0..3) {
                val y = chartHeight * (i / 3f) + 6.dp.toPx()
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val totalPath = Path()
            val totalDots = mutableListOf<Offset>()

            points.forEachIndexed { idx, pt ->
                val centerX = slotWidth * idx + slotWidth / 2f
                val cashHeight = (pt.cashIncome / maxVal) * (chartHeight - 10.dp.toPx())
                val bankHeight = (pt.bankIncome / maxVal) * (chartHeight - 10.dp.toPx())
                val baseY = chartHeight + 6.dp.toPx()

                // Cash Bar
                drawRoundRect(
                    color = if (idx == selectedIdx) cashColor else cashColor.copy(alpha = 0.65f),
                    topLeft = Offset(centerX - barWidth - gap / 2f, baseY - cashHeight),
                    size = Size(barWidth, cashHeight.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )

                // Bank Bar
                drawRoundRect(
                    color = if (idx == selectedIdx) bankColor else bankColor.copy(alpha = 0.65f),
                    topLeft = Offset(centerX + gap / 2f, baseY - bankHeight),
                    size = Size(barWidth, bankHeight.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )

                val totalY = baseY - (pt.income.coerceAtLeast(0L) / maxVal) * (chartHeight - 10.dp.toPx())
                val dotOffset = Offset(centerX, totalY)
                totalDots.add(dotOffset)
                if (idx == 0) totalPath.moveTo(dotOffset.x, dotOffset.y)
                else totalPath.lineTo(dotOffset.x, dotOffset.y)
            }

            // Total Income Trend Line
            drawPath(
                path = totalPath,
                color = totalLineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
            totalDots.forEachIndexed { idx, pt ->
                drawCircle(
                    color = dotHaloColor,
                    radius = if (idx == selectedIdx) 5.5.dp.toPx() else 4.dp.toPx(),
                    center = pt,
                )
                drawCircle(
                    color = totalLineColor,
                    radius = if (idx == selectedIdx) 3.8.dp.toPx() else 2.5.dp.toPx(),
                    center = pt,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Interactive Month Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            points.forEachIndexed { idx, pt ->
                val isSel = idx == selectedIdx
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIdx = idx },
                ) {
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            ChartLegendDot(color = cashColor, label = "Tunai")
            ChartLegendDot(color = bankColor, label = "Transfer Bank")
            ChartLegendDot(color = totalLineColor, label = "Total Masuk")
        }
    }
}

@Composable
fun WalletSplitBar(
    cashAmount: Long,
    bankAmount: Long,
    modifier: Modifier = Modifier,
) {
    val semantic = netKasSemanticColors()
    val total = (cashAmount + bankAmount).coerceAtLeast(1L)
    val cashFraction = if (cashAmount + bankAmount == 0L) 0.5f else (cashAmount.toFloat() / total.toFloat())
    val bankFraction = 1f - cashFraction

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Tunai ${(cashFraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = semantic.cash,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Transfer Bank ${(bankFraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = semantic.bank,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (cashFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(cashFraction.coerceAtLeast(0.01f))
                        .height(10.dp)
                        .background(semantic.cash)
                )
            }
            if (bankFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(bankFraction.coerceAtLeast(0.01f))
                        .height(10.dp)
                        .background(semantic.bank)
                )
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
