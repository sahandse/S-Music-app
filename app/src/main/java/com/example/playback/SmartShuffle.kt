package com.example.playback

import com.example.data.network.SubsonicSong

object SmartShuffle {
    fun reorder(
        queue: List<SubsonicSong>,
        favoriteIds: Set<String>,
        recentIds: List<String>
    ): List<SubsonicSong> {
        if (queue.size <= 1) return queue

        val recentSet = recentIds.take(20).toSet()
        val tier0 = mutableListOf<SubsonicSong>()
        val tier1 = mutableListOf<SubsonicSong>()
        val tier2 = mutableListOf<SubsonicSong>()

        for (song in queue) {
            when {
                song.id in favoriteIds -> tier0.add(song)
                song.id in recentSet -> tier1.add(song)
                else -> tier2.add(song)
            }
        }

        tier0.shuffle(java.util.Random())
        tier1.shuffle(java.util.Random())
        tier2.shuffle(java.util.Random())

        val result = mutableListOf<SubsonicSong>()
        val tiers = listOf(tier0, tier1, tier2)
        val counts = tiers.map { (it.size + 2) / 3 }.toMutableList()
        val pointers = IntArray(3) { 0 }

        while (result.size < queue.size) {
            for (tier in 0..2) {
                if (result.size >= queue.size) break
                if (pointers[tier] < tiers[tier].size && counts[tier] > 0) {
                    result.add(tiers[tier][pointers[tier]++])
                    counts[tier]--
                }
            }
        }

        return result
    }
}
