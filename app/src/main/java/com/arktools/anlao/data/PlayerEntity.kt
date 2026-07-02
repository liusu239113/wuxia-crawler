package com.arktools.anlao.data

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

    var baseStats: PlayerStats = PlayerStats(),
    var bonusStats: BonusStats = BonusStats(),
    var equippedStats: PlayerStats = PlayerStats(0, 0, 0, 0, 0f, 0f, 0f, 0f),
    var setBonusStats: PlayerStats = PlayerStats(0, 0, 0, 0, 0f, 0f, 0f, 0f),
    var stats: PlayerStats = PlayerStats(),
    var tempStats: BonusStats = BonusStats(),

    var exp: ExpInfo = ExpInfo(),

    var equipped: String = "[]",
    var inventory: String = "[]",

    var skills: String = "",
    var inCombat: Boolean = false,
    var isAllocated: Boolean = false,
    var prologueSeen: Boolean = false,
    var blessing: Int = 1,

    // 心魔值 0-200
    var stress: Int = 0,
    var stressAffliction: String = "",
    var stressVirtue: String = "",

    // 火折子（torchCount 为旧存档兼容字段，新逻辑使用分档数量）
    var torchCount: Int = 0,
    var torchCommonCount: Int = 0,
    var torchFineCount: Int = 0,
    var torchSuperiorCount: Int = 0,
    var torchActive: Boolean = false,
    var torchSecondsLeft: Int = 0,

    // 解毒散（antidoteCount 为旧存档兼容字段，新逻辑使用分档数量）
    var antidoteCount: Int = 0,
    var antidoteCommonCount: Int = 0,
    var antidoteFineCount: Int = 0,
    var antidoteSuperiorCount: Int = 0,
    var antidoteActive: Boolean = false,
    var antidoteSecondsLeft: Int = 0,

    // 毒雾
    var poisonFogActive: Boolean = false,
    var poisonFogTurns: Int = 0
)

data class EquipmentItem(
    val category: String = "",
    val attribute: String = "DAMAGE",
    val type: String = "Weapon",
    val rarity: String = "凡品",
    val lvl: Int = 1,
    val tier: Int = 1,
    val value: Int = 0,
    val stats: List<Map<String, Float>> = emptyList(),
    val durability: Int = 100
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