package ir.dakhlokharj

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ir.dakhlokharj.di.AppContainer
import ir.dakhlokharj.domain.model.UserSettings
import ir.dakhlokharj.security.PinHasher
import ir.dakhlokharj.ui.components.AppUpdateOverlay
import ir.dakhlokharj.ui.navigation.DkNavHost
import ir.dakhlokharj.ui.theme.DkTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DakhloKharjApp).container
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsState(initial = UserSettings())
            DkTheme(settings) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot(container = container, settings = settings, activity = this@MainActivity)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    container: AppContainer,
    settings: UserSettings,
    activity: FragmentActivity,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var unlocked by remember { mutableStateOf(false) }
    var lockError by remember { mutableStateOf<String?>(null) }
    var biometricActive by remember { mutableStateOf(false) }

    val pinConfigured = settings.pinHash != null
    val locked = settings.appLockEnabled && pinConfigured && !unlocked
    val enabledState = rememberUpdatedState(settings.appLockEnabled)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && enabledState.value && !biometricActive) {
                unlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SideEffect {
        if (settings.screenshotProtection) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (locked) {
        LockScreen(
            error = lockError,
            biometricEnabled = settings.biometricEnabled,
            onSubmit = { pin ->
                scope.launch {
                    val hash = container.settingsRepository.current().pinHash
                    if (hash != null && PinHasher.verify(pin, hash)) {
                        lockError = null
                        unlocked = true
                    } else {
                        lockError = "رمز نادرست است"
                    }
                }
            },
            onBiometric = {
                biometricActive = true
                BiometricHelper.authenticate(
                    activity = activity,
                    onSuccess = {
                        biometricActive = false
                        lockError = null
                        unlocked = true
                    },
                    onFailure = {
                        biometricActive = false
                        lockError = it
                    },
                )
            },
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            DkNavHost(
                container = container,
                onCheckUpdates = { container.appUpdateRepository.check(manual = true) },
            )
            AppUpdateOverlay(repository = container.appUpdateRepository, activity = activity)
        }
    }
}

@Composable
private fun LockScreen(
    error: String?,
    biometricEnabled: Boolean,
    onSubmit: (String) -> Unit,
    onBiometric: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var pin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.background, colors.primary.copy(alpha = 0.08f)),
                ),
            )
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.primary)
            }
            Spacer(Modifier.height(14.dp))
            Text("دخل‌وخرج", style = MaterialTheme.typography.headlineMedium)
            Text(
                "برای دیدن داده‌های مالی، رمز خود را وارد کنید",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(maxOf(4, pin.length)) { index ->
                    val filled = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(if (index < pin.length) 14.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (filled) colors.primary else colors.outline.copy(alpha = 0.35f)),
                    )
                }
            }
            if (pin.length > 4) {
                Spacer(Modifier.height(6.dp))
                Text("${pin.length} رقم وارد شده", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        row.forEach { digit ->
                            KeypadKey(digit, Modifier.weight(1f)) {
                                if (pin.length < 8) pin += digit
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    KeypadKey(
                        label = null,
                        modifier = Modifier.weight(1f),
                        enabled = biometricEnabled,
                        onClick = onBiometric,
                        icon = { Icon(Icons.Outlined.Fingerprint, contentDescription = "اثر انگشت", tint = colors.primary) },
                    )
                    KeypadKey("0", Modifier.weight(1f)) { if (pin.length < 8) pin += "0" }
                    KeypadKey(
                        label = null,
                        modifier = Modifier.weight(1f),
                        onClick = { pin = pin.dropLast(1) },
                        icon = { Icon(Icons.Outlined.Backspace, contentDescription = "حذف", tint = colors.onSurfaceVariant) },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onSubmit(pin) },
                    enabled = pin.length >= 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("ورود", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "همه داده‌ها فقط روی این دستگاه ذخیره می‌شود",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KeypadKey(
    label: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        tonalElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) icon() else Text(
                label ?: "",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
        }
    }
}

private object BiometricHelper {
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure("ورود با اثر انگشت انجام نشد")
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ورود به دخل‌وخرج")
            .setSubtitle("با اثر انگشت یا چهره وارد شوید")
            .setNegativeButtonText("استفاده از رمز")
            .setConfirmationRequired(false)
            .build()
        prompt.authenticate(info)
    }
}