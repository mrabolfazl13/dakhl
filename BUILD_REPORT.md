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
| versionCode / versionName | 2 / 1.1.0 |

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
| `dakhlo-kharj-debug-apk` | `dist/dakhlo-kharj-debug.apk` | ۲۱٬۰۳۷٬۷۱۵ بایت | APK دیباگ |
| `dakhlo-kharj-release-apk` | `dist/dakhlo-kharj-release.apk` | ۱۴٬۰۷۶٬۱۰۳ بایت | APK ریلیز |
| `dakhlo-kharj-release-aab` | `dist/dakhlo-kharj-release.aab` | ۱۳٬۹۷۱٬۷۴۲ بایت | AAB ریلیز |
| `dakhlo-kharj-checksums` | `dist/SHA256SUMS.txt` | — | checksum |
| `test-reports` | گزارش تست و lint | — | ۹۷ تست موفق، lint با ۰ خطا |

### SHA-256

```text
eb86277f180ef57e52191d329791c5a439524112bc476350e7c93e52064b2318  dakhlo-kharj-debug.apk
993df2aee21a935e06bdcb0ec69d201bcdc655b0b254438fbf2f9ae9fc584762  dakhlo-kharj-release.apk
9f27dfd6675c977bbcbe67d03af7127d127d749dda6263f73816feb1e0ab24fd  dakhlo-kharj-release.aab
```

این مقادیر با `Get-FileHash` روی فایل‌های دانلودشده بازمحاسبه شدند و با `SHA256SUMS.txt` اجرای
`46113088195` یکسان‌اند. این checksum‌ها به همان اجرا وابسته‌اند؛ چون امضای APK زمان‌دار است،
اجراهای بعدی حتی با کد یکسان checksum متفاوتی تولید می‌کنند. برای بازتولید همین فایل‌ها از همان
Run ID استفاده کنید.

## امضا

- Secretsهای `SIGNING_STORE_FILE`، `SIGNING_STORE_PASSWORD`، `SIGNING_KEY_ALIAS` و
  `SIGNING_KEY_PASSWORD` در این مخزن تنظیم نشده‌اند؛ بنابراین خروجی‌های ریلیز با کلید دیباگ امضا می‌شوند.
- APK ریلیز با APK Signature Scheme v2 تأیید شد:
  `Signer #1 certificate DN: C=US, O=Android, CN=Android Debug`.
- AAB ریلیز با `jarsigner -verify` به‌عنوان jar امضاشده تأیید شد.
- برای انتشار در Google Play باید کلید ریلیز اختصاصی در Secrets قرار گیرد و `versionCode` افزایش یابد.

## وضعیت manifest

```text
package: name='ir.dakhlokharj' versionCode='2' versionName='1.1.0'
sdkVersion:'26'
targetSdkVersion:'35'
```

مجوز `android.permission.INTERNET` وجود ندارد. مجوزهای نهایی: `POST_NOTIFICATIONS`،
`USE_BIOMETRIC`، `USE_FINGERPRINT`، `VIBRATE`، `RECEIVE_BOOT_COMPLETED`، `WAKE_LOCK`،
`ACCESS_NETWORK_STATE` و `FOREGROUND_SERVICE` (چند مورد آخر از کتابخانه Play In-App Update
و WorkManager می‌آیند) به‌همراه مجوز داخلی `ir.dakhlokharj.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

## کیفیت کد

- تست واحد: ۹۷ تست، ۰ شکست، ۰ رد‌شده.
- lint: ۰ خطا و ۲۱ هشدار (عمدتاً وابستگی‌های قدیمی‌تر و targetSdk 35 در برابر ۳۶). این هشدارها
  برای پایداری نسخه ۱.۱.۰ عمداً بدون ارتقای ناگهانی وابستگی‌ها باقی مانده‌اند.
- Room schema در `app/schemas` نگهداری می‌شود.

## وضعیت اجرای مرجع

| مورد | مقدار |
| --- | --- |
| Run ID | 46113088195 (مرجع آرتیفکت‌های این گزارش) |
| شاخه | master |
| Commit | 560e623ba93ec60ddd36fd3f3da4fa06af3cb4ca |
| نتیجه | موفق (۶ دقیقه و ۴ ثانیه) |
| تاریخ | ۲۰۲۶-۱۱-۲۸ |

## نحوه دریافت آرتیفکت‌ها

```bash
gh run download 46113088195 -D artifacts
```

checksumها را با فایل دانلودشده مقایسه کنید:

```powershell
Get-ChildItem artifacts -Recurse -Include *.apk,*.aab |
  ForEach-Object { (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower() + "  " + $_.Name }
```

## یادداشت

آرتیفکت‌های CI در مخزن commit نمی‌شوند (`/artifacts/` در `.gitignore`). منبع حقیقت، اجرای Actions است.