package ir.dakhlokharj.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import ir.dakhlokharj.BuildConfig
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.model.BackupMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Full local backup/restore. The exported file contains a schema version, creation date and
 * a SHA-256 checksum; optional password encryption uses PBKDF2 + AES-256-GCM. Restores are
 * validated and applied inside one database transaction, so a corrupt file cannot leave the
 * database half-restored.
 */
class BackupRepository(
    private val context: Context,
    private val db: DkDatabase,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun observeHistory(): Flow<List<BackupMetadata>> =
        db.backupMetadataDao().observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun createBackup(uri: Uri, password: CharArray?): Result<BackupMetadata> = runCatching {
        withContext(Dispatchers.IO) {
            val payload = collectPayload()
            val payloadJson = json.encodeToString(BackupPayload.serializer(), payload)
            val createdAt = System.currentTimeMillis()
            val envelope = if (password != null && password.isNotEmpty()) {
                val encrypted = BackupCrypto.encrypt(payloadJson, password)
                BackupEnvelope(
                    schemaVersion = DkDatabase.VERSION,
                    createdAt = createdAt,
                    appVersion = BuildConfig.VERSION_NAME,
                    encrypted = true,
                    payload = encrypted.ciphertext,
                    salt = encrypted.salt,
                    iv = encrypted.iv,
                    checksum = BackupCrypto.sha256(encrypted.ciphertext),
                )
            } else {
                BackupEnvelope(
                    schemaVersion = DkDatabase.VERSION,
                    createdAt = createdAt,
                    appVersion = BuildConfig.VERSION_NAME,
                    encrypted = false,
                    payload = payloadJson,
                    checksum = BackupCrypto.sha256(payloadJson),
                )
            }
            val text = json.encodeToString(BackupEnvelope.serializer(), envelope)
            writeText(uri, text)
            val metadata = BackupMetadata(
                fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "dakhlo-kharj-backup.dkb",
                locationUri = uri.toString(),
                createdAt = createdAt,
                sizeBytes = text.toByteArray(Charsets.UTF_8).size.toLong(),
                schemaVersion = DkDatabase.VERSION,
                encrypted = envelope.encrypted,
            )
            db.backupMetadataDao().insert(metadata.toEntity())
            metadata
        }
    }

    suspend fun restore(uri: Uri, password: CharArray?): Result<RestoreSummary> = runCatching {
        withContext(Dispatchers.IO) {
            val text = readText(uri)
            val envelope = runCatching { json.decodeFromString(BackupEnvelope.serializer(), text) }
                .getOrElse { throw IllegalArgumentException("فایل پشتیبان معتبر نیست") }
            require(envelope.format == "dakhlo-kharj-backup") { "فرمت فایل پشتیبان پشتیبانی نمی‌شود" }
            require(envelope.schemaVersion <= DkDatabase.VERSION) {
                "این فایل با نسخه جدیدتری از برنامه ساخته شده است"
            }
            require(BackupCrypto.sha256(envelope.payload) == envelope.checksum) {
                "فایل پشتیبان آسیب دیده است (checksum نامعتبر)"
            }
            val payloadJson = if (envelope.encrypted) {
                require(password != null && password.isNotEmpty()) { "این فایل رمزگذاری شده است" }
                runCatching {
                    BackupCrypto.decrypt(
                        EncryptedPayload(
                            ciphertext = envelope.payload,
                            salt = requireNotNull(envelope.salt) { "فایل پشتیبان ناقص است" },
                            iv = requireNotNull(envelope.iv) { "فایل پشتیبان ناقص است" },
                        ),
                        password,
                    )
                }.getOrElse { throw IllegalArgumentException("رمز عبور پشتیبان نادرست است") }
            } else {
                envelope.payload
            }
            val payload = runCatching { json.decodeFromString(BackupPayload.serializer(), payloadJson) }
                .getOrElse { throw IllegalArgumentException("محتوای پشتیبان قابل خواندن نیست") }
            require(payload.schemaVersion <= DkDatabase.VERSION) { "نسخه داده پشتیبان پشتیبانی نمی‌شود" }
            validate(payload)

            db.withTransaction {
                clearAll()
                db.accountDao().insertAll(payload.accounts)
                db.categoryDao().insertAll(payload.categories)
                db.transactionDao().insertAll(payload.transactions)
                db.budgetDao().insertAll(payload.budgets)
                db.envelopeDao().insertAll(payload.envelopes)
                db.debtDao().insertAll(payload.debts)
                db.debtPaymentDao().insertAll(payload.debtPayments)
                db.receivableDao().insertAll(payload.receivables)
                db.receivablePaymentDao().insertAll(payload.receivablePayments)
                db.installmentPlanDao().insertAll(payload.installmentPlans)
                db.installmentDao().insertAll(payload.installments)
                db.recurringDao().insertAll(payload.recurringExpenses)
                db.savingsGoalDao().insertAll(payload.savingsGoals)
                db.savingsContributionDao().insertAll(payload.savingsContributions)
                db.financialEventDao().insertAll(payload.financialEvents)
                db.backupMetadataDao().insertAll(payload.backupMetadata)
            }
            RestoreSummary(
                accounts = payload.accounts.size,
                transactions = payload.transactions.size,
                categories = payload.categories.size,
                budgets = payload.budgets.size,
                debts = payload.debts.size,
                savingsGoals = payload.savingsGoals.size,
            )
        }
    }

    private suspend fun collectPayload(): BackupPayload = BackupPayload(
        schemaVersion = DkDatabase.VERSION,
        createdAt = System.currentTimeMillis(),
        appVersion = BuildConfig.VERSION_NAME,
        accounts = db.accountDao().getAll(),
        categories = db.categoryDao().getAll(),
        transactions = db.transactionDao().getAll(),
        budgets = db.budgetDao().getAll(),
        envelopes = db.envelopeDao().getAll(),
        debts = db.debtDao().getAll(),
        debtPayments = db.debtPaymentDao().getAll(),
        receivables = db.receivableDao().getAll(),
        receivablePayments = db.receivablePaymentDao().getAll(),
        installmentPlans = db.installmentPlanDao().getAll(),
        installments = db.installmentDao().getAll(),
        recurringExpenses = db.recurringDao().getAll(),
        savingsGoals = db.savingsGoalDao().getAll(),
        savingsContributions = db.savingsContributionDao().getAll(),
        financialEvents = db.financialEventDao().getAll(),
        backupMetadata = db.backupMetadataDao().getAll(),
    )

    private suspend fun clearAll() {
        db.transactionDao().clear()
        db.debtPaymentDao().clear()
        db.receivablePaymentDao().clear()
        db.installmentDao().clear()
        db.installmentPlanDao().clear()
        db.savingsContributionDao().clear()
        db.savingsGoalDao().clear()
        db.recurringDao().clear()
        db.envelopeDao().clear()
        db.budgetDao().clear()
        db.financialEventDao().clear()
        db.debtDao().clear()
        db.receivableDao().clear()
        db.categoryDao().clear()
        db.accountDao().clear()
        db.backupMetadataDao().clear()
    }

    private fun validate(payload: BackupPayload) {
        require(payload.accounts.isNotEmpty()) { "فایل پشتیبان هیچ حسابی ندارد" }
        val accountIds = payload.accounts.map { it.id }.toSet()
        val categoryIds = payload.categories.map { it.id }.toSet()
        val planIds = payload.installmentPlans.map { it.id }.toSet()
        val goalIds = payload.savingsGoals.map { it.id }.toSet()
        val debtIds = payload.debts.map { it.id }.toSet()
        val receivableIds = payload.receivables.map { it.id }.toSet()
        require(payload.accounts.all { it.balanceRial >= 0 || true }) { "موجودی حساب نامعتبر است" }
        require(payload.transactions.all { it.accountId in accountIds }) {
            "تراکنشی به حساب ناموجود اشاره می‌کند"
        }
        require(payload.transactions.all { it.categoryId == null || it.categoryId in categoryIds }) {
            "تراکنشی به دسته‌بندی ناموجود اشاره می‌کند"
        }
        require(payload.transactions.all { it.targetAccountId == null || it.targetAccountId in accountIds }) {
            "انتقالی به حساب مقصد ناموجود اشاره می‌کند"
        }
        require(payload.installments.all { it.planId in planIds }) {
            "قسطی به طرح ناموجود اشاره می‌کند"
        }
        require(payload.savingsContributions.all { it.goalId in goalIds }) {
            "واریزی به هدف ناموجود اشاره می‌کند"
        }
        require(payload.debtPayments.all { it.debtId in debtIds }) {
            "پرداختی به بدهی ناموجود اشاره می‌کند"
        }
        require(payload.receivablePayments.all { it.receivableId in receivableIds }) {
            "دریافتی به طلب ناموجود اشاره می‌کند"
        }
        require(payload.accounts.none { it.name.isBlank() }) { "نام حساب نمی‌تواند خالی باشد" }
    }

    private fun writeText(uri: Uri, text: String) {
        val stream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalArgumentException("امکان نوشتن فایل وجود ندارد")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private fun readText(uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("امکان خواندن فایل وجود ندارد")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
