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

package com.russoviller.cryogenic.game.download.assets.platform

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 可用的资源搜索平台
 */
@Serializable
@Parcelize
enum class Platform(val displayName: String, val isEnabled: Boolean): Parcelable {
    @SerialName("CURSEFORGE")
    CURSEFORGE("CurseForge", true),
    @SerialName("MODRINTH")
    MODRINTH("Modrinth", true);

    companion object {
        val available: List<Platform> get() = entries.filter { it.isEnabled }
    }
}
