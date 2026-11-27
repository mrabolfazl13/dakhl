package ir.dakhlokharj.data.repository

import ir.dakhlokharj.data.local.AccountEntity
import ir.dakhlokharj.data.local.CategoryEntity
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.domain.model.CategoryKind

object SeedData {

    private val incomeCategories = listOf(
        Triple("حقوق", "salary", 0xFF34D399.toInt()),
        Triple("کسب‌وکار", "business", 0xFF22C55E.toInt()),
        Triple("پروژه", "project", 0xFF0EA5E9.toInt()),
        Triple("هدیه", "gift", 0xFFE9C46A.toInt()),
        Triple("سود دریافتی", "profit", 0xFF14B8A6.toInt()),
        Triple("سایر درآمدها", "income", 0xFF64748B.toInt()),
    )

    private val expenseCategories = listOf(
        Triple("خوراک", "food", 0xFFF97316.toInt()),
        Triple("حمل‌ونقل", "transport", 0xFF3B82F6.toInt()),
        Triple("مسکن", "home", 0xFF8B5CF6.toInt()),
        Triple("پوشاک", "clothes", 0xFFEC4899.toInt()),
        Triple("درمان", "health", 0xFFEF4444.toInt()),
        Triple("تفریح", "fun", 0xFFE9C46A.toInt()),
        Triple("آموزش", "education", 0xFF06B6D4.toInt()),
        Triple("قبوض", "bills", 0xFF64748B.toInt()),
        Triple("خرید", "shopping", 0xFFF59E0B.toInt()),
        Triple("خانواده", "family", 0xFF10B981.toInt()),
        Triple("سایر هزینه‌ها", "other", 0xFF475569.toInt()),
    )

    /** Non-essential categories used by the insights engine to estimate discretionary spending. */
    private val nonEssential = setOf("تفریح", "خرید", "پوشاک", "سایر هزینه‌ها")

    suspend fun seedIfEmpty(db: DkDatabase) {
        if (db.categoryDao().count() == 0) {
            val categories = mutableListOf<CategoryEntity>()
            var order = 0
            incomeCategories.forEach { (name, icon, color) ->
                categories += CategoryEntity(
                    name = name,
                    kind = CategoryKind.INCOME,
                    iconKey = icon,
                    colorArgb = color,
                    essential = true,
                    system = true,
                    sortOrder = order++,
                )
            }
            order = 0
            expenseCategories.forEach { (name, icon, color) ->
                categories += CategoryEntity(
                    name = name,
                    kind = CategoryKind.EXPENSE,
                    iconKey = icon,
                    colorArgb = color,
                    essential = name !in nonEssential,
                    system = true,
                    sortOrder = order++,
                )
            }
            db.categoryDao().insertAll(categories)
        }
        if (db.accountDao().count() == 0) {
            db.accountDao().insert(
                AccountEntity(
                    name = "کیف پول نقد",
                    type = AccountType.CASH,
                    openingBalanceRial = 0,
                    balanceRial = 0,
                    colorArgb = 0xFF34D399.toInt(),
                    iconKey = "wallet",
                    createdAt = System.currentTimeMillis(),
                    sortOrder = 0,
                ),
            )
        }
    }
}
