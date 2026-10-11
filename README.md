<div align="center">

# 🎧 High Music
### موسیقی خودت، روی دستگاه‌های خودت

**Android · Android TV · Windows**

اپلیکیشن پخش و مدیریت موسیقی با رابط فارسی، تم‌های قابل انتخاب و پشتیبانی از سرورهای سازگار با Subsonic.

**A personal music experience across Android, Android TV and Windows.**

[امکانات](#-امکانات) · [نصب و اجرا](#-نصب-و-اجرا) · [اتصال به سرور](#-اتصال-به-سرور) · [English](#english)

</div>

---

## ✨ های موزیک چیه؟

High Music برای گوش‌دادن به آهنگ‌ها و مدیریت کتابخانه موسیقی ساخته شده؛ از گوشی تا تلویزیون و کامپیوتر. می‌تونی به سرور موسیقی خودت وصل بشی، آهنگ‌ها و آلبوم‌ها رو مرور کنی و تجربه پخش رو شخصی‌سازی کنی.

این پروژه هنوز در حال توسعه است. وجود قابلیت در کد لزوماً به معنی تست کامل یا انتشار نسخه پایدار آن نیست.

## 🎵 امکانات

- **پخش موسیقی:** پلیر تمام‌صفحه، مینی‌پلیر، صف پخش، Shuffle و Repeat.
- **کتابخانه:** نمایش آهنگ، آلبوم و هنرمند، پلی‌لیست‌ها و علاقه‌مندی‌ها.
- **جست‌وجو و کشف:** جست‌وجوی آهنگ و هنرمند، مرور کاتالوگ و بخش کشف موسیقی.
- **متن آهنگ:** نمایش Lyrics در صورت دردسترس‌بودن اطلاعات.
- **حالت آفلاین:** بخش دانلودها و پخش فایل‌های ذخیره‌شده با توجه به مجوزهای دستگاه.
- **شخصی‌سازی:** زبان فارسی/انگلیسی، حالت روشن/تاریک و رنگ‌های متنوع.
- **نسخه تلویزیون:** ناوبری متناسب با Android TV و پشتیبانی از تمرکز کنترل از راه دور.
- **نسخه ویندوز:** برنامه دسکتاپ مبتنی بر Electron، React و TypeScript.

برخی محتواها و امکانات آنلاین به سرور متصل و سرویس‌های مرتبط وابسته‌اند؛ برنامه همراه با کتابخانه ساختگی یا موسیقی آزمایشی ارائه نمی‌شود.

## 📲 نصب و اجرا

### اندروید | Android

1. از بخش [Releases](https://github.com/sahandse/S-Music-app/releases) یک فایل APK متناسب با نسخه دستگاهت تهیه کن، **در صورتی که نسخه منتشرشده و تأییدشده‌ای موجود باشد**.
2. APK را روی گوشی نصب کن و برنامه را باز کن.
3. سرور موسیقی‌ات را تنظیم و با حساب خودت وارد شو.
4. برای آهنگ‌های دستگاه یا اعلان‌ها، تنها در صورت نیاز مجوز مربوط را بده.

**ساخت از سورس:** پروژه را در Android Studio باز کن، JDK سازگار با Gradle پروژه (در حال حاضر Java 21) را تنظیم کن و برای اجرای دیباگ از `./gradlew :app:assembleDebug` استفاده کن. محیط ساخت به Android SDK و ابزارهای Gradle نیاز دارد.

### تلویزیون اندرویدی | Android TV

1. فایل APK سازگار با تلویزیون را از نسخه‌های منتشرشده دریافت و روی تلویزیون نصب کن.
2. برنامه را از بخش Apps باز کن.
3. به سرور موسیقی متصل شو و با کنترل تلویزیون میان بخش‌ها حرکت کن.

نسخه TV در همین پروژه اندروید تعریف شده است؛ سازگاری با همه تلویزیون‌ها هنوز به تست واقعی نیاز دارد.

### ویندوز | Windows

1. در صورت موجودبودن نسخه تأییدشده، فایل نصب یا نسخه Portable را از [Releases](https://github.com/sahandse/S-Music-app/releases) دریافت کن.
2. برنامه را اجرا کن و اتصال سرور را تنظیم کن.

**ساخت از سورس:**

```bash
cd desktop
npm install
npm run build
```

برای توسعه دسکتاپ، داخل پوشه `desktop` دستور `npm run dev` را اجرا کن. ساخت نسخه ویندوز با Electron Builder انجام می‌شود و بهتر است برای خروجی نهایی از محیط Windows استفاده شود.

## 🔗 اتصال به سرور

High Music برای مرور کتابخانه شخصی از سرورهای سازگار با **Subsonic API** مانند Navidrome پشتیبانی می‌کند. نشانی سرور، نام کاربری و رمز عبور خودت را در صفحه ورود وارد کن. سرور باید از دستگاهت قابل‌دسترسی باشد.

برای فایل‌های شخصی و دانلودها می‌توانی از بخش‌های مربوط به کتابخانه و دانلود استفاده کنی. بدون اتصال یا داده واقعی، اپلیکیشن محتوای جعلی نمایش نمی‌دهد.

## 🛠 فناوری‌ها

| بخش | فناوری |
| --- | --- |
| Android / Android TV | Kotlin, Jetpack Compose, Media3 |
| Windows | Electron, React, TypeScript, Vite |
| Build | Gradle, GitHub Actions, Electron Builder |
| Music server | Subsonic-compatible API |

## 🚧 وضعیت توسعه

- تغییرات رابط کاربری و رفع برخی باگ‌های اندروید و ویندوز در شاخه `main` ثبت شده است.
- ساخت خودکار Android و همگام‌سازی کاتالوگ در آخرین بررسی GitHub Actions **ناموفق** بودند؛ این وضعیت به معنی تأیید خطای کامپایل اپ نیست، زیرا اجراها پیش از دریافت Runner متوقف شده‌اند.
- تا پیش از Build موفق و تست روی دستگاه، نسخه‌ای را **پایدار و آماده انتشار** فرض نکنید.

وضعیت لحظه‌ای را در [Actions](https://github.com/sahandse/S-Music-app/actions) ببین.

## 💬 پشتیبانی و پیشنهادها

اگر باگ پیدا کردی یا ایده‌ای برای بهترشدن برنامه داری، در [Issues](https://github.com/sahandse/S-Music-app/issues) بنویس.

ارتباط: [تلگرام](https://t.me/sahandse)

---

## English

**High Music** is a personal music player for **Android, Android TV and Windows**, featuring library browsing, playlists, search, playback controls, lyrics when available, offline downloads and customizable appearance.

The Android application is built with **Kotlin and Jetpack Compose**; the Windows client uses **Electron, React and TypeScript**. For personal server libraries, it supports **Subsonic-compatible services**, including Navidrome.

**Getting started:** Check [Releases](https://github.com/sahandse/S-Music-app/releases) for available builds, install the appropriate application and sign in to your reachable music server. Releases are not guaranteed to be stable until build and device testing have passed.

**For developers:** Open the Android project in Android Studio, or run `cd desktop && npm install && npm run dev` for the desktop client. Build status is available in [GitHub Actions](https://github.com/sahandse/S-Music-app/actions).

Questions or bugs? Open an [issue](https://github.com/sahandse/S-Music-app/issues).
