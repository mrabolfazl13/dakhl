# معماری — دخل‌وخرج

## نمای کلی

یک ماژول اندروید (`:app`) با مرزبندی منطقی:

```
core/        ابزارهای خالص Kotlin (پول، تقویم، قالب‌بندی)
domain/      مدل‌ها و موتورهای مالی بدون وابستگی به اندروید
data/        Room، DAOها، مخازن و Seed
backup/      رمزنگاری و پشتیبان‌گیری
security/    هش رمز
notifications/ WorkManager و کانال‌ها
export/      CSV و PDF گزارش‌ها با SAF
di/          AppContainer (DI دستی)
ui/          Compose: theme، components، screens، navigation، viewmodels
```

جهت وابستگی: `ui → domain ← data` و `data → domain`. موتورهای `domain` هیچ وابستگی اندرویدی ندارند و
روی JVM تست می‌شوند.

## لایه‌ها

### core
- `Money` (value class روی Long، ریال)
- `JalaliDate` و `JalaliCalendar`
- `PersianText` و `MoneyFormatter`

### domain
مدل‌های تغییرناپذیر: Account، Category، Transaction، Budget، Envelope، Debt، Receivable، Installment،
RecurringExpense، SavingsGoal، FinancialEvent، UserSettings، BackupMetadata.

موتورها:
- `ForecastEngine`
- `BudgetEngine`
- `HealthEngine`
- `InsightsEngine`
- `WhatIfEngine`
- `CalendarEngine`
- `ReportEngine`
- `InstallmentScheduler`
- `AlertEngine`

### data
- `Entities.kt` — ۱۶ موجودیت Room
- `Daos.kt` و `PlanningDaos.kt`
- `DkDatabase` نسخه ۱
- `Converters` برای `JalaliDate` (ذخیره به‌صورت epochDay) و `Money` (ذخیره ریال)
- مخازن: Account، Category، Transaction، Budget، Debt، Installment، Recurring، Savings، Event، Settings

### export
- `CsvExporter` — CSV با BOM برای Excel، شامل همه تراکنش‌ها.
- `PdfExporter` — PDF چندصفحه‌ای A4 با `PdfDocument`، `StaticLayout` راست‌به‌چپ و فونت Vazirmatn؛
  خلاصه دوره، تفکیک دسته و همه تراکنش‌های بازه.
- نوشتن روی URI انتخابی کاربر با `ContentResolver` انجام می‌شود؛ مجوز ذخیره‌سازی درخواست نمی‌شود.

### ui
- `theme/` — رنگ، تایپوگرافی Vazirmatn، فرمترها، آیکون‌ها
- `components/` — DkScreen، DkCard، Sheetها، نمودارها، کارت‌های مالی
- `screens/` — ۲۸ صفحه
- `viewmodel/` — Overview، Transactions، Planning، Settings
- `navigation/DkNavHost.kt`

## MVVM و جریان داده

ViewModelها از مخازن `Flow`ها را جمع می‌کنند و به `StateFlow` تبدیل می‌کنند. صفحه‌ها با
`collectAsStateWithLifecycle` مصرف می‌کنند. عملیات نوشتن در `viewModelScope` اجرا و نتیجه به‌صورت
`Result` به UI برگردانده می‌شود.

`OverviewViewModel` وضعیت تجمیعی داشبورد را می‌سازد. `PlanningViewModel` عملیات برنامه‌ریزی
(حساب، بودجه، بدهی، قسط، اشتراک، پس‌انداز، رویداد) را انجام می‌دهد. `SettingsViewModel` تنظیمات،
دسته‌ها، پشتیبان و خروجی را مدیریت می‌کند.

## مدل پول

```kotlin
@JvmInline value class Money(val rial: Long)
```

۱ تومان = ۱۰ ریال. هیچ `Float` یا `Double` برای پول استفاده نمی‌شود. UI مقدار تومان را با
`Money.toman(value)` تبدیل می‌کند.

## اتمی بودن

همه تغییرات مالی در `RoomDatabase.withTransaction` انجام می‌شوند:
- ایجاد تراکنش: ثبت تراکنش + به‌روزرسانی موجودی حساب(ها)
- ویرایش: برگشت اثر قبلی + اعمال اثر جدید
- حذف: برگشت اثر و حذف رکورد
- پرداخت بدهی/طلب/قسط: ثبت پرداخت + تراکنش + به‌روزرسانی موجودی
- واریز/برداشت پس‌انداز: به‌روزرسانی هدف + تراکنش اختیاری

## امنیت

- رمز با Salt تصادفی ۱۶ بایتی و SHA-256 ذخیره می‌شود؛ رمز خام هرگز ذخیره نمی‌شود.
- پشتیبان رمزدار: PBKDF2-HMAC-SHA256 با ۱۲۰٬۰۰۰ تکرار، AES-256-GCM، Salt و IV تصادفی.
- `FLAG_SECURE` برای جلوگیری از اسکرین‌شات.
- هیچ مجوز اینترنتی در Manifest وجود ندارد.

## وابستگی‌های اصلی

AGP 8.7.3، Kotlin 2.0.21، Compose BOM 2024.09.03، Room 2.6.1، DataStore 1.1.1، WorkManager 2.9.1،
Biometric 1.1.0، Navigation Compose 2.8.4، kotlinx-serialization-json 1.7.3.