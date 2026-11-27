package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.ui.accountTypeLabel
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

private val palette = listOf(
    0xFF34D399.toInt(), 0xFF60A5FA.toInt(), 0xFFE9C46A.toInt(),
    0xFFF472B6.toInt(), 0xFF818CF8.toInt(), 0xFF94A3B8.toInt(),
)

@Composable
fun AccountDetailScreen(
    viewModel: PlanningViewModel,
    accountId: Long,
    onBack: () -> Unit,
    onAddTransaction: (String) -> Unit,
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val account = accounts.firstOrNull { it.id == accountId }
    val scope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var showOpening by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    var openingToman by remember { mutableStateOf(0L) }
    var color by remember { mutableStateOf(palette.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(account) {
        if (account != null && !showEdit) {
            name = account.name
            type = account.type
            openingToman = account.openingBalance.toTomanRounded()
            color = account.colorArgb
        }
    }

    val accountTransactions = transactions
        .filter { it.accountId == accountId || it.targetAccountId == accountId }
        .sortedWith(compareByDescending { it.date }.thenByDescending { it.createdAt })
    val categoryMap = categories.associateBy { it.id }

    DkScreen(
        title = account?.name ?: "حساب",
        subtitle = account?.let { accountTypeLabel(it.type) },
        onBack = onBack,
        actions = {
            if (account != null) {
                IconButton(onClick = { showEdit = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "ویرایش")
                }
                IconButton(onClick = { showArchive = true }) {
                    Icon(Icons.Outlined.Archive, contentDescription = "بایگانی")
                }
            }
        },
    ) { padding ->
        if (account == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("حساب یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@DkScreen
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 6.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(account.colorArgb).copy(alpha = 0.35f), MaterialTheme.colorScheme.surface),
                            ),
                        )
                        .padding(22.dp),
                ) {
                    Text("موجودی فعلی", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(moneyText(account.balance), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("موجودی اولیه", moneyShort(account.openingBalance), Modifier.weight(1f))
                        StatTile("تعداد تراکنش", fa(accountTransactions.size), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton("هزینه جدید", Modifier.weight(1f), icon = Icons.Outlined.Add) { onAddTransaction("EXPENSE") }
                        SecondaryButton("درآمد", Modifier.weight(1f)) { onAddTransaction("INCOME") }
                    }
                }
            }

            item {
                SectionHeader("تاریخچه تراکنش‌ها")
            }
            if (accountTransactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "تراکنشی برای این حساب نیست",
                        description = "با ثبت اولین تراکنش، تاریخچه این حساب ساخته می‌شود.",
                        icon = Icons.Outlined.Add,
                    )
                }
            } else {
                items(accountTransactions, key = { it.id }) { transaction ->
                    TransactionTile(
                        transaction = transaction,
                        categoryName = categoryMap[transaction.categoryId]?.name,
                        accountName = faDate(transaction.date, DateStyle.SHORT),
                    )
                }
            }
        }
    }

    if (showEdit && account != null) {
        DkBottomSheet(onDismiss = { showEdit = false }) {
            SheetTitle("ویرایش حساب", "تغییر موجودی اولیه روی سابقه تراکنش‌ها اثر نمی‌گذارد")
            DkTextField(name, { name = it }, label = "نام حساب")
            Spacer(Modifier.height(12.dp))
            Text("نوع حساب", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AccountType.entries.toList()) { entry ->
                    DkChoiceChip(accountTypeLabel(entry), type == entry) { type = entry }
                }
            }
            Spacer(Modifier.height(12.dp))
            PickerRow("موجودی اولیه", moneyText(Money.toman(openingToman))) { showOpening = true }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .clickable { color = argb },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (color == argb) Box(Modifier.size(9.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("ذخیره تغییرات") {
                if (name.isBlank()) {
                    error = "نام حساب را وارد کنید"
                } else {
                    scope.launch {
                        viewModel.saveAccount(
                            account.copy(
                                name = name,
                                type = type,
                                openingBalance = Money.toman(openingToman),
                                colorArgb = color,
                            ),
                            editing = true,
                        ).onSuccess { showEdit = false }
                            .onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showOpening) {
        AmountInputSheet(
            title = "موجودی اولیه",
            initialToman = openingToman,
            onDismiss = { showOpening = false },
            onConfirm = { openingToman = it },
        )
    }

    if (showArchive && account != null) {
        ConfirmDialog(
            title = if (account.archived) "بازگرداندن حساب؟" else "بایگانی حساب؟",
            message = if (account.archived) {
                "این حساب دوباره در فهرست حساب‌های فعال نمایش داده می‌شود."
            } else {
                "حساب از فهرست فعال حذف می‌شود اما تاریخچه و موجودی آن حفظ می‌شود."
            },
            confirmLabel = if (account.archived) "بازگرداندن" else "بایگانی",
            onConfirm = {
                scope.launch {
                    viewModel.archiveAccount(account, !account.archived).onSuccess { onBack() }
                }
            },
            onDismiss = { showArchive = false },
        )
    }
}