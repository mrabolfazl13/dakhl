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
11. کپی خروجی‌ها به `dist/` و تولید `SHA256SUMS.txt`
12. آپلود آرتیفکت‌ها

## آرتیفکت‌ها

| نام | مسیر داخلی | توضیح |
| --- | --- | --- |
| `dakhlo-kharj-debug-apk` | `dist/dakhlo-kharj-debug.apk` | APK دیباگ |
| `dakhlo-kharj-release-apk` | `dist/dakhlo-kharj-release.apk` | APK ریلیز |
| `dakhlo-kharj-release-aab` | `dist/dakhlo-kharj-release.aab` | AAB ریلیز |
| `dakhlo-kharj-checksums` | `dist/SHA256SUMS.txt` | checksum |
| `test-reports` | گزارش تست و lint | در صورت وجود |

## امضا

- اگر Secretsهای `SIGNING_STORE_FILE`، `SIGNING_STORE_PASSWORD`، `SIGNING_KEY_ALIAS` و
  `SIGNING_KEY_PASSWORD` تنظیم شوند، ریلیز با کلید اختصاصی امضا می‌شود.
- در غیر این صورت ریلیز با کلید دیباگ امضا می‌شود (برای نصب آزمایشی و انتشار داخلی مناسب است،
  برای Google Play باید کلید اختصاصی تنظیم شود).

## آخرین وضعیت

| مورد | مقدار |
| --- | --- |
| Run ID | 45921933460 (دستی، در حال اصلاح) |
| شاخه | master |
| نتیجه | در حال تکمیل — پس از اجرای سبز به‌روزرسانی می‌شود |

## نحوه دریافت آرتیفکت‌ها

```bash
gh run download <RUN_ID> -D artifacts
```

سپس checksumها را بررسی کنید:

```bash
cd artifacts/dakhlo-kharj-checksums
sha256sum -c SHA256SUMS.txt
```
