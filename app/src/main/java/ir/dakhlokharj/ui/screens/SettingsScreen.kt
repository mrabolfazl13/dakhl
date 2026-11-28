package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.domain.model.ThemeMode
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.currencyUnitLabel
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.moneyText
import ir.dakhlokharj.ui.themeModeLabel
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import ir.dakhlokharj.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private enum class PinStage { NEW, CURRENT_FOR_CHANGE, CURRENT_FOR_DISABLE }

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    planning: PlanningViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onCheckUpdates: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accounts by planning.accounts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var pinStage by remember { mutableStateOf<PinStage?>(null) }
    var showDefaultAccount by remember { mutableStateOf(false) }
    var showHourPicker by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var currentPinForNew by remember { mutableStateOf<String?>(null) }

    DkScreen(
        title = "تنظیمات",
        subtitle = "برنامه را مطابق سلیقه خود تنظیم کنید",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            if (message != null) {
                Text(message!!, color = colors.primary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            SectionHeader("ظاهر و واحد پول")
            DkCard(contentPadding = PaddingValues(6.dp)) {
                SettingsRow("ظاهر برنامه", themeModeLabel(settings.themeMode), Icons.Outlined.Palette) {
                    onNavigate("appearance")
                }
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Wallet, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("واحد پول", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    SegmentedControl(
                        options = listOf("تومان", "ریال"),
                        selectedIndex = if (settings.currencyUnit == CurrencyUnit.TOMAN) 0 else 1,
                        modifier = Modifier.width(150.dp),
                    ) { index ->
                        scope.launch {
                            viewModel.setCurrency(if (index == 0) CurrencyUnit.TOMAN else CurrencyUnit.RIAL)
                        }
                    }
                }
                SettingsToggleRow(
                    "پنهان‌کردن موجودی",
                    "مبالغ در صفحه اصلی پنهان می‌شوند",
                    Icons.Outlined.Security,
                    settings.hideBalances,
                ) { hidden -> scope.launch { viewModel.setHideBalances(hidden) } }
            }

            Spacer(Modifier.height(10.dp))
            SectionHeader("یادآوری‌ها")
            DkCard(contentPadding = PaddingValues(6.dp)) {
                SettingsToggleRow(
                    "یادآوری روزانه",
                    "هشدار سررسید اقساط، بدهی و بودجه",
                    Icons.Outlined.Notifications,
                    settings.reminderEnabled,
                ) { enabled -> scope.launch { viewModel.setReminder(enabled, settings.dailyReminderHour, settings.dailyReminderMinute) } }
                if (settings.reminderEnabled) {
                    DkTopDivider()
                    SettingsRow(
                        "ساعت یادآوری",
                        "${fa(settings.dailyReminderHour)}:${fa(settings.dailyReminderMinute)}",
                        Icons.Outlined.Notifications,
                    ) { showHourPicker = true }
                }
            }

            Spacer(Modifier.height(10.dp))
            SectionHeader("امنیت")
            DkCard(contentPadding = PaddingValues(6.dp)) {
                SettingsToggleRow(
                    "قفل برنامه",
                    if (settings.pinHash == null) "برای فعال‌سازی، ابتدا رمز تعیین کنید" else "ورود با رمز عددی",
                    Icons.Outlined.Lock,
                    settings.appLockEnabled,
                ) { enabled ->
                    if (enabled) {
                        if (settings.pinHash == null) {
                            currentPinForNew = null
                            pinStage = PinStage.NEW
                        } else {
                            scope.launch { viewModel.setAppLock(true) }
                        }
                    } else {
                        pinStage = PinStage.CURRENT_FOR_DISABLE
                    }
                }
                DkTopDivider()
                SettingsRow(
                    if (settings.pinHash == null) "تعیین رمز برنامه" else "تغییر رمز برنامه",
                    if (settings.pinHash == null) "رمز ۴ تا ۸ رقمی" else "برای تغییر، رمز فعلی را وارد کنید",
                    Icons.Outlined.Security,
                ) {
                    if (settings.pinHash == null) {
                        currentPinForNew = null
                        pinStage = PinStage.NEW
                    } else {
                        pinStage = PinStage.CURRENT_FOR_CHANGE
                    }
                }
                if (settings.pinHash != null) {
                    DkTopDivider()
                    SettingsToggleRow(
                        "ورود با اثر انگشت",
                        "در صفحه قفل نمایش داده می‌شود",
                        Icons.Outlined.Security,
                        settings.biometricEnabled,
                    ) { enabled -> scope.launch { viewModel.setBiometric(enabled) } }
                }
                DkTopDivider()
                SettingsToggleRow(
                    "محافظت از اسکرین‌شات",
                    "جلوگیری از عکس‌برداری صفحه",
                    Icons.Outlined.PrivacyTip,
                    settings.screenshotProtection,
                ) { enabled -> scope.launch { viewModel.setScreenshotProtection(enabled) } }
            }

            Spacer(Modifier.height(10.dp))
            SectionHeader("داده‌ها")
            DkCard(contentPadding = PaddingValues(6.dp)) {
                DkTopDivider()
                SettingsRow("دسته‌بندی‌ها", "مدیریت دسته‌های هزینه و درآمد", Icons.Outlined.Category) {
                    onNavigate("categories")
                }
                DkTopDivider()
                SettingsRow("پشتیبان‌گیری و بازیابی", "فایل رمزدار یا JSON", Icons.Outlined.Backup) {
                    onNavigate("backup")
                }
                DkTopDivider()
                SettingsRow(
                    "حساب پیش‌فرض",
                    accounts.firstOrNull { it.id == settings.defaultAccountId }?.name ?: "انتخاب خودکار",
                    Icons.Outlined.Wallet,
                ) { showDefaultAccount = true }
            }

            Spacer(Modifier.height(10.dp))
            SectionHeader("به‌روزرسانی")
            DkCard(contentPadding = PaddingValues(6.dp)) {
                SettingsRow(
                    "به‌روزرسانی خودکار",
                    "بررسی نسخه جدید هنگام اجرا از طریق فروشگاه",
                    Icons.Outlined.SystemUpdate,
                ) { onCheckUpdates() }
            }

            Spacer(Modifier.height(10.dp))
            SectionHeader("درباره و حریم خصوصی")
            DkCard(contentPadding = PaddingValues(6.dp)) {
                SettingsRow("حریم خصوصی", "آفلاین، بدون اینترنت و بدون حساب", Icons.Outlined.PrivacyTip) {
                    onNavigate("privacy")
                }
                DkTopDivider()
                SettingsRow("درباره برنامه", "نسخه ۱.۰.۰ • مجوز فونت", Icons.Outlined.Info) {
                    onNavigate("about")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "دخل‌وخرج • نسخه ۱.۰.۰ • همه داده‌ها روی این دستگاه",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    fun applyNewPin(password: String) {
        scope.launch {
            viewModel.changePin(currentPinForNew, password)
                .onSuccess {
                    message = "رمز با موفقیت تنظیم شد"
                    pinStage = null
                    currentPinForNew = null
                }
                .onFailure {
                    message = it.message
                    pinStage = null
                }
        }
    }

    when (pinStage) {
        PinStage.CURRENT_FOR_CHANGE -> TextPromptDialog(
            title = "رمز فعلی",
            description = "برای تغییر رمز، ابتدا رمز فعلی را وارد کنید.",
            label = "رمز فعلی",
            password = true,
            confirmLabel = "ادامه",
            onDismiss = { pinStage = null },
            onConfirm = { current ->
                currentPinForNew = current
                pinStage = PinStage.NEW
            },
        )
        PinStage.CURRENT_FOR_DISABLE -> TextPromptDialog(
            title = "غیرفعال‌سازی قفل",
            description = "برای خاموش‌کردن قفل برنامه، رمز فعلی را وارد کنید.",
            label = "رمز فعلی",
            password = true,
            confirmLabel = "غیرفعال‌سازی",
            onDismiss = { pinStage = null },
            onConfirm = { current ->
                scope.launch {
                    viewModel.disableLock(current)
                        .onSuccess { message = "قفل برنامه غیرفعال شد"; pinStage = null }
                        .onFailure { message = it.message; pinStage = null }
                }
            },
        )
        PinStage.NEW -> TextPromptDialog(
            title = "رمز جدید",
            description = "رمز ۴ تا ۸ رقمی انتخاب کنید. این رمز فقط روی همین دستگاه نگهداری می‌شود.",
            label = "رمز جدید",
            password = true,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
            confirmLabel = "ذخیره رمز",
            onDismiss = { pinStage = null },
            onConfirm = { pin -> applyNewPin(pin) },
        )
        null -> Unit
    }

    if (showDefaultAccount) {
        OptionPickerSheet(
            title = "حساب پیش‌فرض",
            options = listOf(SheetOption(0L, "انتخاب خودکار")) + accounts.map {
                SheetOption(it.id, it.name, moneyText(it.balance))
            },
            selectedId = settings.defaultAccountId,
            onDismiss = { showDefaultAccount = false },
            onSelect = { id ->
                scope.launch { viewModel.setDefaultAccount(if (id == 0L) null else id) }
            },
        )
    }
    if (showHourPicker) {
        OptionPickerSheet(
            title = "ساعت یادآوری",
            options = (6..23).map { SheetOption(it.toLong(), "${fa(it)}:۰۰") },
            selectedId = settings.dailyReminderHour.toLong(),
            onDismiss = { showHourPicker = false },
            onSelect = { hour ->
                scope.launch { viewModel.setReminder(settings.reminderEnabled, hour.toInt(), 0) }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}