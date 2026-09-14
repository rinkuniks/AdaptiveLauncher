package com.adaptive.launcher.core.common

import java.text.Normalizer
import java.util.Locale

object AppLabelNormalizer {
    fun normalize(label: String): String {
        val nfkd = Normalizer.normalize(label, Normalizer.Form.NFD)
        return nfkd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    fun sectionFor(normalized: String): Char {
        val c = normalized.firstOrNull() ?: return '#'
        return when {
            c in 'a'..'z' -> c.uppercaseChar()
            c in '0'..'9' -> '#'
            else -> '#'
        }
    }
}
