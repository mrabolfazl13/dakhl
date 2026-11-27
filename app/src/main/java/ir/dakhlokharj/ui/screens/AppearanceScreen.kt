package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.domain.model.ThemeMode
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.themeModeLabel
import ir.dakhlokharj.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    DkScreen(title = "ظاهر برنامه", subtitle = "رنگ، حالت نمایش و واحد پول", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            SectionHeader("حالت نمایش")
            ThemeMode.entries.forEach { mode ->
                val selected = settings.themeMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (selected) colors.primary.copy(alpha = 0.5f) else colors.outline.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.medium,
                        )
                        .clickable { scope.launch { viewModel.setTheme(mode) } }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (mode) {
                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                            else -> Icons.Outlined.SettingsBrightness
                        },
                        contentDescription = null,
                        tint = if (selected) colors.primary else colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(themeModeLabel(mode), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    RadioButton(selected = selected, onClick = { scope.launch { viewModel.setTheme(mode) } })
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader("واحد پول")
            DkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("واحد نمایش مبالغ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "همه محاسبات در ریال انجام می‌شود؛ این گزینه فقط نمایش را تغییر می‌دهد.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                SegmentedControl(
                    listOf("تومان", "ریال"),
                    if (settings.currencyUnit == CurrencyUnit.TOMAN) 0 else 1,
                ) { index ->
                    scope.launch {
                        viewModel.setCurrency(if (index == 0) CurrencyUnit.TOMAN else CurrencyUnit.RIAL)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader("حریم صفحه")
            DkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("پنهان‌کردن موجودی", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "مبالغ صفحه اصلی با نقطه نمایش داده می‌شوند.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.hideBalances,
                        onCheckedChange = { scope.launch { viewModel.setHideBalances(it) } },
                    )
                }
                DkTopDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("محافظت از اسکرین‌شات", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "در اندروید از عکس‌برداری و ضبط صفحه جلوگیری می‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.screenshotProtection,
                        onCheckedChange = { scope.launch { viewModel.setScreenshotProtection(it) } },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "فونت Vazirmatn • طراحی راست‌به‌چپ • حالت ${themeModeLabel(settings.themeMode)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
