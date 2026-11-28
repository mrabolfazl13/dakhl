package ir.dakhlokharj.data.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import ir.dakhlokharj.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data class Available(val info: AppUpdateInfo, val mandatory: Boolean) : AppUpdateState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : AppUpdateState
    data object ReadyToInstall : AppUpdateState
    data class Message(val text: String, val showStore: Boolean = false) : AppUpdateState
    data class Failed(val text: String) : AppUpdateState
}

/**
 * به‌روزرسانی درون‌برنامه‌ای از طریق Google Play.
 *
 * این مسیر به مجوز اینترنت نیاز ندارد؛ دانلود و نصب را خود Play Store انجام می‌دهد.
 * برای نصب‌های خارج از Play، دکمه فروشگاه به بازار/Play/مرورگر هدایت می‌کند.
 */
class AppUpdateRepository(context: Context) {

    private val appContext = context.applicationContext
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)
    private val preferences = appContext.getSharedPreferences("app_update", Context.MODE_PRIVATE)
    private val checking = AtomicBoolean(false)
    private val mutableState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = mutableState.asStateFlow()

    private val listener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADING -> mutableState.value = AppUpdateState.Downloading(
                downloadedBytes = installState.bytesDownloaded().coerceAtLeast(0),
                totalBytes = installState.totalBytesToDownload().coerceAtLeast(0),
            )
            InstallStatus.DOWNLOADED -> mutableState.value = AppUpdateState.ReadyToInstall
            InstallStatus.FAILED -> mutableState.value =
                AppUpdateState.Failed("دریافت به‌روزرسانی کامل نشد. دوباره تلاش کنید.")
            InstallStatus.CANCELED -> mutableState.value = AppUpdateState.Idle
        }
    }

    init {
        manager.registerListener(listener)
    }

    fun check(manual: Boolean) {
        if (state.value is AppUpdateState.Downloading || state.value is AppUpdateState.ReadyToInstall) {
            return
        }
        if (!checking.compareAndSet(false, true)) return
        if (BuildConfig.DEBUG) {
            checking.set(false)
            if (manual) {
                mutableState.value = AppUpdateState.Message(
                    "بررسی به‌روزرسانی در نسخه آزمایشی غیرفعال است. نسخه ریلیز را از فروشگاه به‌روزرسانی کنید.",
                    showStore = true,
                )
            }
            return
        }
        mutableState.value = AppUpdateState.Checking
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                checking.set(false)
                evaluate(info, manual)
            }
            .addOnFailureListener {
                checking.set(false)
                if (manual) {
                    mutableState.value = AppUpdateState.Message(
                        "ارتباط با فروشگاه برقرار نشد. برای بررسی نسخه جدید، فروشگاه را باز کنید.",
                        showStore = true,
                    )
                } else {
                    mutableState.value = AppUpdateState.Idle
                }
            }
    }

    private fun evaluate(info: AppUpdateInfo, manual: Boolean) {
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            mutableState.value = AppUpdateState.ReadyToInstall
            return
        }
        if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
            info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        ) {
            val version = info.availableVersionCode()
            val dismissedVersion = preferences.getInt(KEY_DISMISSED_VERSION, 0)
            if (!manual && dismissedVersion >= version) {
                mutableState.value = AppUpdateState.Idle
                return
            }
            val canUpdate = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ||
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            if (!canUpdate) {
                mutableState.value = if (manual) {
                    AppUpdateState.Message(
                        "این نسخه اجازه به‌روزرسانی درون‌برنامه‌ای را ندارد. برنامه را از فروشگاه به‌روزرسانی کنید.",
                        showStore = true,
                    )
                } else {
                    AppUpdateState.Idle
                }
                return
            }
            mutableState.value = AppUpdateState.Available(
                info = info,
                mandatory = info.updatePriority() >= MANDATORY_PRIORITY,
            )
            return
        }
        mutableState.value = if (manual) {
            AppUpdateState.Message("نسخه جدیدی در دسترس نیست. برنامه به‌روز است.", showStore = true)
        } else {
            AppUpdateState.Idle
        }
    }

    /** شروع جریان به‌روزرسانی؛ Play ادامه کار را بر عهده می‌گیرد. */
    fun start(info: AppUpdateInfo, activity: Activity) {
        val type = when {
            info.updatePriority() >= MANDATORY_PRIORITY &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            else -> {
                mutableState.value = AppUpdateState.Failed("شروع به‌روزرسانی ممکن نشد.")
                return
            }
        }
        val started = runCatching {
            manager.startUpdateFlowForResult(info, type, activity, UPDATE_REQUEST_CODE)
        }.getOrDefault(false)
        if (!started) {
            mutableState.value = AppUpdateState.Failed("شروع به‌روزرسانی ممکن نشد. دوباره تلاش کنید.")
        }
    }

    fun complete() {
        manager.completeUpdate()
            .addOnFailureListener {
                mutableState.value = AppUpdateState.Failed("نصب به‌روزرسانی انجام نشد. دوباره تلاش کنید.")
            }
    }

    fun dismiss(info: AppUpdateInfo) {
        val mandatory = (state.value as? AppUpdateState.Available)?.mandatory == true
        if (!mandatory) {
            preferences.edit().putInt(KEY_DISMISSED_VERSION, info.availableVersionCode()).apply()
            mutableState.value = AppUpdateState.Idle
        }
    }

    fun hide() {
        mutableState.value = AppUpdateState.Idle
    }

    fun openStore(): Boolean {
        val packageName = appContext.packageName
        val intents = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("bazaar://details?id=$packageName"))
                .setPackage(BAZAAR_PACKAGE),
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                .setPackage(PLAY_PACKAGE),
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")),
        )
        for (intent in intents) {
            val opened = runCatching {
                appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (opened) {
                mutableState.value = AppUpdateState.Idle
                return true
            }
        }
        mutableState.value = AppUpdateState.Failed("فروشگاهی برای باز کردن پیدا نشد.")
        return false
    }

    companion object {
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val MANDATORY_PRIORITY = 5
        const val UPDATE_REQUEST_CODE = 7102
        private const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
        private const val PLAY_PACKAGE = "com.android.vending"
    }
}
