package ir.dakhlokharj.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.CategoryTotal
import ir.dakhlokharj.domain.engine.MonthlyTotal
import ir.dakhlokharj.domain.engine.ProjectionPoint
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.moneyShort
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CashFlowChart(months: List<MonthlyTotal>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val maxValue = max(
        1L,
        months.maxOfOrNull { max(it.income.rial, it.expense.rial) } ?: 1L,
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            months.forEach { month ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        BarSegment(month.income.rial / maxValue.toFloat(), colors.primary)
                        BarSegment(month.expense.rial / maxValue.toFloat(), colors.secondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = month.month.monthName.take(4),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(colors.primary, "درآمد")
            LegendDot(colors.secondary, "هزینه")
        }
    }
}

@Composable
private fun BarSegment(fraction: Float, color: Color) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "bar",
    )
    Box(
        modifier = Modifier
            .width(10.dp)
            .fillMaxHeight(animated.coerceAtLeast(0.02f))
            .background(color, RoundedCornerShape(50)),
    )
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(50)),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ProjectionChart(
    points: List<ProjectionPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (points.isEmpty()) return
    val minValue = points.minOf { it.balance.rial }.toFloat()
    val maxValue = points.maxOf { it.balance.rial }.toFloat()
    val range = max(1f, maxValue - minValue)
    val path = Path()
    val fillPath = Path()
    val colors = MaterialTheme.colorScheme
    Canvas(modifier = modifier.height(150.dp)) {
        val stepX = size.width / max(1, points.size - 1)
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val normalized = (point.balance.rial - minValue) / range
            val y = size.height - (normalized * (size.height - 16.dp.toPx())) - 8.dp.toPx()
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.0f)),
            ),
        )
        drawPath(
            path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val normalized = (point.balance.rial - minValue) / range
            val y = size.height - (normalized * (size.height - 16.dp.toPx())) - 8.dp.toPx()
            drawCircle(color = colors.surface, radius = 5.dp.toPx(), center = Offset(x, y))
            drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun DonutChart(
    slices: List<CategoryTotal>,
    centerLabel: String,
    centerValue: String,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            var start = -90f
            slices.take(8).forEach { slice ->
                val sweep = (slice.share * 360f - 3f).coerceAtLeast(1f)
                drawArc(
                    color = Color(slice.colorArgb),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                start += slice.share * 360f
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HealthRing(score: Int, modifier: Modifier = Modifier, size: Int = 132) {
    val animated by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "health",
    )
    val color = when {
        score >= 75 -> MaterialTheme.colorScheme.primary
        score >= 50 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            drawArc(
                color = MaterialTheme.colorScheme.surfaceVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = fa(score),
                style = MaterialTheme.typography.displaySmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "از ۱۰۰",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DkProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color? = null, height: Int = 8) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "progress",
    )
    val barColor = color ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(barColor.copy(alpha = 0.75f), barColor),
                    ),
                    RoundedCornerShape(50),
                ),
        )
    }
}

@Composable
fun MiniBars(values: List<Money>, modifier: Modifier = Modifier) {
    val max = max(1L, values.maxOfOrNull { it.rial } ?: 1L)
    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { value ->
            val fraction = (value.rial.toFloat() / max).coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight(fraction)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f + fraction * 0.5f),
                        RoundedCornerShape(4.dp),
                    ),
            )
        }
    }
}
