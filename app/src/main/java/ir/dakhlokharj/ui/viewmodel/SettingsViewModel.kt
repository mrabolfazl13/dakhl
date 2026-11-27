package ir.dakhlokharj.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.dakhlokharj.di.AppContainer
import ir.dakhlokharj.domain.model.BackupMetadata
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.domain.model.ThemeMode
import ir.dakhlokharj.domain.model.UserSettings
import ir.dakhlokharj.export.CsvExporter
import ir.dakhlokharj.security.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<UserSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val categories: StateFlow<List<Category>> = container.categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val backupHistory: StateFlow<List<BackupMetadata>> = container.backupRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val busy = MutableStateFlow(false)

    suspend fun setTheme(mode: ThemeMode) = container.settingsRepository.setTheme(mode)

    suspend fun setCurrency(unit: CurrencyUnit) = container.settingsRepository.setCurrency(unit)

    suspend fun setHideBalances(hidden: Boolean) = container.settingsRepository.setHideBalances(hidden)

    suspend fun setScreenshotProtection(enabled: Boolean) =
        container.settingsRepository.setScreenshotProtection(enabled)

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) =
        container.settingsRepository.setReminder(enabled, hour, minute)

    suspend fun setDefaultAccount(id: Long?) = container.settingsRepository.setDefaultAccount(id)

    suspend fun setAppLock(enabled: Boolean) = container.settingsRepository.setAppLock(enabled)

    suspend fun setBiometric(enabled: Boolean) = container.settingsRepository.setBiometric(enabled)

    suspend fun setOnboardingDone(done: Boolean) = container.settingsRepository.setOnboardingDone(done)

    suspend fun changePin(currentPin: String?, newPin: String): Result<Unit> = runCatching {
        require(newPin.length in 4..8 && newPin.all { it.isDigit() }) { "رمز باید ۴ تا ۸ رقم باشد" }
        val stored = container.settingsRepository.current().pinHash
        if (stored != null && !PinHasher.verify(currentPin ?: "", stored)) {
            throw IllegalArgumentException("رمز فعلی نادرست است")
        }
        container.settingsRepository.setPinHash(PinHasher.hash(newPin))
        container.settingsRepository.setAppLock(true)
    }

    suspend fun disableLock(pin: String): Result<Unit> = runCatching {
        val stored = container.settingsRepository.current().pinHash
        require(stored != null && PinHasher.verify(pin, stored)) { "رمز عبور نادرست است" }
        container.settingsRepository.setAppLock(false)
        container.settingsRepository.setBiometric(false)
        container.settingsRepository.setPinHash(null)
    }

    suspend fun verifyPin(pin: String): Boolean = PinHasher.verify(
        pin,
        container.settingsRepository.current().pinHash,
    )

    suspend fun addCategory(category: Category): Result<Unit> = runCatching {
        require(category.name.isNotBlank()) { "نام دسته‌بندی را وارد کنید" }
        val exists = container.categoryRepository.getAll()
            .any { it.name == category.name && it.kind == category.kind }
        require(!exists) { "دسته‌بندی با این نام وجود دارد" }
        container.categoryRepository.add(category)
    }

    suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        require(category.name.isNotBlank()) { "نام دسته‌بندی را وارد کنید" }
        container.categoryRepository.update(category)
    }

    suspend fun deleteCategory(category: Category): Result<Unit> = runCatching {
        require(!category.system) { "دسته‌بندی‌های پیش‌فرض قابل حذف نیستند" }
        val used = container.database.transactionDao().getAll().any { it.categoryId == category.id }
        require(!used) { "این دسته در تراکنش‌ها استفاده شده و قابل حذف نیست" }
        container.categoryRepository.delete(category)
    }

    suspend fun createBackup(uri: Uri, password: String?): Result<BackupMetadata> = withBusy {
        container.backupRepository.createBackup(uri, password?.toCharArray())
    }

    suspend fun restoreBackup(uri: Uri, password: String?): Result<Int> = withBusy {
        container.backupRepository.restore(uri, password?.toCharArray()).map { it.transactions }
    }

    suspend fun exportCsv(uri: Uri): Result<Unit> = withBusy {
        withContext(Dispatchers.IO) {
            val transactions = container.transactionRepository.getAll()
            val categories = container.categoryRepository.getAll()
            val accounts = container.accountRepository.getAll()
            val dates = transactions.map { it.date }
            val from = dates.minOrNull() ?: ir.dakhlokharj.core.date.JalaliDate.today()
            val to = dates.maxOrNull() ?: ir.dakhlokharj.core.date.JalaliDate.today()
            CsvExporter.write(container.appContext, uri, transactions, categories, accounts, from, to)
        }
        Result.success(Unit)
    }

    private suspend fun <T> withBusy(block: suspend () -> Result<T>): Result<T> {
        busy.value = true
        return try {
            block()
        } finally {
            busy.value = false
        }
    }
}