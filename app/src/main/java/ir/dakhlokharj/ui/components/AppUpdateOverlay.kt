package ir.dakhlokharj.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.BuildConfig
import ir.dakhlokharj.data.update.AppUpdateRepository
import ir.dakhlokharj.data.update.AppUpdateState
import ir.dakhlokharj.ui.theme.fa

@Composable
fun AppUpdateOverlay(
    repository: AppUpdateRepository,
    activity: Activity,
) {
    val state by repository.state.collectAsStateWithLifecycle()

    LaunchedEffect(repository) {
        repository.check(manual = false)
    }

    when (val current = state) {
        AppUpdateState.Idle, AppUpdateState.Checking -> Unit
        is AppUpdateState.Available -> AlertDialog(
            onDismissRequest = { repository.dismiss(current.info) },
            icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
            title = { Text("به‌روزرسانی جدید") },
            text = {
                Column {
                    Text(
                        "نسخه تازه‌ای از دخل‌وخرج آماده است. با به‌روزرسانی، بهبودها و رفع اشکال‌ها را دریافت می‌کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "نسخه فعلی: ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { repository.start(current.info, activity) }) {
                    Text("دریافت به‌روزرسانی")
                }
            },
            dismissButton = if (current.mandatory) {
                null
            } else {
                {
                    TextButton(onClick = { repository.dismiss(current.info) }) { Text("بعداً") }
                }
            },
        )
        is AppUpdateState.Downloading -> {
            val total = current.totalBytes
            val downloaded = current.downloadedBytes
            AlertDialog(
                onDismissRequest = {},
                icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                title = { Text("در حال دریافت به‌روزرسانی") },
                text = {
                    Column {
                        Text(
                            "نسخه جدید در حال دانلود است. پس از پایان، نصب را تأیید کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(14.dp))
                        if (total > 0) {
                            LinearProgressIndicator(
                                progress = { (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (total > 0) {
                                "${fa(downloaded / MB)} از ${fa(total / MB)} مگابایت"
                            } else {
                                "${fa(downloaded / MB)} مگابایت دریافت شد"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { repository.hide() }) { Text("ادامه در پس‌زمینه") }
                },
            )
        }
        AppUpdateState.ReadyToInstall -> AlertDialog(
            onDismissRequest = { repository.hide() },
            icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
            title = { Text("به‌روزرسانی آماده نصب است") },
            text = {
                Text(
                    "دریافت نسخه جدید کامل شد. برای نصب، برنامه یک‌بار بسته و باز می‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { repository.complete() }) { Text("نصب و راه‌اندازی") }
            },
            dismissButton = {
                TextButton(onClick = { repository.hide() }) { Text("بعداً") }
            },
        )
        is AppUpdateState.Message -> AlertDialog(
            onDismissRequest = { repository.hide() },
            icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
            title = { Text("به‌روزرسانی برنامه") },
            text = { Text(current.text, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { repository.hide() }) { Text("باشه") }
            },
            dismissButton = if (current.showStore) {
                {
                    TextButton(onClick = { repository.openStore() }) { Text("باز کردن فروشگاه") }
                }
            } else {
                null
            },
        )
        is AppUpdateState.Failed -> AlertDialog(
            onDismissRequest = { repository.hide() },
            icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
            title = { Text("به‌روزرسانی انجام نشد") },
            text = { Text(current.text, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { repository.check(manual = true) }) { Text("تلاش دوباره") }
            },
            dismissButton = {
                TextButton(onClick = { repository.hide() }) { Text("بستن") }
            },
        )
    }
}

private const val MB = 1024L * 1024L
