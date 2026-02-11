package ru.fromchat.api

/**
 * In-memory cache for user profiles. Used to show profile immediately from cache
 * and reload in the background.
 */
object ProfileCache {
    private val cache = mutableMapOf<Int, UserProfile>()

    fun get(userId: Int): UserProfile? = cache[userId]

    fun put(profile: UserProfile) {
        cache[profile.id] = profile
    }
}
