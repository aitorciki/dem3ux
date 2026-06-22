package net.aitorciki.dem3ux.bridge

fun interface PersistedTreeUrisProvider {
    fun persistedReadableTreeUris(): List<String>
}
