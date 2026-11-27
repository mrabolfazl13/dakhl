package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.dakhlokharj.ui.components.DkCard
import ir.dakhlokharj.ui.components.DkScreen
import ir.dakhlokharj.ui.components.InfoRow
import ir.dakhlokharj.ui.components.SectionHeader
import ir.dakhlokharj.ui.theme.fa

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    DkScreen(title = "درباره برنامه", subtitle = "دخل‌وخرج، همراه مالی آفلاین", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(colors.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("د", style = MaterialTheme.typography.displayMedium, color = colors.primary)
            }
            Spacer(Modifier.height(10.dp))
            Text("دخل‌وخرج", style = MaterialTheme.typography.headlineMedium)
            Text("مدیریت مالی شخصی، کاملاً آفلاین", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("نسخه ۱.۰.۰", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            DkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.OfflineBolt, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("آفلاین واقعی", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "هیچ سرور، حساب کاربری یا مجوز اینترنتی وجود ندارد. همه داده‌ها فقط روی همین دستگاه ذخیره می‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            DkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("امنیت", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "قفل برنامه با رمز و اثر انگشت، محافظت از اسکرین‌شات و پشتیبان رمزنگاری‌شده AES-256.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            DkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Code, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("فونت و منابع", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "فونت Vazirmatn تحت مجوز SIL Open Font License 1.1 استفاده شده است.\n" +
                        "تقویم جلالی با پیاده‌سازی بومی و بدون وابستگی خارجی محاسبه می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            DkCard {
                Text("امکانات", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "• ثبت درآمد، هزینه، انتقال و بودجه\n" +
                        "• پیش‌بینی پایان ماه و مشاور خرید\n" +
                        "• امتیاز سلامت مالی با توضیح شفاف\n" +
                        "• اقساط، بدهی، طلب، اشتراک و پس‌انداز\n" +
                        "• تقویم مالی و گزارش‌های دوره‌ای\n" +
                        "• خروجی CSV و PDF و پشتیبان کامل",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            DkCard {
                InfoRow("نسخه پایگاه داده", fa(1))
                InfoRow("حداقل اندروید", "۸")
                InfoRow("واحد پول پیش‌فرض", "تومان")
                InfoRow("مجوز اینترنت", "ندارد")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "ساخته‌شده برای مدیریت آرام‌تر پول",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}