package ir.dakhlokharj.data.local

import androidx.room.TypeConverter
import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.domain.model.BudgetPeriod
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.PlannedEventKind
import ir.dakhlokharj.domain.model.RecurringKind
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.domain.model.TransactionType

class Converters {
    @TypeConverter fun accountTypeToString(value: AccountType): String = value.name
    @TypeConverter fun stringToAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter fun transactionTypeToString(value: TransactionType): String = value.name
    @TypeConverter fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter fun categoryKindToString(value: CategoryKind): String = value.name
    @TypeConverter fun stringToCategoryKind(value: String): CategoryKind = CategoryKind.valueOf(value)

    @TypeConverter fun budgetPeriodToString(value: BudgetPeriod): String = value.name
    @TypeConverter fun stringToBudgetPeriod(value: String): BudgetPeriod = BudgetPeriod.valueOf(value)

    @TypeConverter fun budgetScopeToString(value: BudgetScope): String = value.name
    @TypeConverter fun stringToBudgetScope(value: String): BudgetScope = BudgetScope.valueOf(value)

    @TypeConverter fun recurringPeriodToString(value: RecurringPeriod): String = value.name
    @TypeConverter fun stringToRecurringPeriod(value: String): RecurringPeriod = RecurringPeriod.valueOf(value)

    @TypeConverter fun recurringKindToString(value: RecurringKind): String = value.name
    @TypeConverter fun stringToRecurringKind(value: String): RecurringKind = RecurringKind.valueOf(value)

    @TypeConverter fun plannedKindToString(value: PlannedEventKind): String = value.name
    @TypeConverter fun stringToPlannedKind(value: String): PlannedEventKind = PlannedEventKind.valueOf(value)
}
