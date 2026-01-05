package com.example.campergas.domain.model

import androidx.core.os.LocaleListCompat

/**
 * Represents the available application languages.
 *
 * Each language stores its BCP-47 language tag so it can be mapped directly to the
 * [androidx.core.os.LocaleListCompat] used by `AppCompatDelegate` for per-app language
 * management.
 */
enum class AppLanguage(val languageTag: String) {
    /** Follows the system language configuration. */
    SYSTEM(""),

    /** Spanish language resources. */
    SPANISH("es"),

    /** English language resources. */
    ENGLISH("en"),

    /** Catalan language resources. */
    CATALAN("ca");

    /**
     * Converts the enum value into a [LocaleListCompat] instance.
     */
    fun toLocaleList(): LocaleListCompat = if (languageTag.isEmpty()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageTag)
    }

    companion object {
        /**
         * Retrieves the enum value for the provided language tag.
         */
        fun fromLanguageTag(tag: String?): AppLanguage {
            if (tag.isNullOrEmpty()) {
                return SYSTEM
            }

            val normalizedTag = tag.lowercase()
            val languageCode = normalizedTag.substringBefore('-')

            return entries.firstOrNull {
                it != SYSTEM && it.languageTag.equals(languageCode, ignoreCase = true)
            } ?: SYSTEM
        }
    }
}

