package com.wuxiacrawler.data

data class PlayerStats(
    var hp: Int = 500, var hpMax: Int = 500, var atk: Int = 100, var def: Int = 50,
    var atkSpd: Float = 0.6f, var vamp: Float = 0f, var critRate: Float = 0f, var critDmg: Float = 50f,
    var hpPercent: Float = 100f
)

data class BonusStats(
    var hp: Float = 0f, var atk: Float = 0f, var def: Float = 0f,
    var atkSpd: Float = 0f, var vamp: Float = 0f, var critRate: Float = 0f, var critDmg: Float = 0f
)

data class ExpInfo(
    var expCurr: Int = 0, var expMax: Int = 100,
    var expCurrLvl: Int = 0, var expMaxLvl: Int = 100,
    var expPercent: Float = 0f, var lvlGained: Int = 0
)

data class PlayerEntity(
    var name: String = "无名侠客",
    var gender: String = "male",
    var portrait: String = "characters/hero_male.png",
    var sect: String = "WANDERER",
    var realm: String = "NONE",
    var lvl: Int = 1,
    var kills: Int = 0,
    var deaths: Int = 0,
    var playtime: Long = 0,
    var gold: Long = 500L,

    // 基础属性
    var baseStats: PlayerStats = PlayerStats(),
    // 等级奖励属性（百分比）
    var bonusStats: BonusStats = BonusStats(),
    // 装备加成属性
    var equippedStats: PlayerStats = PlayerStats(0, 0, 0, 0, 0f, 0f, 0f, 0f),
    // 套装加成属性
    var setBonusStats: PlayerStats = PlayerStats(0, 0, 0, 0, 0f, 0f, 0f, 0f),
    // 当前最终属性
    var stats: PlayerStats = PlayerStats(),
    // 临时buff（嗜战/影舞步）
    var tempStats: BonusStats = BonusStats(),

    // 经验
    var exp: ExpInfo = ExpInfo(),

    // 装备
    var equipped: String = "[]",      // JSON array of EquipmentItem
    var inventory: String = "[]",

    // 技能
    var skills: String = "",
    var inCombat: Boolean = false,
    var isAllocated: Boolean = false,
    var prologueSeen: Boolean = false,

    // 祝福等级
    var blessing: Int = 1
)

data class EquipmentItem(
    val category: String = "",       // EquipmentType name
    val attribute: String = "DAMAGE", // EquipmentAttribute name
    val type: String = "Weapon",     // 武器/护甲/盾牌/头盔
    val rarity: String = "凡品",
    val lvl: Int = 1,
    val tier: Int = 1,
    val value: Int = 0,
    val stats: List<Map<String, Float>> = emptyList() // [{statName: value}, ...]
)

data class CombatState(
    var combatId: Int = 0,
    var battleType: String = "normal",
    var bossPhase: Int = 1,
    var enemyName: String = "",
    var enemyLvl: Int = 1,
    var enemyArchetype: String = "",
    var enemyHp: Int = 0, var enemyHpMax: Int = 0, var enemyHpPercent: Float = 100f,
    var enemyAtk: Int = 0, var enemyDef: Int = 0, var enemyAtkSpd: Float = 0.4f,
    var enemyVamp: Float = 0f, var enemyCritRate: Float = 0f, var enemyCritDmg: Float = 0f,
    var playerHp: Int = 0, var playerHpMax: Int = 0,
    var playerAtk: Int = 0, var playerDef: Int = 0, var playerAtkSpd: Float = 0.6f,
    var playerVamp: Float = 0f, var playerCritRate: Float = 0f, var playerCritDmg: Float = 0f,
    var expReward: Int = 0, var goldReward: Int = 0, var hasDrop: Boolean = false,
    var enemyDead: Boolean = false, var playerDead: Boolean = false, var combatSeconds: Int = 0
)