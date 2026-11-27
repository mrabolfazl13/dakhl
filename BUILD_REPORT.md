# گزارش بیلد — دخل‌وخرج

## قواعد

بیلد و تست فقط در GitHub Actions انجام می‌شود. هیچ Gradle محلی اجرا نمی‌شود.

Workflow: `.github/workflows/android-ci.yml` — نام: Android CI

## محیط

| مورد | مقدار |
| --- | --- |
| Runner | ubuntu-latest |
| JDK | Temurin 17 |
| Gradle | 8.11.1 |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose Compiler | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| versionCode / versionName | 1 / 1.0.0 |

## مراحل

1. Checkout
2. نصب JDK 17
3. نصب Android SDK (platform 35، build-tools 35.0.0، platform-tools)
4. راه‌اندازی Gradle با cache
5. `gradle projects`
6. `gradle testDebugUnitTest`
7. `gradle lintDebug`
8. `gradle assembleDebug`
9. `gradle assembleRelease`
10. `gradle bundleRelease`
11. بررسی امضای APK و AAB با `apksigner`/`jarsigner`، چاپ manifest و رد کردن مجوز INTERNET
12. کپی خروجی‌ها به `dist/` و تولید `SHA256SUMS.txt`
13. آپلود آرتیفکت‌ها

## آرتیفکت‌ها

| نام | مسیر داخلی | حجم | توضیح |
| --- | --- | --- | --- |
| `dakhlo-kharj-debug-apk` | `dist/dakhlo-kharj-debug.apk` | ۲۰٬۸۷۲٬۵۳۸ بایت | APK دیباگ |
| `dakhlo-kharj-release-apk` | `dist/dakhlo-kharj-release.apk` | ۱۳٬۹۲۷٬۲۹۷ بایت | APK ریلیز |
| `dakhlo-kharj-release-aab` | `dist/dakhlo-kharj-release.aab` | ۱۳٬۸۳۳٬۱۳۳ بایت | AAB ریلیز |
| `dakhlo-kharj-checksums` | `dist/SHA256SUMS.txt` | — | checksum |
| `test-reports` | گزارش تست و lint | — | ۹۷ تست موفق، lint با ۰ خطا |

### SHA-256

```text
d8338003bcb69a8625996bca2fa01a2d14637912ac56c1fd74d5f710b40e662a  dakhlo-kharj-debug.apk
bb44a9a893880d4a36aff4d57e3e4a25874b157417a82577ba047e00a49fbfce  dakhlo-kharj-release.apk
7f08e66569ddb348d6ddedcd2401ebf854d7c70237369bd0688348a94893adf7  dakhlo-kharj-release.aab
```

این مقادیر با `Get-FileHash` روی فایل‌های دانلودشده بازمحاسبه شدند و با `SHA256SUMS.txt` اجرا یکسان‌اند.

## امضا

- Secretsهای `SIGNING_STORE_FILE`، `SIGNING_STORE_PASSWORD`، `SIGNING_KEY_ALIAS` و
  `SIGNING_KEY_PASSWORD` در این مخزن تنظیم نشده‌اند؛ بنابراین خروجی‌های ریلیز با کلید دیباگ امضا می‌شوند.
- APK ریلیز با APK Signature Scheme v2 تأیید شد:
  `Signer #1 certificate DN: C=US, O=Android, CN=Android Debug`.
- AAB ریلیز با `jarsigner -verify` به‌عنوان jar امضاشده تأیید شد.
- برای انتشار در Google Play باید کلید ریلیز اختصاصی در Secrets قرار گیرد و `versionCode` افزایش یابد.

## وضعیت manifest

```text
package: name='ir.dakhlokharj' versionCode='1' versionName='1.0.0'
sdkVersion:'26'
targetSdkVersion:'35'
```

مجوز `android.permission.INTERNET` وجود ندارد؛ فقط
`POST_NOTIFICATIONS`، `USE_BIOMETRIC`، `VIBRATE` و `RECEIVE_BOOT_COMPLETED` درخواست می‌شوند.

## کیفیت کد

- تست واحد: ۹۷ تست، ۰ شکست، ۰ رد‌شده.
- lint: ۰ خطا و ۲۰ هشدار (عمدتاً وابستگی‌های قدیمی‌تر و targetSdk 35 در برابر ۳۶). این هشدارها
  برای پایداری نسخه ۱.۰.۰ عمداً بدون ارتقای ناگهانی وابستگی‌ها باقی مانده‌اند.
- Room schema در `app/schemas` نگهداری می‌شود.

## آخرین وضعیت

| مورد | مقدار |
| --- | --- |
| Run ID | 45925225154 |
| شاخه | master |
| Commit | c00575b264a5a8febd9334714e94005292a041be |
| نتیجه | موفق (۵ دقیقه و ۴۷ ثانیه) |
| تاریخ | ۲۰۲۶-۱۱-۲۷ |

## نحوه دریافت آرتیفکت‌ها

```bash
gh run download 45925225154 -D artifacts
```

checksumها را با فایل دانلودشده مقایسه کنید:

```powershell
Get-ChildItem artifacts -Recurse -Include *.apk,*.aab |
  ForEach-Object { (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower() + "  " + $_.Name }
```

## یادداشت

آرتیفکت‌های CI در مخزن commit نمی‌شوند (`/artifacts/` در `.gitignore`). منبع حقیقت، اجرای Actions است.