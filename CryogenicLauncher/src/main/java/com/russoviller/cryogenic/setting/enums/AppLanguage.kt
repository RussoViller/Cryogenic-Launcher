/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.russoviller.cryogenic.setting.enums

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.russoviller.cryogenic.R

enum class AppLanguage(
    val tag: String,
    @param:StringRes val textRes: Int
) {
    FOLLOW_SYSTEM("", R.string.cryo_language_system),
    BRAZILIAN_PORTUGUESE("pt-BR", R.string.language_brazilian_portuguese),
    ENGLISH("en", R.string.language_english),
    SPANISH("es", R.string.language_spanish),
    RUSSIAN("ru", R.string.language_russian),
    JAPANESE("ja", R.string.language_japanese),
    VIETNAMESE("vi", R.string.language_vietnamese),
    SIMPLIFIED_CHINESE("zh-CN", R.string.language_simplified_chinese),
    TRADITIONAL_CHINESE("zh-TW", R.string.language_traditional_chinese),
    TURKISH("tr", R.string.language_turkish),
    INDONESIAN("id", R.string.language_indonesian),
    THAI("th", R.string.language_thai),
    GERMAN("de", R.string.language_german),
    FRENCH("fr", R.string.language_french),
    ITALIAN("it", R.string.language_italian),
    DUTCH("nl", R.string.language_dutch),
    POLISH("pl", R.string.language_polish),
    SWEDISH("sv", R.string.language_swedish),
    NORWEGIAN("nb", R.string.language_norwegian),
    FINNISH("fi", R.string.language_finnish),
    ICELANDIC("is", R.string.language_icelandic),
    KOREAN("ko", R.string.language_korean),
    ARABIC("ar", R.string.language_arabic),
    HINDI("hi", R.string.language_hindi)
}

fun applyLanguage(language: AppLanguage = AppLanguage.FOLLOW_SYSTEM) {
    val appLocale = if (language == AppLanguage.FOLLOW_SYSTEM) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(language.tag)
    }
    if (AppCompatDelegate.getApplicationLocales() != appLocale) {
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
