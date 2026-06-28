package com.wuxiacrawler.config

object GameConfig {
    const val START_MONEY: Long = 500
    const val START_MAX_HP = 500
    const val START_ATK = 100
    const val START_DEF = 50
    const val START_SPEED = 0.6f
    const val START_VAMP = 0f
    const val START_CRIT_RATE = 0f
    const val START_CRIT_DMG = 50f
    const val MAX_ATTACK_SPEED = 2.5f
    const val ROOMS_PER_FLOOR = 5
    const val FLOOR_LIMIT = 100

    const val ENEMY_BASE_LVL = 1
    const val ENEMY_LVL_GAP = 5
    const val ENEMY_SCALING_START = 1.1f
    const val BLESSING_COST_MULT = 500.0
    const val CURSE_COST_MULT = 10000.0
    const val HP_RECOVER_PCT = 20
}

/** 敌人类型（匹配原版5类） */
enum class EnemyArchetype {
    OFFENSIVE,  // 攻击型：高攻低防
    DEFENSIVE,  // 防御型：高血高防
    BALANCED,   // 均衡型
    QUICK,      // 迅捷型：高攻速
    LETHAL      // 致命型：高暴击
}

/** 装备稀有度 */
enum class EquipmentRarity(val displayName: String, val colorHex: Long, val chance: Float, val statCount: Int) {
    COMMON("凡品", 0xFF9D9D9D, 0.70f, 2),
    UNCOMMON("良品", 0xFF4CAF50, 0.20f, 3),
    RARE("稀有", 0xFF2196F3, 0.04f, 4),
    EPIC("史诗", 0xFF9C27B0, 0.03f, 5),
    LEGENDARY("传说", 0xFFFF9800, 0.02f, 6),
    HEIRLOOM("太古", 0xFFF44336, 0.01f, 8)
}

/** 装备槽位属性 */
enum class EquipmentAttribute { DAMAGE, DEFENSE }

/** 装备类型 */
enum class EquipmentType(val displayName: String, val attr: EquipmentAttribute) {
    // 兵器（Damage）
    SWORD("长剑", EquipmentAttribute.DAMAGE),
    AXE("巨斧", EquipmentAttribute.DAMAGE),
    HAMMER("重锤", EquipmentAttribute.DAMAGE),
    DAGGER("匕首", EquipmentAttribute.DAMAGE),
    WHIP("软鞭", EquipmentAttribute.DAMAGE),
    SCYTHE("钩镰", EquipmentAttribute.DAMAGE),
    // 护甲（Defense）
    PLATE_ARMOR("板甲", EquipmentAttribute.DEFENSE),
    CHAIN_ARMOR("锁子甲", EquipmentAttribute.DEFENSE),
    LEATHER_ARMOR("皮甲", EquipmentAttribute.DEFENSE),
    // 盾牌（Defense）
    TOWER_SHIELD("塔盾", EquipmentAttribute.DEFENSE),
    KITE_SHIELD("鸢盾", EquipmentAttribute.DEFENSE),
    BUCKLER("圆盾", EquipmentAttribute.DEFENSE),
    // 头盔（Defense）
    GREAT_HELM("重盔", EquipmentAttribute.DEFENSE),
    HORNED_HELM("角盔", EquipmentAttribute.DEFENSE),
}

/** 敌人名库（匹配原版每个 archetype 下的具体名字） */
object EnemyNames {
    val OFFENSIVE_NORMAL = listOf("山贼弓手", "黑狼", "冰原狼", "剑奴", "骷髅射手", "骷髅剑师", "骷髅刺客", "骷髅海盗", "赤蜘蛛", "兽人斧卫", "兽人射手")
    val OFFENSIVE_GUARDIAN = listOf("霸天·妖兽统领", "骨皇·骷髅君主", "炽热蜘蛛王", "不死·骸骨帝王")
    val OFFENSIVE_BOSS = listOf("灭世·比希摩斯", "龙帝·煞拉洛斯")

