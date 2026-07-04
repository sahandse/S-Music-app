import { getSettings } from './settings'

export type Lang = 'fa' | 'en'

// key -> [fa, en]
const DICT: Record<string, [string, string]> = {
  // nav
  'nav.home': ['خانه', 'Home'],
  'nav.albums': ['آلبوم‌ها', 'Albums'],
  'nav.artists': ['هنرمندان', 'Artists'],
  'nav.playlists': ['پلی‌لیست‌ها', 'Playlists'],
  'nav.starred': ['پسندیده‌ها', 'Favorites'],
  'nav.search': ['جستجو', 'Search'],
  'nav.catalog': ['کاتالوگ آنلاین', 'Online Catalog'],
  'nav.mysongs': ['آهنگ‌های من', 'My Songs'],
  'nav.local': ['آهنگ‌های محلی', 'Local Files'],
  'nav.settings': ['تنظیمات', 'Settings'],
  'nav.logout': ['خروج', 'Log out'],

  // common
  'common.loading': ['در حال بارگذاری...', 'Loading…'],
  'common.searching': ['در حال جستجو...', 'Searching…'],
  'common.noSongs': ['آهنگی نیست', 'No songs'],
  'common.noResults': ['نتیجه‌ای پیدا نشد', 'No results found'],
  'common.play': ['پخش', 'Play'],
  'common.playAll': ['پخش همه', 'Play all'],
  'common.download': ['دانلود روی کامپیوتر', 'Download to computer'],
  'common.like': ['پسندیدن', 'Like'],
  'common.songs': ['آهنگ', 'songs'],

  // player
  'player.nothing': ['چیزی در حال پخش نیست', 'Nothing is playing'],
  'player.nothing2': ['چیزی پخش نمی‌شود', 'Nothing playing'],
  'player.playPause': ['پخش/توقف', 'Play/Pause'],
  'player.next': ['بعدی', 'Next'],
  'player.prev': ['قبلی', 'Previous'],
  'player.shuffle': ['تصادفی', 'Shuffle'],
  'player.repeat': ['تکرار', 'Repeat'],
  'player.queue': ['صف پخش', 'Queue'],
  'player.emptyQueue': ['صف خالی است', 'Queue is empty'],
  'player.removeFromQueue': ['حذف از صف', 'Remove from queue'],
  'player.addToQueue': ['افزودن به صف', 'Add to queue'],
  'player.dragReorder': ['کشیدن برای جابجایی', 'Drag to reorder'],
  'player.fullscreen': ['نمای تمام‌صفحه', 'Full-screen view'],
  'player.fullscreenLyrics': ['تمام‌صفحه + متن آهنگ', 'Full screen + lyrics'],
  'player.mini': ['پخش‌کننده کوچک', 'Mini player'],
  'player.restore': ['بازگشت به حالت عادی', 'Restore window'],
  'player.close': ['بستن', 'Close'],
  'player.noLyrics': ['متن آهنگ یافت نشد', 'No lyrics found'],
  'player.remove': ['حذف', 'Remove'],

  // home
  'home.newest': ['تازه‌ترین‌ها', 'Newest'],
  'home.frequent': ['پرشنیده‌ها', 'Most played'],
  'home.random': ['آهنگ‌های تصادفی', 'Random songs'],

  // albums / artists / album
  'albums.notFound': ['آلبوم پیدا نشد', 'Album not found'],
  'album.kind': ['آلبوم', 'Album'],
  'artists.notFound': ['هنرمند پیدا نشد', 'Artist not found'],
  'artist.kind': ['هنرمند', 'Artist'],
  'artists.filter': ['فیلتر هنرمندان...', 'Filter artists…'],

  // playlists
  'playlists.none': ['پلی‌لیستی وجود ندارد', 'No playlists'],
  'playlist.kind': ['پلی‌لیست', 'Playlist'],
  'playlist.notFound': ['پلی‌لیست پیدا نشد', 'Playlist not found'],
  'playlist.exportM3u': ['خروجی M3U', 'Export M3U'],
  'playlist.downloadM3u': ['دانلود M3U', 'Download M3U'],

  // search
  'search.placeholder': ['آهنگ، آلبوم یا هنرمند...', 'Song, album or artist…'],
  'search.songs': ['آهنگ‌ها', 'Songs'],
  'search.albums': ['آلبوم‌ها', 'Albums'],
  'search.artists': ['هنرمندان', 'Artists'],

  // starred
  'starred.likedAlbums': ['آلبوم‌های پسندیده', 'Liked albums'],
  'starred.noLiked': ['آهنگ پسندیده‌ای نیست', 'No liked songs'],

  // login
  'login.subtitle': ['برای ورود، نام کاربری و رمز خود را وارد کنید', 'Enter your username and password to sign in'],
  'login.username': ['نام کاربری', 'Username'],
  'login.password': ['رمز عبور', 'Password'],
  'login.connect': ['ورود', 'Sign in'],
  'login.connecting': ['در حال اتصال...', 'Connecting…'],
  'login.needUsername': ['نام کاربری لازم است', 'Username is required'],
  'login.connectFail': ['اتصال ناموفق: ', 'Connection failed: '],
  'login.wrongCreds': ['نام کاربری یا رمز اشتباه است', 'Wrong username or password'],
  'login.noAccount': ['حساب ندارید؟', "Don't have an account?"],
  'login.signup': ['ثبت‌نام از طریق تلگرام', 'Sign up via Telegram'],

  // settings
  'set.title': ['تنظیمات', 'Settings'],
  'set.appearance': ['ظاهر', 'Appearance'],
  'set.language': ['زبان', 'Language'],
  'set.theme': ['پوسته', 'Theme'],
  'set.dark': ['تیره', 'Dark'],
  'set.light': ['روشن', 'Light'],
  'set.accent': ['رنگ لهجه', 'Accent color'],
  'set.customColor': ['رنگ دلخواه', 'Custom color'],
  'set.playback': ['پخش', 'Playback'],
  'set.fadeOnPause': ['محو تدریجی هنگام توقف/پخش', 'Fade on pause/play'],
  'set.crossfade': ['محو بین آهنگ‌ها (Crossfade)', 'Crossfade between tracks'],
  'set.eq': ['اکولایزر', 'Equalizer'],
  'set.enabled': ['فعال', 'Enabled'],
  'set.download': ['دانلود', 'Downloads'],
  'set.noFolder': ['پوشه‌ای انتخاب نشده', 'No folder selected'],
  'set.pickFolder': ['انتخاب پوشه', 'Choose folder'],
  'set.lastfm': ['Last.fm (اسکروبل)', 'Last.fm (scrobbling)'],
  'set.lastfmDesc': [
    'کلید API و Secret را از last.fm/api بگیرید، سپس با حساب Last.fm خود وارد شوید.',
    'Get an API key + secret from last.fm/api, then sign in with your Last.fm account.'
  ],
  'set.apiKey': ['API Key', 'API Key'],
  'set.apiSecret': ['API Secret', 'API Secret'],
  'set.lastfmUser': ['نام کاربری Last.fm', 'Last.fm username'],
  'set.lastfmPass': ['رمز Last.fm', 'Last.fm password'],
  'set.connect': ['اتصال', 'Connect'],
  'set.connectedTo': ['متصل به: ', 'Connected as: '],
  'set.disconnect': ['قطع اتصال', 'Disconnect'],
  'set.updates': ['به‌روزرسانی', 'Updates'],
  'set.currentVersion': ['نسخه فعلی: ', 'Current version: '],
  'set.openReleases': ['دریافت آخرین نسخه', 'Get latest version'],
  'set.releasesHint': [
    'برای به‌روزرسانی، آخرین نصب‌کننده را از صفحه‌ی ریلیز دانلود و نصب کنید.',
    'To update, download and run the latest installer from the releases page.'
  ],
  'set.updateAvailable': [' — نسخه {v} موجود است', ' — version {v} available'],
  'set.upToDate': [' — به‌روز هستید', ' — up to date'],
  'set.downloading': [' — دانلود {p}%', ' — downloading {p}%'],
  'set.readyInstall': [' — آماده نصب', ' — ready to install'],
  'set.checkFailed': [' — بررسی ناموفق', ' — check failed'],
  'set.downloadBtn': ['دانلود', 'Download'],
  'set.installBtn': ['نصب و راه‌اندازی مجدد', 'Install & restart'],
  'set.checkBtn': ['بررسی به‌روزرسانی', 'Check for updates'],
  'eq.neutral': ['خنثی', 'Flat'],
  'eq.bass': ['بیس‌دار', 'Bass'],
  'eq.pop': ['پاپ', 'Pop'],
  'eq.rock': ['راک', 'Rock'],
  'eq.clear': ['صدای واضح', 'Vocal'],

  // last.fm errors
  'lfm.needKeys': ['کلید API و Secret را وارد کنید', 'Enter API key and secret'],
  'lfm.loginFail': ['ورود ناموفق', 'Login failed'],
  'lfm.connected': ['متصل شد: ', 'Connected: '],
  'lfm.error': ['خطا', 'Error'],

  // local
  'local.title': ['آهنگ‌های محلی', 'Local Files'],
  'local.addFile': ['افزودن فایل', 'Add file'],
  'local.empty': ['فایل صوتی از کامپیوتر خود اضافه کنید (MP3, FLAC, ...).', 'Add audio files from your computer (MP3, FLAC, …).'],
  'local.desktopOnly': ['این بخش فقط در نسخه دسکتاپ کار می‌کند.', 'This section only works in the desktop app.'],
  'local.fileSource': ['فایل محلی', 'Local file'],

  // album sort
  'sort.newest': ['تازه‌ترین', 'Newest'],
  'sort.frequent': ['پرشنیده', 'Most played'],
  'sort.recent': ['اخیراً پخش‌شده', 'Recently played'],
  'sort.alpha': ['الفبا', 'A–Z'],
  'sort.random': ['تصادفی', 'Random'],
  'album.albums': ['آلبوم', 'albums'],
  'album.addQueue': ['به صف', 'To queue'],
  'artist.topPlay': ['پخش برترین‌ها', 'Play top tracks'],
  'artist.topSongs': ['برترین آهنگ‌ها', 'Top songs'],

  // catalog
  'catalog.title': ['کاتالوگ آنلاین', 'Online Catalog'],
  'catalog.refresh': ['بروزرسانی', 'Refresh'],
  'catalog.subtitle': ['آهنگ‌های اشتراکی که ادمین در کانال منتشر می‌کند', 'Shared songs posted by the admin'],
  'catalog.empty': ['کاتالوگ هنوز خالی است', 'The catalog is still empty'],

  // mysongs
  'my.title': ['آهنگ‌های من', 'My Songs'],
  'my.desc': [
    'کاور، نام هنرمند و لینک مستقیم آهنگ را وارد کنید تا به برنامه اضافه شود. لینک باید مستقیم به فایل صوتی (mp3، m4a، ...) اشاره کند.',
    'Add a cover, artist and a direct audio link. The link must point straight to an audio file (mp3, m4a, …).'
  ],
  'my.addSong': ['افزودن آهنگ', 'Add song'],
  'my.bulkImport': ['وارد کردن گروهی (JSON)', 'Bulk import (JSON)'],
  'my.import': ['وارد کردن', 'Import'],
  'my.list': ['لیست', 'List'],
  'my.empty': ['هنوز آهنگی اضافه نکرده‌اید', "You haven't added any songs yet"],

  // catalog / mysongs
  'catalog.error': ['خطا در دریافت کاتالوگ', 'Failed to load catalog'],
  'my.songName': ['نام آهنگ', 'Song title'],
  'my.artistName': ['نام هنرمند', 'Artist name'],
  'my.directLink': ['لینک مستقیم آهنگ', 'Direct audio link'],
  'my.coverLink': ['لینک کاور (URL تصویر)', 'Cover URL (image)'],
  'my.minRequired': ['حداقل نام آهنگ و لینک مستقیم لازم است', 'At least a title and a direct link are required'],
  'my.added': ['✓ اضافه شد', '✓ Added'],
  'my.jsonArray': ['باید یک آرایه JSON باشد', 'Must be a JSON array'],
  'my.importErr': ['خطا در وارد کردن: ', 'Import error: '],
  'my.listImported': ['✓ لیست وارد شد', '✓ List imported']
}

export function getLang(): Lang {
  return getSettings().lang
}

export function t(key: string, vars?: Record<string, string | number>): string {
  const entry = DICT[key]
  let s = entry ? entry[getLang() === 'en' ? 1 : 0] : key
  if (vars) for (const k in vars) s = s.replace('{' + k + '}', String(vars[k]))
  return s
}

export function applyDir(lang: Lang = getLang()) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('dir', lang === 'en' ? 'ltr' : 'rtl')
  document.documentElement.setAttribute('lang', lang)
}
