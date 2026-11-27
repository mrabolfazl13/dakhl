# طرح پایگاه داده — دخل‌وخرج

Room، نسخه ۱، بدون `fallbackToDestructiveMigration` (مهاجرت‌ها در `Migrations.kt` نگهداری می‌شوند).
همه مبالغ به ریال (`Long`) و همه تاریخ‌ها به `epochDay` (`Long`) ذخیره می‌شوند.

## جداول

### accounts
| ستون | نوع | توضیح |
| --- | --- | --- |
| id | Long PK | شناسه |
| name | String | نام حساب |
| type | AccountType | BANK / CASH / DIGITAL_WALLET / SAVINGS / CUSTOM |
| openingBalanceRial | Long | موجودی اولیه |
| balanceRial | Long | موجودی جاری |
| colorArgb | Int | رنگ |
| iconKey | String | آیکون |
| archived | Boolean | بایگانی |
| createdAt | Long | زمان ایجاد |
| sortOrder | Int | ترتیب |

### categories
id، name، kind (INCOME/EXPENSE)، iconKey، colorArgb، essential، system، sortOrder.
ایندکس: `kind`، `sortOrder`.

### transactions
id، type (INCOME/EXPENSE/TRANSFER/ADJUSTMENT/SAVINGS_DEPOSIT/SAVINGS_WITHDRAW)، amountRial، title،
categoryId، accountId، targetAccountId، dateEpochDay، timeMinutes، note، receiptUri، tag،
recurringId، debtId، receivableId، installmentId، savingsGoalId، createdAt، updatedAt.
ایندکس: `dateEpochDay`، `accountId`، `categoryId`، `type`.

### budgets
id، name، scope (OVERALL/CATEGORY)، categoryId، period (MONTHLY/WEEKLY/CUSTOM)، startEpochDay،
endEpochDay، amountRial، alertAt50/75/90/100، repeatNextMonth، createdAt.

### envelopes
id، name، categoryId، allocatedRial، sortOrder.

### debts
id، creditor، principalRial، paidRial، dueEpochDay، installmentCount، note، createdAt.

### debt_payments
id، debtId، amountRial، dateEpochDay، transactionId (unique)، createdAt.

### receivables
id، debtor، amountRial، receivedRial، expectedEpochDay، note، createdAt.

### receivable_payments
id، receivableId، amountRial، dateEpochDay، transactionId (unique)، createdAt.

### installment_plans
id، title، totalRial، downPaymentRial، installmentCount، installmentRial، firstDueEpochDay،
accountId، categoryId، note، createdAt.

### installments
id، planId، index، amountRial، dueEpochDay، paidEpochDay، transactionId.
ایندکس: `planId`، `dueEpochDay`.

### recurring_expenses
id، title، amountRial، categoryId، accountId، period (WEEKLY/MONTHLY/YEARLY)، nextDueEpochDay،
kind (BILL/SUBSCRIPTION)، active، reminderEnabled، reminderDaysBefore، note، lastPaidEpochDay، createdAt.

### savings_goals
id، title، targetRial، savedRial، targetEpochDay، colorArgb، iconKey، note، achieved، createdAt.

### savings_contributions
id، goalId، amountRial، dateEpochDay، transactionId، createdAt.

### financial_events
id، kind (INCOME/EXPENSE)، title، amountRial، dateEpochDay، definite، note، createdAt.

### backup_metadata
id، fileName، locationUri، createdAt، sizeBytes، schemaVersion، encrypted.

## روابط منطقی

- `transactions.categoryId → categories.id`
- `transactions.accountId → accounts.id` و `targetAccountId → accounts.id`
- `debt_payments.debtId → debts.id`
- `receivable_payments.receivableId → receivables.id`
- `installments.planId → installment_plans.id`
- `savings_contributions.goalId → savings_goals.id`
- `transactions.debtId / receivableId / installmentId / savingsGoalId / recurringId` ارجاع نرم به موجودیت‌های مرتبط

SQLite به‌صورت پیش‌فرض Foreign Key اجباری ندارد؛ اعتبارسنجی ارجاعات در لایه مخازن و در
`BackupRepository` (هنگام بازیابی) انجام می‌شود.

## قواعد حذف

- حذف تراکنش: برگشت اثر مالی روی حساب(ها) و سپس حذف.
- حذف طرح اقساط: حذف اقساط وابسته و برگشت تراکنش‌های پرداخت‌شده.
- حذف بدهی/طلب: حذف پرداخت‌ها و تراکنش‌های مرتبط.
- حذف دسته سیستمی ممنوع؛ دسته استفاده‌شده قابل حذف نیست.
- حذف حساب دارای تراکنش ممنوع است؛ به‌جای آن بایگانی می‌شود.

## پشتیبان و بازیابی

فایل پشتیبان شامل نسخه schema، checksum، مجموعه کامل جدول‌ها و متادیتا است. بازیابی پس از
اعتبارسنجی کامل و در یک تراکنش انجام می‌شود تا داده نیمه‌بازیابی‌شده باقی نماند.
