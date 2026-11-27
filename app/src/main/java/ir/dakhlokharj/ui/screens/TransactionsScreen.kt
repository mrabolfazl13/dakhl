package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.faDate
import ir.dakhlokharj.ui.theme.moneyText
import ir.dakhlokharj.ui.viewmodel.TransactionsViewModel

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onOpenTransaction: (Long) -> Unit,
    onAddTransaction: (String) -> Unit,
    onOpenAccounts: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var showAccountFilter by remember { mutableStateOf(false) }
    val grouped = state.transactions.groupBy { it.date }

    DkScreen(
        title = "تراکنش‌ها",
        subtitle = "جست‌وجو، فیلتر و مرور کامل تاریخ مالی",
        actions = {
            IconButton(onClick = { showAccountFilter = true }) {
                Icon(Icons.Outlined.FilterList, contentDescription = "فیلتر حساب")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTransaction("EXPENSE") },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ) { Icon(Icons.Outlined.Add, contentDescription = "افزودن تراکنش") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                DkTextField(
                    value = state.filters.query,
                    onValueChange = viewModel::setQuery,
                    label = "جست‌وجو",
                    placeholder = "عنوان، دسته، حساب یا یادداشت",
                    leadingIcon = Icons.Outlined.Search,
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        DkChoiceChip("همه", state.filters.type == null) { viewModel.setType(null) }
                    }
                    items(TransactionType.entries.toList()) { type ->
                        if (type == TransactionType.EXPENSE || type == TransactionType.INCOME || type == TransactionType.TRANSFER) {
                            DkChoiceChip(
                                label = ir.dakhlokharj.ui.transactionTypeLabel(type),
                                selected = state.filters.type == type,
                            ) { viewModel.setType(type) }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    StatTile("درآمد", moneyText(state.totalIncome), Modifier.weight(1f), colors.primary)
                    StatTile("هزینه", moneyText(state.totalExpense), Modifier.weight(1f), colors.secondary)
                    StatTile("خالص", moneyText(state.totalIncome - state.totalExpense), Modifier.weight(1f))
                }
            }

            if (state.transactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "تراکنشی پیدا نشد",
                        description = "فیلترها را تغییر دهید یا اولین تراکنش را ثبت کنید.",
                        icon = Icons.Outlined.Add,
                        actionLabel = "ثبت تراکنش",
                        onAction = { onAddTransaction("EXPENSE") },
                    )
                }
            } else {
                grouped.forEach { (date, transactions) ->
                    item(key = "header-${date.formatShort()}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                faDate(date),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                fa(transactions.size) + " تراکنش",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionTile(
                            transaction = transaction,
                            categoryName = state.categoryById[transaction.categoryId]?.name,
                            accountName = state.accountById[transaction.accountId]?.name,
                        ) { onOpenTransaction(transaction.id) }
                    }
                }
            }
        }
    }

    if (showAccountFilter) {
        OptionPickerSheet(
            title = "فیلتر حساب",
            options = listOf(SheetOption(0L, "همه حساب‌ها")) +
                state.accounts.map { SheetOption(it.id, it.name, moneyText(it.balance)) },
            selectedId = state.filters.accountId,
            onDismiss = { showAccountFilter = false },
            onSelect = { id -> viewModel.setAccount(if (id == 0L) null else id) },
        )
    }
}
