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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.ui.accountTypeLabel
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.moneyText
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

private val accountColors = listOf(
    0xFF34D399.toInt(),
    0xFF60A5FA.toInt(),
    0xFFE9C46A.toInt(),
    0xFFF472B6.toInt(),
    0xFF818CF8.toInt(),
    0xFF94A3B8.toInt(),
)

@Composable
fun AccountsScreen(
    viewModel: PlanningViewModel,
    onBack: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onAddTransaction: (String) -> Unit,
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var showOpeningSheet by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    var openingToman by remember { mutableStateOf(0L) }
    var color by remember { mutableStateOf(accountColors.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    val total = Money.sum(accounts.map { it.balance })

    DkScreen(
        title = "حساب‌ها",
        subtitle = "همه دارایی‌ها در یک نگاه",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "افزودن حساب")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 6.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DkCard(contentPadding = PaddingValues(20.dp)) {
                    Text(
                        "جمع دارایی",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(moneyText(total), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${fa(accounts.size)} حساب فعال • ${if (settings.hideBalances) "موجودی پنهان" else "همه‌چیز آفلاین"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(accounts, key = { it.id }) { account ->
                AccountCard(account, onClick = { onOpenAccount(account.id) })
            }

            item {
                EmptyState(
                    title = "حساب جدید بسازید",
                    description = "حساب نقدی، بانکی، پس‌انداز یا سرمایه‌گذاری را جدا نگه دارید تا تحلیل دقیق‌تری داشته باشید.",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    actionLabel = "افزودن حساب",
                    onAction = { showAdd = true },
                )
            }
        }
    }

    if (showAdd) {
        DkBottomSheet(onDismiss = { showAdd = false }) {
            SheetTitle("حساب جدید", "نوع حساب و موجودی اولیه را مشخص کنید")
            DkTextField(name, { name = it }, label = "نام حساب", placeholder = "مثلاً حساب بانک ملت")
            Spacer(Modifier.height(12.dp))
            Text("نوع حساب", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AccountType.entries.toList()) { entry ->
                    DkChoiceChip(accountTypeLabel(entry), type == entry) { type = entry }
                }
            }
            Spacer(Modifier.height(12.dp))
            PickerRow(
                label = "موجودی اولیه",
                value = moneyText(Money.toman(openingToman)),
                icon = Icons.Outlined.AccountBalanceWallet,
            ) { showOpeningSheet = true }
            Spacer(Modifier.height(12.dp))
            Text("رنگ حساب", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accountColors.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .clickable { color = argb },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (color == argb) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }
                    }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "ساخت حساب", icon = Icons.Outlined.Add) {
                if (name.isBlank()) {
                    error = "نام حساب را وارد کنید"
                } else {
                    scope.launch {
                        viewModel.saveAccount(
                            Account(
                                name = name,
                                type = type,
                                openingBalance = Money.toman(openingToman),
                                balance = Money.toman(openingToman),
                                colorArgb = color,
                                iconKey = when (type) {
                                    AccountType.BANK -> "bank"
                                    AccountType.CASH -> "wallet"
                                    AccountType.SAVINGS -> "savings"
                                    AccountType.DIGITAL_WALLET -> "phone"
                                    AccountType.CUSTOM -> "wallet"
                                },
                            ),
                            editing = false,
                        ).onSuccess {
                            showAdd = false
                            name = ""
                            openingToman = 0
                            error = null
                        }.onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showOpeningSheet) {
        AmountInputSheet(
            title = "موجودی اولیه",
            initialToman = openingToman,
            onDismiss = { showOpeningSheet = false },
            onConfirm = { openingToman = it },
        )
    }
}