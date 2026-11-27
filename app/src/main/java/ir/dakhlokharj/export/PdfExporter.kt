package ir.dakhlokharj.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import ir.dakhlokharj.R
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.format.MoneyFormatter
import ir.dakhlokharj.core.format.PersianText
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.ReportEngine
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.ui.transactionTypeLabel

/** Builds a multi-page, right-to-left Persian PDF report with Android's native PdfDocument. */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 80

    fun write(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
        from: JalaliDate,
        to: JalaliDate,
    ) {
        val regular = ResourcesCompat.getFont(context, R.font.vazirmatn_regular)
            ?: Typeface.SANS_SERIF
        val bold = ResourcesCompat.getFont(context, R.font.vazirmatn_bold) ?: regular
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(pageInfo(pageNumber))
        var canvas = page.canvas
        var y = MARGIN

        val body = textPaint(regular, 11f, Color.rgb(30, 41, 59))
        val small = textPaint(regular, 9f, Color.rgb(100, 116, 139))
        val heading = textPaint(bold, 15f, Color.rgb(15, 23, 42))
        val title = textPaint(bold, 21f, Color.rgb(5, 150, 105))

        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        val periodTransactions = transactions
            .filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
            .sortedBy { it.date }
        val income = Money.sum(
            periodTransactions.filter { it.type == TransactionType.INCOME }.map { it.amount },
        )
        val expense = Money.sum(
            periodTransactions.filter { it.type == TransactionType.EXPENSE }.map { it.amount },
        )
        val net = income - expense
        val expenseSlices = ReportEngine.categoryTotals(
            periodTransactions, categories, ir.dakhlokharj.domain.model.CategoryKind.EXPENSE, from, to,
        )

        fun finishPage() {
            val footer = textPaint(regular, 8f, Color.rgb(148, 163, 184))
            drawRaw(canvas, PersianText.toPersianDigits("صفحه " + pageNumber), MARGIN, PAGE_HEIGHT - 24f, CONTENT_WIDTH, footer)
            document.finishPage(page)
        }

        fun newPage() {
            finishPage()
            pageNumber += 1
            page = document.startPage(pageInfo(pageNumber))
            canvas = page.canvas
            y = MARGIN
        }

        fun ensure(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN - 12f) newPage()
        }

        fun paragraph(text: String, paint: TextPaint, gap: Float = 6f) {
            val height = measure(text, paint, CONTENT_WIDTH)
            ensure(height + gap)
            y += draw(canvas, text, y, paint, CONTENT_WIDTH)
            y += gap
        }

        fun section(text: String) {
            ensure(42f)
            y += 12f
            y += draw(canvas, text, y, heading, CONTENT_WIDTH)
            y += 8f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 0.8f
            })
            y += 10f
        }

        fun line(label: String, value: String) {
            val paint = body
            val text = label + ": " + value
            val height = measure(text, paint, CONTENT_WIDTH)
            ensure(height + 5f)
            y += draw(canvas, text, y, paint, CONTENT_WIDTH)
            y += 5f
        }

        y += draw(canvas, "گزارش مالی دخل‌وخرج", y, title, CONTENT_WIDTH)
        y += 6f
        paragraph("از " + from.formatPersianDigits() + " تا " + to.formatPersianDigits(), small, 12f)

        section("خلاصه دوره")
        line("مجموع درآمد", MoneyFormatter.format(income))
        line("مجموع هزینه", MoneyFormatter.format(expense))
        line("مانده دوره", MoneyFormatter.format(net))
        line("تعداد تراکنش", PersianText.toPersianDigits(periodTransactions.size.toString()))

        if (expenseSlices.isNotEmpty()) {
            section("هزینه به تفکیک دسته")
            expenseSlices.forEach { slice ->
                val percent = PersianText.toPersianDigits((slice.share * 100).toInt().toString())
                line(slice.name, MoneyFormatter.format(slice.amount) + " • " + percent + "٪")
            }
        }

        section("تراکنش‌های دوره")
        if (periodTransactions.isEmpty()) {
            paragraph("در این دوره تراکنشی ثبت نشده است.", body, 8f)
        } else {
            periodTransactions.forEach { transaction ->
                val type = transactionTypeLabel(transaction.type)
                val category = categoryMap[transaction.categoryId]?.name ?: "بدون دسته"
                val account = accountMap[transaction.accountId]?.name ?: "حساب حذف‌شده"
                val amount = MoneyFormatter.format(transaction.amount)
                val text = PersianText.toPersianDigits(transaction.date.formatShort()) +
                    " • " + type + " • " + transaction.title +
                    " • " + category + " • " + account + " • " + amount
                paragraph(text, body, 3f)
                if (transaction.note.isNotBlank()) {
                    paragraph("یادداشت: " + transaction.note, small, 6f)
                }
            }
        }

        finishPage()
        val stream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalArgumentException("امکان نوشتن فایل وجود ندارد")
        stream.use { output -> document.writeTo(output) }
        document.close()
    }

    private fun pageInfo(number: Int): PdfDocument.PageInfo =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()

    private fun textPaint(typeface: Typeface, size: Float, color: Int): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            this.color = color
        }

    private fun layout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, 1.25f)
            .setIncludePad(false)
            .build()

    private fun measure(text: String, paint: TextPaint, width: Int): Float =
        layout(text, paint, width).height.toFloat()

    private fun draw(canvas: Canvas, text: String, top: Float, paint: TextPaint, width: Int): Float {
        val staticLayout = layout(text, paint, width)
        canvas.save()
        canvas.translate(MARGIN, top)
        staticLayout.draw(canvas)
        canvas.restore()
        return staticLayout.height.toFloat()
    }

    private fun drawRaw(canvas: Canvas, text: String, left: Float, top: Float, width: Int, paint: TextPaint) {
        val staticLayout = layout(text, paint, width)
        canvas.save()
        canvas.translate(left, top)
        staticLayout.draw(canvas)
        canvas.restore()
    }
}
