package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.dakhlokharj.ui.components.DkCard
import ir.dakhlokharj.ui.components.DkScreen
import ir.dakhlokharj.ui.components.SectionHeader
import ir.dakhlokharj.ui.components.SecondaryButton

@Composable
fun PrivacyScreen(onBack: () -> Unit, onOpenBackup: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    DkScreen(title = "حریم خصوصی", subtitle = "داده‌های شما فقط مال شماست", onBack = onBack) { padding ->
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
            PrivacyCard(
                Icons.Outlined.WifiOff,
                "بدون اینترنت",
                "برنامه هیچ مجوز اینترنتی ندارد و نمی‌تواند داده‌ای را به بیرون ارسال کند. هیچ تحلیلگر، تبلیغ یا سروری وجود ندارد.",
            )
            PrivacyCard(
                Icons.Outlined.Lock,
                "ذخیره‌سازی محلی",
                "همه تراکنش‌ها، بودجه‌ها و اهداف در پایگاه داده رمزنگاری‌نشده اما خصوصی دستگاه (Room) نگهداری می‌شوند و از آن خارج نمی‌شوند.",
            )
            PrivacyCard(
                Icons.Outlined.VisibilityOff,
                "محافظت از صفحه",
                "به‌صورت پیش‌فرض از گرفتن اسکرین‌شات و ضبط صفحه جلوگیری می‌شود. این گزینه در تنظیمات ظاهر قابل تغییر است.",
            )
            PrivacyCard(
                Icons.Outlined.Backup,
                "پشتیبان رمزدار",
                "نسخه پشتیبان با AES-256 و کلید مشتق‌شده از رمز شما رمزنگاری می‌شود. بدون رمز، محتوای فایل قابل خواندن نیست.",
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "این برنامه برای استفاده شخصی طراحی شده و هیچ داده‌ای را با شخص ثالث به اشتراک نمی‌گذارد.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton("تنظیمات امنیتی") { onOpenSettings() }
            SecondaryButton("پشتیبان‌گیری") { onOpenBackup() }
        }
    }
}

@Composable
private fun PrivacyCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    DkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
