package ir.dakhlokharj.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val history by viewModel.backupHistory.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var pendingBackupUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showBackupPassword by remember { mutableStateOf(false) }
    var showRestorePassword by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun runBackup(uri: Uri, password: String?) {
        scope.launch {
            message = null
            error = null
            viewModel.createBackup(uri, password)
                .onSuccess { message = "نسخه پشتیبان با موفقیت ساخته شد." }
                .onFailure { error = it.message ?: "ساخت نسخه پشتیبان ناموفق بود" }
        }
    }

    fun openRestore(uri: Uri) {
        pendingRestoreUri = uri
        showRestoreConfirm = true
    }

    fun runRestore(uri: Uri, password: String?) {
        scope.launch {
            message = null
            error = null
            viewModel.restoreBackup(uri, password)
                .onSuccess { count -> message = "بازیابی انجام شد؛ ${fa(count)} تراکنش بازگردانی شد." }
                .onFailure { error = it.message ?: "بازیابی ناموفق بود" }
        }
    }

    val encryptedBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            pendingBackupUri = uri
            showBackupPassword = true
        }
    }
    val plainBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) runBackup(uri, null)
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) openRestore(uri)
    }

    DkScreen(
        title = "پشتیبان‌گیری",
        subtitle = "ذخیره امن و بازیابی کامل داده‌ها",
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
            DkCard(accent = colors.primary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("رمزنگاری AES-256", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "نسخه رمزدار با PBKDF2 محافظت می‌شود و فقط با رمز شما باز می‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ساخت نسخه رمزدار", icon = Icons.Outlined.CloudUpload, enabled = !busy) {
                encryptedBackupLauncher.launch("dakhlo-kharj-${System.currentTimeMillis()}.dkbackup")
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton("ساخت نسخه بدون رمز", icon = Icons.Outlined.Upload, enabled = !busy) {
                plainBackupLauncher.launch("dakhlo-kharj-plain-${System.currentTimeMillis()}.json")
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton("بازیابی از فایل", icon = Icons.Outlined.Download, enabled = !busy) {
                restoreLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
            }
            if (busy) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (message != null) {
                Spacer(Modifier.height(10.dp))
                Text(message!!, color = colors.primary, style = MaterialTheme.typography.bodySmall)
            }
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("تاریخچه نسخه‌ها")
            if (history.isEmpty()) {
                Text(
                    "هنوز نسخه پشتیبانی ساخته نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                history.forEach { meta ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (meta.encrypted) Icons.Outlined.Lock else Icons.Outlined.Info,
                            contentDescription = null,
                            tint = if (meta.encrypted) colors.primary else colors.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meta.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(
                                SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(meta.createdAt)) +
                                    " • " + (meta.sizeBytes / 1024).toString() + " کیلوبایت",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            DkCard {
                Text("نکته‌های مهم", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "• فایل پشتیبان را در جای امن نگه دارید.\n" +
                        "• بازیابی، داده‌های فعلی را با نسخه پشتیبان جایگزین می‌کند.\n" +
                        "• اگر رمز را فراموش کنید، بازیابی نسخه رمزدار ممکن نیست.\n" +
                        "• همه فایل‌ها با انتخاب خود شما در حافظه دستگاه ذخیره می‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }

    if (showBackupPassword && pendingBackupUri != null) {
        TextPromptDialog(
            title = "رمز نسخه پشتیبان",
            description = "یک رمز قوی انتخاب کنید. این رمز جایی ذخیره نمی‌شود و در بازیابی به آن نیاز دارید.",
            label = "رمز (حداقل ۶ کاراکتر)",
            password = true,
            confirmLabel = "ساخت نسخه",
            onDismiss = { showBackupPassword = false },
            onConfirm = { password ->
                if (password.length < 6) {
                    error = "رمز باید حداقل ۶ کاراکتر باشد"
                } else {
                    pendingBackupUri?.let { runBackup(it, password) }
                }
            },
        )
    }

    if (showRestoreConfirm && pendingRestoreUri != null) {
        DkBottomSheet(onDismiss = { showRestoreConfirm = false }) {
            SheetTitle("بازیابی داده‌ها", "نوع نسخه پشتیبان را انتخاب کنید")
            Text(
                "داده‌های فعلی با محتوای نسخه پشتیبان جایگزین می‌شوند. اگر نسخه رمزدار است، رمز را در مرحله بعد وارد کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton("نسخه رمزدار") {
                showRestoreConfirm = false
                showRestorePassword = true
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton("نسخه بدون رمز") {
                showRestoreConfirm = false
                pendingRestoreUri?.let { runRestore(it, null) }
            }
        }
    }

    if (showRestorePassword && pendingRestoreUri != null) {
        TextPromptDialog(
            title = "رمز نسخه پشتیبان",
            description = "رمزی که هنگام ساخت این نسخه انتخاب کرده‌اید را وارد کنید.",
            label = "رمز",
            password = true,
            confirmLabel = "بازیابی",
            onDismiss = { showRestorePassword = false },
            onConfirm = { password -> pendingRestoreUri?.let { runRestore(it, password) } },
        )
    }

}
