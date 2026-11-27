package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.domain.model.InsightTone
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel

@Composable
fun HealthScreen(viewModel: OverviewViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val health = state.health
    val colors = MaterialTheme.colorScheme

    DkScreen(
        title = "سلامت مالی",
        subtitle = "امتیاز شفاف و قابل توضیح",
        onBack = onBack,
    ) { padding ->
        if (health == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@DkScreen
        }
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
            item {
                DkCard(accent = colors.primary) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        HealthRing(score = health.score, size = 160)
                        Spacer(Modifier.height(10.dp))
                        Text(health.grade, style = MaterialTheme.typography.headlineMedium, color = colors.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            health.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            item { SectionHeader("اجزای امتیاز") }
            items(health.components, key = { it.key }) { component ->
                DkCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(component.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                component.explanation,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Text(
                            "${fa(component.score)} / ${fa(component.maxScore)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ToneColor(component.tone),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    DkProgressBar(
                        progress = if (component.maxScore > 0) component.score.toFloat() / component.maxScore else 0f,
                        color = ToneColor(component.tone),
                    )
                }
            }

            if (health.positives.isNotEmpty()) {
                item { SectionHeader("نقاط قوت") }
                items(health.positives, key = { it }) { text ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (health.improvements.isNotEmpty()) {
                item { SectionHeader("پیشنهاد بهبود") }
                items(health.improvements, key = { it }) { text ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "امتیاز از داده‌های واقعی و بدون تصادف محاسبه می‌شود؛ هر جزء سهم مشخصی دارد.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}