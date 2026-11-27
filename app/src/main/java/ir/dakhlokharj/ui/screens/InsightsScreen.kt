package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel

@Composable
fun InsightsScreen(viewModel: OverviewViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    DkScreen(
        title = "بینش‌های مالی",
        subtitle = "الگوهای واقعی خرج‌کردن شما",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 6.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.insights.isEmpty()) {
                item {
                    EmptyState(
                        title = "هنوز داده کافی نیست",
                        description = "چند تراکنش در دسته‌های مختلف ثبت کنید تا الگوها و پیشنهادها ساخته شوند.",
                        icon = Icons.Outlined.Insights,
                    )
                }
            } else {
                items(state.insights, key = { it.title + it.description }) { insight ->
                    DkCard(accent = ToneColor(insight.tone).copy(alpha = 0.6f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        ToneColor(insight.tone).copy(alpha = 0.14f),
                                        MaterialTheme.shapes.medium,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    iconFor(insight.iconKey),
                                    contentDescription = null,
                                    tint = ToneColor(insight.tone),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(insight.title, style = MaterialTheme.typography.titleSmall)
                                if (insight.amount != null) {
                                    Text(moneyShort(insight.amount), style = MaterialTheme.typography.labelSmall, color = ToneColor(insight.tone))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            insight.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            item { SectionHeader("الگوی هزینه در روزهای هفته") }
            item {
                DkCard {
                    val max = state.weekdayTotals.maxOfOrNull { it.amount.rial } ?: 1L
                    state.weekdayTotals.forEach { day ->
                        Row(
                            modifier = Modifier.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                day.label,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(64.dp),
                                color = colors.onSurfaceVariant,
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                DkProgressBar(
                                    progress = if (max > 0) day.amount.rial.toFloat() / max else 0f,
                                    color = colors.secondary,
                                    height = 6,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(moneyShort(day.amount), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}