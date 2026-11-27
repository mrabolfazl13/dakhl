# سیستم طراحی — دخل‌وخرج

## فلسفه

یک تجربه مالی آرام، لوکس و فارسی. پس‌زمینه تیره سرمه‌ای/روشن گرم، سبز زمردی به‌عنوان رنگ اصلی و طلایی
ملایم به‌عنوان لهجه. بدون شلوغی، بدون رنگ‌های تصادفی، با تأکید بر خوانایی اعداد و راست‌به‌چپ بودن کامل.

## رنگ‌ها

| نقش | روشن | تاریک |
| --- | --- | --- |
| پس‌زمینه | WarmWhite | MidnightNavy |
| سطح | White | SurfaceDark |
| اصلی | EmeraldDeep | Emerald |
| ثانویه | MutedGold تیره | MutedGold |
| متن | Charcoal | OnDark |
| خطا | Danger | Danger |

رنگ دسته‌ها و حساب‌ها از پالت ثابت هشت‌رنگ انتخاب می‌شود و در مدل ذخیره می‌گردد.

## تایپوگرافی

- فونت: Vazirmatn (Regular / Medium / Bold) — مجوز SIL OFL 1.1
- عنوان صفحه: `headlineMedium`
- عنوان کارت: `titleMedium`/`titleLarge`
- متن اصلی: `bodyLarge`/`bodyMedium`
- توضیح کوچک: `labelSmall` با رنگ `onSurfaceVariant`
- اعداد همیشه با ارقام فارسی و جداکننده هزارگان نمایش داده می‌شوند.

## فاصله‌گذاری و شکل

- فاصله افقی صفحه: ۱۶dp
- فاصله بین کارت‌ها: ۱۰ تا ۱۲dp
- پدینگ داخلی کارت: ۱۸dp (کارت‌های فشرده ۱۴dp)
- شعاع کارت: `shapes.large` (حدود ۲۰dp)
- شعاع دکمه: `shapes.large`
- حداقل ارتفاع دکمه اصلی: ۵۲dp

## کامپوننت‌های پایه

- `DkScreen` — پس‌زمینه گرادیانی، نوار بالای RTL، دکمه بازگشت، FAB
- `DkCard` — کارت گرادیانی با حاشیه نازک و حالت کلیک
- `PrimaryButton` / `SecondaryButton`
- `DkTextField`
- `SegmentedControl`
- `StatTile`، `InfoRow`، `Pill`
- `SectionHeader` با اکشن متنی
- `DkBottomSheet`، `SheetTitle`
- `AmountInputSheet` — صفحه‌کلید عددی مبلغ
- `DatePickerSheet` — انتخاب تاریخ جلالی
- `OptionPickerSheet` / `PickerRow`
- `DkChoiceChip` — تراشه فیلتر
- `ConfirmDialog` / `TextPromptDialog`

## نمودارها

- `CashFlowChart` — میله‌های درآمد/هزینه شش ماه
- `ProjectionChart` — خط روند موجودی
- `DonutChart` — سهم دسته‌ها
- `HealthRing` — امتیاز سلامت
- `DkProgressBar`، `MiniBars`، `LegendDot`

همه نمودارها با Canvas بومی Compose کشیده می‌شوند و وابستگی خارجی ندارند.

## راست‌به‌چپ

- `LocalLayoutDirection = Rtl` در ریشه برنامه
- `android:supportsRtl="true"`
- آیکون بازگشت در سمت راست، فلش‌ها با `ChevronLeft`
- ترتیب ستون‌ها و نمودارها از راست به چپ
- اعداد و تاریخ‌ها همیشه فارسی

## حالت‌ها

- روشن، تاریک و هماهنگ با سیستم
- پنهان‌کردن موجودی با `LocalHideBalances`
- تغییر واحد نمایش تومان/ریال با `LocalCurrencyUnit`

## اصول دسترس‌پذیری

- حداقل ناحیه لمسی ۴۸dp
- `contentDescription` برای آیکون‌های تعاملی
- کنتراست متن اصلی روی سطح حداقل ۴.۵:۱
- عدم اتکا به رنگ تنها برای انتقال معنا (متن وضعیت در کنار رنگ)
