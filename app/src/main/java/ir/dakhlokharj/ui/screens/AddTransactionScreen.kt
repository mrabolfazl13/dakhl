package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.transactionTypeLabel
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.TransactionsViewModel
import kotlinx.coroutines.launch

@Composable
fun AddTransactionScreen(
    viewModel: TransactionsViewModel,
    editingId: Long?,
    initialType: String,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var initialized by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(runCatching { TransactionType.valueOf(initialType) }.getOrDefault(TransactionType.EXPENSE)) }
    var amountToman by remember { mutableStateOf(0L) }
    var title by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var targetAccountId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf(JalaliDate.today()) }
    var note by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var createdAt by remember { mutableStateOf(0L) }
    var showAmountSheet by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetSheet by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(editingId, state.ready) {
        if (!initialized && state.ready) {
            if (editingId != null) {
                val existing = viewModel.getById(editingId)
                if (existing != null) {
                    type = existing.type
                    amountToman = existing.amount.toTomanRounded()
                    title = existing.title
                    categoryId = existing.categoryId
                    accountId = existing.accountId
                    targetAccountId = existing.targetAccountId
                    date = existing.date
                    note = existing.note
                    tag = existing.tag ?: ""
                    createdAt = existing.createdAt
                }
            } else {
                accountId = state.accounts.firstOrNull()?.id
            }
            initialized = true
        }
    }

    val availableCategories = state.categories.filter {
        when (type) {
            TransactionType.INCOME -> it.kind == CategoryKind.INCOME
            TransactionType.EXPENSE -> it.kind == CategoryKind.EXPENSE
            else -> false
        }
    }
    val selectedCategory = availableCategories.firstOrNull { it.id == categoryId }
    val selectedAccount = state.accounts.firstOrNull { it.id == accountId }
    val selectedTarget = state.accounts.firstOrNull { it.id == targetAccountId }

    DkScreen(
        title = if (editingId != null) "ویرایش تراکنش" else "ثبت تراکنش",
        subtitle = "همه‌چیز آفلاین و روی همین دستگاه ذخیره می‌شود",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SegmentedControl(
                    options = listOf("هزینه", "درآمد", "انتقال"),
                    selectedIndex = when (type) {
                        TransactionType.INCOME -> 1
                        TransactionType.TRANSFER -> 2
                        else -> 0
                    },
                ) { index ->
                    type = when (index) {
                        1 -> TransactionType.INCOME
                        2 -> TransactionType.TRANSFER
                        else -> TransactionType.EXPENSE
                    }
                    categoryId = null
                }
            }

            item {
                DkCard(
                    onClick = { showAmountSheet = true },
                    accent = colors.primary,
                    contentPadding = PaddingValues(20.dp),
                ) {
                    Text(
                        "مبلغ",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (amountToman == 0L) "۰" else moneyText(Money.toman(amountToman)),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (amountToman == 0L) colors.onSurfaceVariant.copy(alpha = 0.5f) else colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "تغییر مبلغ",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                    if (type == TransactionType.TRANSFER) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "مبلغ از حساب مبدأ کسر و به حساب مقصد اضافه می‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                DkTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "عنوان",
                    placeholder = when (type) {
                        TransactionType.INCOME -> "مثلاً حقوق مهر"
                        TransactionType.TRANSFER -> "مثلاً انتقال به حساب پس‌انداز"
                        else -> "مثلاً خرید هفتگی"
                    },
                )
            }

            if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
                item {
                    Text(
                        "دسته‌بندی",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            DkChoiceChip(
                                label = "بدون دسته",
                                selected = categoryId == null,
                                onClick = { categoryId = null },
                            )
                        }
                        items(availableCategories, key = { it.id }) { category ->
                            DkChoiceChip(
                                label = category.name,
                                selected = categoryId == category.id,
                                color = Color(category.colorArgb),
                                onClick = { categoryId = category.id },
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "حساب",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                PickerRow(
                    label = "از حساب",
                    value = selectedAccount?.name ?: "انتخاب حساب",
                    icon = ir.dakhlokharj.ui.theme.iconFor(selectedAccount?.iconKey ?: "wallet"),
                ) { showAccountSheet = true }
            }

            if (type == TransactionType.TRANSFER) {
                item {
                    PickerRow(
                        label = "به حساب",
                        value = selectedTarget?.name ?: "انتخاب حساب مقصد",
                        icon = ir.dakhlokharj.ui.theme.iconFor(selectedTarget?.iconKey ?: "wallet"),
                    ) { showTargetSheet = true }
                }
            }

            item {
                PickerRow(
                    label = "تاریخ",
                    value = faDate(date, DateStyle.FULL),
                    icon = Icons.Outlined.CalendarToday,
                ) { showDateSheet = true }
            }

            item {
                DkTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "یادداشت (اختیاری)",
                    placeholder = "جزئیات بیشتر درباره این تراکنش",
                    singleLine = false,
                    maxLines = 3,
                )
            }

            item {
                DkTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = "برچسب (اختیاری)",
                    placeholder = "مثلاً سفر، خانواده",
                )
            }

            if (!selectedCategory?.essential.orTrue()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(colors.secondary.copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "این دسته غیرضروری است؛ در تحلیل هزینه‌های اختیاری لحاظ می‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                PrimaryButton(
                    text = if (saving) "در حال ذخیره..." else if (editingId != null) "ذخیره تغییرات" else "ثبت تراکنش",
                    enabled = !saving,
                    icon = Icons.Outlined.Check,
                ) {
                    error = null
                    val amountValid = amountToman > 0
                    when {
                        !amountValid -> error = "مبلغ تراکنش را وارد کنید"
                        selectedAccount == null -> error = "یک حساب انتخاب کنید"
                        type == TransactionType.TRANSFER && selectedTarget == null -> error = "حساب مقصد را انتخاب کنید"
                        type == TransactionType.TRANSFER && selectedTarget?.id == selectedAccount?.id -> error = "حساب مبدأ و مقصد یکسان است"
                        else -> {
                            saving = true
                            scope.launch {
                                val transaction = Transaction(
                                    id = editingId ?: 0,
                                    type = type,
                                    amount = Money.toman(amountToman),
                                    title = title.ifBlank {
                                        when (type) {
                                            TransactionType.TRANSFER -> "انتقال بین حساب‌ها"
                                            TransactionType.INCOME -> "درآمد"
                                            else -> "هزینه"
                                        }
                                    },
                                    categoryId = categoryId,
                                    accountId = selectedAccount!!.id,
                                    targetAccountId = if (type == TransactionType.TRANSFER) selectedTarget?.id else null,
                                    date = date,
                                    timeMinutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute },
                                    note = note,
                                    tag = tag.ifBlank { null },
                                    createdAt = createdAt,
                                )
                                viewModel.save(transaction, editingId != null)
                                    .onSuccess { onBack() }
                                    .onFailure {
                                        error = it.message ?: "ثبت تراکنش ناموفق بود"
                                        saving = false
                                    }
                            }
                        }
                    }
                }
            }

            if (editingId != null) {
                item {
                    SecondaryButton(
                        text = "حذف تراکنش",
                        icon = Icons.Outlined.Delete,
                    ) {
                        scope.launch {
                            viewModel.getById(editingId)?.let { existing ->
                                viewModel.delete(existing).onSuccess { onBack() }
                                    .onFailure { error = it.message ?: "حذف ناموفق بود" }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAmountSheet) {
        AmountInputSheet(
            title = "مبلغ تراکنش",
            initialToman = amountToman,
            onDismiss = { showAmountSheet = false },
            onConfirm = { amountToman = it },
        )
    }
    if (showDateSheet) {
        DatePickerSheet(
            initial = date,
            onDismiss = { showDateSheet = false },
            onConfirm = { date = it },
        )
    }
    if (showAccountSheet) {
        OptionPickerSheet(
            title = "انتخاب حساب",
            options = state.accounts.map {
                SheetOption(it.id, it.name, moneyText(it.balance), Color(it.colorArgb), it.iconKey)
            },
            selectedId = accountId,
            onDismiss = { showAccountSheet = false },
            onSelect = { accountId = it },
        )
    }
    if (showTargetSheet) {
        OptionPickerSheet(
            title = "حساب مقصد",
            options = state.accounts.filter { it.id != accountId }.map {
                SheetOption(it.id, it.name, moneyText(it.balance), Color(it.colorArgb), it.iconKey)
            },
            selectedId = targetAccountId,
            onDismiss = { showTargetSheet = false },
            onSelect = { targetAccountId = it },
        )
    }
}

private fun Boolean?.orTrue(): Boolean = this ?: true
