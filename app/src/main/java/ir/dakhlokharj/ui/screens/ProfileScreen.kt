package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.moneyShort
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel

@Composable
fun ProfileScreen(
    viewModel: OverviewViewModel,
    onNavigate: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    DkScreen(
        title = "پروفایل",
        subtitle = "حساب کاربری محلی شما",
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 110.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            DkCard(contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("د", style = MaterialTheme.typography.headlineMedium, color = colors.primary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("کاربر دخل‌وخرج", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "بدون حساب آنلاین • کاملاً محلی",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Pill("امن", ir.dakhlokharj.domain.model.InsightTone.POSITIVE)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("حساب‌ها", fa(state.accounts.size), Modifier.weight(1f))
                    StatTile("تراکنش‌ها", fa(state.transactions.size), Modifier.weight(1f))
                    StatTile("اهداف", fa(state.goals.size), Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))
            DkCard(accent = colors.primary) {
                Text("دارایی خالص", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(moneyShort(state.totalBalance), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${fa(state.accounts.count { !it.archived })} حساب فعال • ${fa(state.goals.count { it.achieved })} هدف محقق‌شده",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("دسترسی سریع")
            DkCard(contentPadding = PaddingValues(6.dp)) {
                ProfileMenuRow("تنظیمات", "قفل، یادآوری و داده‌ها", Icons.Outlined.Settings) { onNavigate("settings") }
                DkTopDivider()
                ProfileMenuRow("گزارش‌ها", "تحلیل دوره‌ای و خروجی CSV", Icons.Outlined.BarChart) { onNavigate("reports") }
                DkTopDivider()
                ProfileMenuRow("سلامت مالی", "امتیاز و پیشنهادهای شفاف", Icons.Outlined.Favorite) { onNavigate("health") }
                DkTopDivider()
                ProfileMenuRow("دسته‌بندی‌ها", "شخصی‌سازی ساختار مالی", Icons.Outlined.Category) { onNavigate("categories") }
            }
            Spacer(Modifier.height(10.dp))
            DkCard(contentPadding = PaddingValues(6.dp)) {
                ProfileMenuRow("پشتیبان‌گیری", "نسخه رمزدار و بازیابی", Icons.Outlined.Backup) { onNavigate("backup") }
                DkTopDivider()
                ProfileMenuRow("حریم خصوصی", "بدون اینترنت و بدون سرور", Icons.Outlined.PrivacyTip) { onNavigate("privacy") }
                DkTopDivider()
                ProfileMenuRow("درباره برنامه", "نسخه ۱.۰.۰ و مجوزها", Icons.Outlined.Info) { onNavigate("about") }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "همه داده‌ها روی همین دستگاه ذخیره می‌شوند و به جایی ارسال نمی‌شوند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
