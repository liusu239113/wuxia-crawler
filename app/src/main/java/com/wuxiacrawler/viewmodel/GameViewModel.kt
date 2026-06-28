package com.wuxiacrawler.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wuxiacrawler.config.MartialSkill
import com.wuxiacrawler.engine.GameEngine

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
    REMNANT_EDGE("残刃刀法", "每次攻击额外造成敌人当前气血8%的伤害"),
    TITAN_WILL("铁骨铮铮", "每次攻击额外造成自身最大气血5%的伤害"),
    DEVASTATOR("破军诀", "攻击伤害提升30%"),
    BLADE_DANCE("影舞步", "每次攻击提升身法，战斗结束后重置"),
    PALADIN_HEART("金钟罩", "受到的所有伤害永久减少25%"),
    AEGIS_THORNS("荆棘反甲", "敌人受到其造成伤害的15%反弹"),
    BLOODTHIRST("嗜血术", "吸血效果额外提升5%"),
    PRECISION("心明眼亮", "暴击率额外提升8%")
}