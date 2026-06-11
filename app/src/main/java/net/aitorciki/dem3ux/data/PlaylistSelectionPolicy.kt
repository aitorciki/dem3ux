package net.aitorciki.dem3ux.data

import net.aitorciki.dem3ux.m3u.M3uEntry

object PlaylistSelectionPolicy {
    fun selectedIndex(
        savedIndex: Int?,
        entries: List<M3uEntry>,
    ): Int? = savedIndex?.takeIf { index -> entries.any { entry -> entry.index == index } } ?: entries.firstOrNull()?.index
}
