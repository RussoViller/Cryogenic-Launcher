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

package com.russoviller.cryogenic.ui.control.gamepad

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class GamepadMappingList(
    val name: String,
    val list: MutableList<GamepadMapping>
) : Parcelable {
    /**
     * 手柄与键盘按键映射绑定
     */
    @IgnoredOnParcel
    private val allKeyMappings = mutableMapOf<Int, TargetKeys>()
    @IgnoredOnParcel
    private val allDpadMappings = mutableMapOf<DpadDirection, TargetKeys>()

    /**
     * 便于记录目标键盘映射的数据类
     */
    data class TargetKeys(
        val inGame: Set<String>,
        val inMenu: Set<String>
    ) {
        fun getKeys(isInGame: Boolean) = if (isInGame) inGame else inMenu
    }

    fun load() {
        allKeyMappings.clear()
        allDpadMappings.clear()

        list.forEach { mapping ->
            addInMappingsMap(mapping)
        }
    }

    private fun addInMappingsMap(mapping: GamepadMapping) {
        val target = TargetKeys(mapping.targetsInGame, mapping.targetsInMenu)
        mapping.dpadDirection?.let {
            allDpadMappings[it] = target
        } ?: run {
            allKeyMappings[mapping.key] = target
        }
    }

    /**
     * 重置手柄与键盘按键映射绑定
     */
    fun resetMapping(gamepadMap: GamepadMap, inGame: Boolean) =
        applyMapping(gamepadMap, inGame)

    /**
     * 为指定手柄映射设置目标键盘映射
     */
    fun saveMapping(gamepadMap: GamepadMap, targets: Set<String>, inGame: Boolean) =
        applyMapping(gamepadMap, inGame, customTargets = targets)

    /**
     * 保存或重置手柄与键盘按键映射绑定
     * @param gamepadMap 手柄映射对象
     * @param inGame 是否为游戏内映射（true 为游戏内，false 为菜单内）
     * @param customTargets 自定义目标键，为空则重置
     */
    private fun applyMapping(
        gamepadMap: GamepadMap,
        inGame: Boolean,
        customTargets: Set<String>? = null,
    ) {
        val dpad = gamepadMap.dpadDirection
        val isDpad = dpad != null
        val existing = if (isDpad) allDpadMappings[dpad] else allKeyMappings[gamepadMap.gamepad]

        val (targetsInGame, targetsInMenu) = if (inGame) {
            val newTargets = customTargets ?: gamepadMap.defaultKeysInGame
            newTargets to (existing?.inMenu ?: emptySet())
        } else {
            val newTargets = customTargets ?: gamepadMap.defaultKeysInMenu
            (existing?.inGame ?: emptySet()) to newTargets
        }

        val mapping = GamepadMapping(
            key = gamepadMap.gamepad,
            dpadDirection = dpad,
            targetsInGame = targetsInGame,
            targetsInMenu = targetsInMenu
        )
        addInMappingsMap(mapping)
        list.removeIf { mapping0 ->
            if (isDpad) {
                mapping0.dpadDirection == mapping.dpadDirection
            } else {
                mapping0.dpadDirection == null && mapping0.key == mapping.key
            }
        }
        list.add(mapping)
        save()
    }

    /**
     * 根据手柄按键键值获取对应的键盘映射代码
     * @return 若未找到，则返回null
     */
    fun findByCode(key: Int, inGame: Boolean) =
        allKeyMappings[key]?.getKeys(inGame)

    /**
     * 根据手柄方向键获取对应的键盘映射代码
     * @return 若未找到，则返回null
     */
    fun findByDpad(dir: DpadDirection, inGame: Boolean) =
        allDpadMappings[dir]?.getKeys(inGame)

    /**
     * 根据手柄映射获取对应的键盘映射代码
     * @return 若未找到，则返回null
     */
    fun findByMap(map: GamepadMap, inGame: Boolean) =
        (map.dpadDirection?.let { allDpadMappings[it] } ?: allKeyMappings[map.gamepad])
            ?.getKeys(inGame)

    /** Dpad pelo código de ligação (ver [dpadBindingCode]). */
    private fun dpadOf(code: Int): DpadDirection? = DpadDirection.entries.firstOrNull { dpadBindingCode(it) == code }

    /** Teclas ligadas a qualquer código (botões padrão, direcional ou extras). */
    fun findTargets(code: Int, inGame: Boolean): Set<String>? =
        dpadOf(code)?.let { allDpadMappings[it]?.getKeys(inGame) } ?: allKeyMappings[code]?.getKeys(inGame)

    /**
     * Liga teclas a um código qualquer (inclusive botões extras que não estão em [GamepadMap]).
     * O outro contexto (jogo/menu) é mantido.
     */
    fun saveTargets(code: Int, targets: Set<String>, inGame: Boolean) {
        GamepadMap.entries.firstOrNull { it.bindingCode() == code }?.let {
            saveMapping(it, targets, inGame)
            return
        }
        val dpad = dpadOf(code)
        val existing = dpad?.let { allDpadMappings[it] } ?: allKeyMappings[code]
        val mapping = GamepadMapping(
            key = if (dpad != null) -1 else code,
            dpadDirection = dpad,
            targetsInGame = if (inGame) targets else existing?.inGame ?: emptySet(),
            targetsInMenu = if (inGame) existing?.inMenu ?: emptySet() else targets
        )
        addInMappingsMap(mapping)
        list.removeIf { m -> if (dpad != null) m.dpadDirection == dpad else m.dpadDirection == null && m.key == code }
        list.add(mapping)
        save()
    }

    /** Remove toda ligação de um código (botão extra apagado). */
    fun removeCode(code: Int) {
        allKeyMappings.remove(code)
        list.removeIf { it.dpadDirection == null && it.key == code }
        save()
    }

    fun save() {
        keyMappingListMMKV().encode(name, this)
    }
}
