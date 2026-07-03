package com.arktools.anlao.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arktools.anlao.config.MartialSkill
import com.arktools.anlao.engine.GameEngine

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val engine = GameEngine(application)

    // 便捷访问
    val player get() = engine.player
    val realm get() = engine.realm
    val combatState get() = engine.combatState
    val combatLog get() = engine.combatLog
    val realmLog get() = engine.realmLog
    val availableUpgrades get() = engine.availableUpgrades
    val rerollsLeft get() = engine.rerollsLeft
}

// 手动补全命名映射（中文输入）
enum class SkillDisplay(val label: String, val desc: String) {
    REMNANT_EDGE("照影断脉", "敌人伤势越重，攻击附加越高；最高不超过自身攻击80%"),
    TITAN_WILL("孤灯守魄", "自身气血低于45%时，造成伤害提升22%"),
    DEVASTATOR("摧锋入势", "攻击提升18%，暴击伤害提升12%"),
    RAMPAGER("炉火连环", "本场战斗每次出手攻击+3，最多叠加36点"),
    BLADE_DANCE("掠雨身法", "本场战斗每次出手暴击率+0.6%，最多叠加6%"),
    PALADIN_HEART("玄息护体", "受到伤害降低18%；气血低于35%时降低28%"),
    AEGIS_THORNS("借劲回澜", "受击后以伤害和防御反震敌人，单次有上限"),
    BLOODTHIRST("归血微澜", "吸血提升4%，气血上限提升4%"),
    PRECISION("星痕洞察", "暴击率提升5%，暴击伤害提升15%")
}