    val DEFENSIVE_NORMAL = listOf("石魔像", "金甲剑奴", "圣光天使", "绿毒蜘蛛", "骷髅骑士", "骷髅武士")
    val DEFENSIVE_GUARDIAN = listOf("魔液君主", "星灵·天蟹圣者", "圣光·白昼泰坦")
    val DEFENSIVE_BOSS = listOf("死灵·乌利奥特")

    val BALANCED_NORMAL = listOf("山贼", "魔液兽", "金甲剑奴", "兽人剑师", "兽人斧卫", "兽人射手", "蜘蛛", "骷髅骑士", "骷髅武士")
    val BALANCED_GUARDIAN = listOf("龙骑·泰玛特", "无名·堕落之王", "星灵·白羊圣者")
    val BALANCED_BOSS = listOf("熔岩·伊弗利特", "冰霜·希瓦", "死神·萨纳托斯")

    val QUICK_NORMAL = listOf("山贼", "山贼刺客", "山贼弓手", "狼", "黑狼", "冰原狼", "兽人剑师", "蜘蛛", "赤蜘蛛", "绿毒蜘蛛", "骷髅剑师", "骷髅海盗", "骷髅刺客")
    val QUICK_GUARDIAN = listOf("蚁后·莉拉德", "发条·机械蜘蛛")
    val QUICK_BOSS = listOf("暗影·天使收割者", "蛛龙·奈兹彻")

    val LETHAL_NORMAL = listOf("山贼刺客", "狼", "黑狼", "冰原狼", "兽人剑师", "兽人斧卫", "赤蜘蛛", "骷髅剑师", "骷髅刺客")
    val LETHAL_GUARDIAN = listOf("致命·弑神狼", "冥犬·赫尔猎犬", "三头·刻耳柏洛斯")
    val LETHAL_BOSS = listOf("血煞·狂化妖")

    val MIMIC_CHEST = "宝箱怪"
    val MIMIC_DOOR = "秘境假门"
}

/** 武学被动技能 */
enum class MartialSkill(val displayName: String, val description: String) {
    REMNANT_EDGE("残刃刀法", "每次攻击额外造成敌人当前气血8%的伤害"),
    TITAN_WILL("铁骨铮铮", "每次攻击额外造成自身最大气血5%的伤害"),
    DEVASTATOR("破军诀", "攻击伤害提升30%"),
    RAMPAGER("嗜战", "每次攻击基础攻击力+5，战斗结束后重置"),
    BLADE_DANCE("影舞步", "每次攻击提升身法，战斗结束后重置"),
    PALADIN_HEART("金钟罩", "受到的所有伤害永久减少25%"),
    AEGIS_THORNS("荆棘反甲", "敌人受到其造成伤害的15%反弹"),
    BLOODTHIRST("嗜血术", "吸血效果额外提升5%"),
    PRECISION("心明眼亮", "暴击率额外提升8%")
}

/** 境界系统 */
enum class CultivationRealm(val displayName: String, val level: Int, val hpBonus: Float, val atkBonus: Float, val defBonus: Float) {
    NONE("未入流", 0, 0f, 0f, 0f),
    BODY_REFINING("淬体境", 1, 0.10f, 0.05f, 0.05f),
    QI_CONDENSING("凝气境", 2, 0.20f, 0.10f, 0.10f),
    FOUNDATION("筑基境", 3, 0.35f, 0.18f, 0.18f),
    GOLDEN_CORE("金丹境", 4, 0.55f, 0.28f, 0.28f),
    NASCENT_SOUL("元婴境", 5, 0.80f, 0.40f, 0.40f),
    SPIRIT_SEVERING("化神境", 6, 1.10f, 0.55f, 0.55f),
    DAO_COMBINING("合道境", 7, 1.50f, 0.75f, 0.75f),
    IMMORTAL("渡劫飞升", 8, 2.00f, 1.00f, 1.00f)
}

/** 秘境随机事件类型 */
enum class RealmEventType {
    NOTHING, ENEMY, TREASURE, BLESSING, CURSE, MONARCH, NEXT_ROOM
}

/** 升级属性选项 */
data class UpgradeOption(val stat: String, val value: Float, val statKey: String)