package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.transactionTypeLabel
import ir.dakhlokharj.ui.viewmodel.TransactionsViewModel
import kotlinx.coroutines.launch

@Composable
fun TransactionDetailScreen(
    viewModel: TransactionsViewModel,
    transactionId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val transaction = state.transactions.firstOrNull { it.id == transactionId }
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DkScreen(
        title = "جزئیات تراکنش",
        onBack = onBack,
        actions = {
            if (transaction != null) {
                IconButton(onClick = { onEdit(transaction.id) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "ویرایش")
                }
            }
        },
    ) { padding ->
        if (transaction == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.ready) {
                    Text("این تراکنش یافت نشد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CircularProgressIndicator()
                }
            }
            return@DkScreen
        }

        val isIncome = transaction.type == TransactionType.INCOME ||
            transaction.type == TransactionType.SAVINGS_WITHDRAW
        val category = state.categoryById[transaction.categoryId]
        val account = state.accountById[transaction.accountId]
        val target = state.accountById[transaction.targetAccountId]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        if (isIncome) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface,
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    transactionTypeLabel(transaction.type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    (if (isIncome) "+" else "−") + moneyText(transaction.amount),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    transaction.title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))
            DkCard {
                InfoRow("تاریخ", faDate(transaction.date, DateStyle.FULL))
                InfoRow("ساعت", faTime(transaction.timeMinutes))
                InfoRow("حساب", account?.name ?: "—")
                if (transaction.type == TransactionType.TRANSFER) {
                    InfoRow("حساب مقصد", target?.name ?: "—")
                }
                InfoRow("دسته‌بندی", category?.name ?: "بدون دسته")
                transaction.tag?.let { InfoRow("برچسب", it) }
                if (transaction.note.isNotBlank()) {
                    DkTopDivider()
                    Text(
                        "یادداشت",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(transaction.note, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            SecondaryButton(
                text = "حذف تراکنش",
                icon = Icons.Outlined.Delete,
            ) { showDelete = true }
        }
    }

    if (showDelete && transaction != null) {
        ConfirmDialog(
            title = "حذف تراکنش؟",
            message = "با حذف این تراکنش، اثر آن روی موجودی حساب به‌صورت خودکار برگشت داده می‌شود. این عملیات قابل بازگشت نیست.",
            confirmLabel = "حذف",
            danger = true,
            onConfirm = {
                scope.launch {
                    viewModel.delete(transaction)
                        .onSuccess { onBack() }
                        .onFailure { error = it.message ?: "حذف ناموفق بود" }
                }
            },
            onDismiss = { showDelete = false },
        )
    }
}
