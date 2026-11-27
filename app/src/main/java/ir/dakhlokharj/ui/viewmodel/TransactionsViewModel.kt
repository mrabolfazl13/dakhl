package ir.dakhlokharj.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.di.AppContainer
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionFilters(
    val query: String = "",
    val type: TransactionType? = null,
    val accountId: Long? = null,
)

data class TransactionsUiState(
    val ready: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filters: TransactionFilters = TransactionFilters(),
) {
    val totalIncome: Money
        get() = Money.sum(transactions.filter { it.type == TransactionType.INCOME }.map { it.amount })
    val totalExpense: Money
        get() = Money.sum(transactions.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
    val categoryById: Map<Long, Category> get() = categories.associateBy { it.id }
    val accountById: Map<Long, Account> get() = accounts.associateBy { it.id }
}

class TransactionsViewModel(private val container: AppContainer) : ViewModel() {

    private val filters = MutableStateFlow(TransactionFilters())
    val saving = MutableStateFlow(false)

    private val rawFlow = combine(
        container.transactionRepository.observeAll(),
        container.accountRepository.observeActive(),
        container.categoryRepository.observeAll(),
        filters,
    ) { transactions, accounts, categories, activeFilters ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        val query = activeFilters.query.trim()
        val filtered = transactions
            .filter { transaction ->
                val matchesType = activeFilters.type == null || transaction.type == activeFilters.type
                val matchesAccount = activeFilters.accountId == null || transaction.accountId == activeFilters.accountId
                val matchesQuery = query.isEmpty() ||
                    transaction.title.contains(query, ignoreCase = true) ||
                    transaction.note.contains(query, ignoreCase = true) ||
                    categoryMap[transaction.categoryId]?.name?.contains(query, ignoreCase = true) == true ||
                    accountMap[transaction.accountId]?.name?.contains(query, ignoreCase = true) == true
                matchesType && matchesAccount && matchesQuery
            }
            .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.timeMinutes })
        TransactionsUiState(
            ready = true,
            transactions = filtered,
            accounts = accounts,
            categories = categories,
            filters = activeFilters,
        )
    }

    val uiState: StateFlow<TransactionsUiState> =
        rawFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setQuery(query: String) {
        filters.value = filters.value.copy(query = query)
    }

    fun setType(type: TransactionType?) {
        filters.value = filters.value.copy(type = type)
    }

    fun setAccount(accountId: Long?) {
        filters.value = filters.value.copy(accountId = accountId)
    }

    fun transactionById(id: Long): Transaction? = uiState.value.transactions.firstOrNull { it.id == id }

    suspend fun getById(id: Long): Transaction? = container.transactionRepository.getById(id)

    suspend fun save(transaction: Transaction, editing: Boolean): Result<Long> = runCatching {
        saving.value = true
        try {
            if (editing) {
                container.transactionRepository.update(transaction)
                transaction.id
            } else {
                container.transactionRepository.add(transaction)
            }
        } finally {
            saving.value = false
        }
    }

    suspend fun delete(transaction: Transaction): Result<Unit> = runCatching {
        container.transactionRepository.delete(transaction.id)
    }

    suspend fun restoreDeleted(transaction: Transaction): Result<Unit> = runCatching {
        container.transactionRepository.add(transaction.copy(id = 0))
    }

    suspend fun createQuickExpense(amountToman: Long, note: String, accountId: Long): Result<Unit> = runCatching {
        require(amountToman > 0) { "مبلغ را وارد کنید" }
        val category = uiState.value.categories.firstOrNull { it.name == "سایر هزینه‌ها" }
        container.transactionRepository.add(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = Money.toman(amountToman),
                title = note.ifBlank { "هزینه" },
                accountId = accountId,
                categoryId = category?.id,
                date = ir.dakhlokharj.core.date.JalaliDate.today(),
            ),
        )
        Unit
    }

    fun refresh() {
        viewModelScope.launch { /* Room flows refresh automatically; kept for UI symmetry. */ }
    }
}