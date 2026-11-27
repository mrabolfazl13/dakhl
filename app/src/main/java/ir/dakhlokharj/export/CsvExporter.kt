package ir.dakhlokharj.export

import android.content.Context
import android.net.Uri
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.core.format.PersianText
import ir.dakhlokharj.ui.transactionTypeLabel

/** Builds a UTF-8 (with BOM) CSV report that opens correctly in Excel and Numbers. */
object CsvExporter {

    fun write(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
        from: JalaliDate,
        to: JalaliDate,
    ) {
        val text = build(transactions, categories, accounts, from, to)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalArgumentException("امکان نوشتن فایل وجود ندارد")
        stream.use { output ->
            output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            output.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    fun build(
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
        from: JalaliDate,
        to: JalaliDate,
    ): String {
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        val income = Money.sum(transactions.filter { it.type == TransactionType.INCOME }.map { it.amount })
        val expense = Money.sum(transactions.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
        val builder = StringBuilder()
        builder.appendLine(csv("گزارش دخل و خرج"))
        builder.appendLine(csv("از تاریخ", from.formatFull()))
        builder.appendLine(csv("تا تاریخ", to.formatFull()))
        builder.appendLine(csv("مجموع درآمد (تومان)", from.rial(income)))
        builder.appendLine(csv("مجموع هزینه (تومان)", from.rial(expense)))
        builder.appendLine(csv("مانده دوره (تومان)", from.rial(income - expense)))
        builder.appendLine(csv("تعداد تراکنش", PersianText.toPersianDigits(transactions.size.toString())))
        builder.appendLine()
        builder.appendLine(
            listOf("تاریخ", "نوع", "عنوان", "دسته‌بندی", "حساب", "مبلغ (تومان)", "یادداشت", "برچسب")
                .joinToString(",") { csv(it) },
        )
        transactions.sortedBy { it.date }.forEach { transaction ->
            builder.appendLine(
                listOf(
                    transaction.date.formatShort(),
                    transactionTypeLabel(transaction.type),
                    transaction.title,
                    categoryMap[transaction.categoryId]?.name ?: "",
                    accountMap[transaction.accountId]?.name ?: "",
                    (transaction.amount.toTomanRounded()).toString(),
                    transaction.note,
                    transaction.tag ?: "",
                ).joinToString(",") { csv(it) },
            )
        }
        return builder.toString()
    }

    private fun JalaliDate.rial(money: Money): String = money.toTomanRounded().toString()

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
