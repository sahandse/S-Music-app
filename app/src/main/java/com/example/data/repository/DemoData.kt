package com.example.data.repository

import com.example.data.network.SubsonicAlbum
import com.example.data.network.SubsonicArtist
import com.example.data.network.SubsonicSong

object DemoData {
    val artists = listOf(
        SubsonicArtist(
            id = "demo_art_1",
            name = "همایون شجریان",
            coverArt = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
            albumCount = 2
        ),
        SubsonicArtist(
            id = "demo_art_2",
            name = "محسن یگانه",
            coverArt = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
            albumCount = 1
        ),
        SubsonicArtist(
            id = "demo_art_3",
            name = "یاس",
            coverArt = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400",
            albumCount = 1
        )
    )

    val albums = listOf(
        SubsonicAlbum(
            id = "demo_alb_1",
            name = "ناشکیبا",
            artist = "همایون شجریان",
            artistId = "demo_art_1",
            coverArt = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
            songCount = 3,
            duration = 582,
            year = 1382,
            genre = "سنتی"
        ),
        SubsonicAlbum(
            id = "demo_alb_2",
            name = "رگ خواب",
            artist = "محسن یگانه",
            artistId = "demo_art_2",
            coverArt = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=400",
            songCount = 2,
            duration = 422,
            year = 1389,
            genre = "پاپ"
        ),
        SubsonicAlbum(
            id = "demo_alb_3",
            name = "هویت من",
            artist = "یاس",
            artistId = "demo_art_3",
            coverArt = "https://images.unsplash.com/photo-1487180142328-054b783fc471?w=400",
            songCount = 2,
            duration = 380,
            year = 1385,
            genre = "رپ"
        )
    )

    val songs = listOf(
        SubsonicSong(
            id = "demo_song_1",
            title = "تصنیف ناشکیبا",
            album = "ناشکیبا",
            artist = "همایون شجریان",
            albumId = "demo_alb_1",
            artistId = "demo_art_1",
            coverArt = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
            duration = 240,
            size = 4800000,
            suffix = "mp3",
            contentType = "audio/mpeg"
        ),
        SubsonicSong(
            id = "demo_song_2",
            title = "زهر شیرین",
            album = "ناشکیبا",
            artist = "همایون شجریان",
            albumId = "demo_alb_1",
            artistId = "demo_art_1",
            coverArt = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
            duration = 180,
            size = 3600000,
            suffix = "mp3",
            contentType = "audio/mpeg"
        ),
        SubsonicSong(
            id = "demo_song_3",
            title = "سکوت",
            album = "رگ خواب",
            artist = "محسن یگانه",
            albumId = "demo_alb_2",
            artistId = "demo_art_2",
            coverArt = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=400",
            duration = 210,
            size = 4200000,
            suffix = "mp3",
            contentType = "audio/mpeg"
        ),
        SubsonicSong(
            id = "demo_song_4",
            title = "بمون",
            album = "رگ خواب",
            artist = "محسن یگانه",
            albumId = "demo_alb_2",
            artistId = "demo_art_2",
            coverArt = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=400",
            duration = 195,
            size = 3900000,
            suffix = "mp3",
            contentType = "audio/mpeg"
        ),
        SubsonicSong(
            id = "demo_song_5",
            title = "هویت من",
            album = "هویت من",
            artist = "یاس",
            albumId = "demo_alb_3",
            artistId = "demo_art_3",
            coverArt = "https://images.unsplash.com/photo-1487180142328-054b783fc471?w=400",
            duration = 190,
            size = 3800000,
            suffix = "mp3",
            contentType = "audio/mpeg"
        )
    )

    // Royalty free standard audio links for flawless streaming playback inside the Android emulator
    fun getDemoAudioUrl(songId: String): String {
        return when (songId) {
            "demo_song_1" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            "demo_song_2" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            "demo_song_3" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
            "demo_song_4" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
            "demo_song_5" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
            else -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        }
    }

    val demoLyrics = mapOf(
        "demo_song_1" to """[00:01.00] به نام آفریدگار هنر و موسیقی
[00:08.00] تصنیف زیبای ناشکیبا اثری جاودانه از همایون شجریان
[00:15.00] شعر از سعدی شیرازی بزرگوار
[00:25.00] ای ساربان آهسته ران کآرام جانم می‌رود
[00:40.00] وآن دل که با خود داشتم با دلستانم می‌رود
[00:55.00] من مانده‌ام مهجور از او بیچاره و رنجور از او
[01:10.00] گویی که نیشی دور از او در استخوانم می‌رود
[01:25.00] گفتم به نیرنگ و فسون پنهان کنم ریش درون
[01:40.00] پنهان نمی‌ماند که خون بر آستانم می‌رود
[01:55.00] محمل بدار ای ساربان تندی مکن با کاروان
[02:10.00] کز عشق آن سرو روان گویی روانم می‌رود""",
        "demo_song_3" to """[00:01.00] سکوت قلبت رو بشکن و برگرد
[00:10.00] نذار این فاصله بیشتر از این شه
[00:20.00] محسن یگانه - آلبوم رگ خواب
[00:30.00] روزای سخت رفتن رو به یاد آر
[00:40.00] صدای خنده‌هامون تو خیابون
[00:50.00] سکوت قلبت رو بشکن و برگرد
[01:05.00] نذار این فاصله بیشتر از این شه"""
    )
}
