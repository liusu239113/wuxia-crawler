package com.arktools.anlao.config

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

/** 游戏难度：当前版本数值作为最高难度“生死论剑”基准线 */
enum class GameDifficulty(
    val displayName: String,
    val rankName: String,
    val description: String,
    val enemyStatMultiplier: Float,
    val enemySpeedMultiplier: Float,
    val silverRewardMultiplier: Float,
    val shopPriceMultiplier: Float,
    val forgePriceMultiplier: Float,
    val enhanceSuccessBonus: Int
) {
    EASY(
        "初涉江湖",
        "简单",
        "适合熟悉暗牢规则：敌人更弱，银两更多，药物与锻造更便宜。",
        0.70f,
        0.85f,
        1.60f,
        0.65f,
        0.55f,
        25
    ),
    NORMAL(
        "江湖好手",
        "普通",
        "适合稳定推进主线：敌人略弱，银两收益和价格更宽松。",
        0.85f,
        0.92f,
        1.25f,
        0.85f,
        0.72f,
        15
    ),
    HARD(
        "生死论剑",
        "困难",
        "当前三档最高难度；敌强、收益和药价保持当前设置，但强化重铸更宽松。",
        1.00f,
        1.00f,
        1.00f,
        1.00f,
        0.82f,
        8
    )
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
    SWORD("青锋剑", EquipmentAttribute.DAMAGE),
    AXE("开山斧", EquipmentAttribute.DAMAGE),
    HAMMER("镇岳锤", EquipmentAttribute.DAMAGE),
    DAGGER("袖里刃", EquipmentAttribute.DAMAGE),
    WHIP("游龙鞭", EquipmentAttribute.DAMAGE),
    SCYTHE("月牙镰", EquipmentAttribute.DAMAGE),
    SPEAR("梨花枪", EquipmentAttribute.DAMAGE),
    SABER("雁翎刀", EquipmentAttribute.DAMAGE),
    STAFF("盘龙棍", EquipmentAttribute.DAMAGE),
    FAN("铁骨扇", EquipmentAttribute.DAMAGE),
    NEEDLE("暴雨针", EquipmentAttribute.DAMAGE),
    BLADE_RING("乾坤轮", EquipmentAttribute.DAMAGE),
    // 护甲（Defense）
    PLATE_ARMOR("玄铁甲", EquipmentAttribute.DEFENSE),
    CHAIN_ARMOR("金丝软甲", EquipmentAttribute.DEFENSE),
    LEATHER_ARMOR("夜行衣", EquipmentAttribute.DEFENSE),
    SCALE_ARMOR("龙鳞甲", EquipmentAttribute.DEFENSE),
    ROBE_ARMOR("青云袍", EquipmentAttribute.DEFENSE),
    VINE_ARMOR("藤纹甲", EquipmentAttribute.DEFENSE),
    // 盾牌（Defense）
    TOWER_SHIELD("玄武盾", EquipmentAttribute.DEFENSE),
    KITE_SHIELD("雁翎盾", EquipmentAttribute.DEFENSE),
    BUCKLER("八卦盾", EquipmentAttribute.DEFENSE),
    LOTUS_SHIELD("莲花盾", EquipmentAttribute.DEFENSE),
    TIGER_SHIELD("虎面盾", EquipmentAttribute.DEFENSE),
    COPPER_SHIELD("赤铜盾", EquipmentAttribute.DEFENSE),
    // 头盔（Defense）
    GREAT_HELM("狮首盔", EquipmentAttribute.DEFENSE),
    HORNED_HELM("龙纹冠", EquipmentAttribute.DEFENSE),
    BAMBOO_HAT("斗笠", EquipmentAttribute.DEFENSE),
    JADE_CROWN("青玉冠", EquipmentAttribute.DEFENSE),
    IRON_MASK("铁面具", EquipmentAttribute.DEFENSE),
    CLOTH_BAND("束发带", EquipmentAttribute.DEFENSE),
    // 鞋履（Defense）
    CLOUD_BOOTS("踏云靴", EquipmentAttribute.DEFENSE),
    IRON_BOOTS("玄铁靴", EquipmentAttribute.DEFENSE),
    WIND_BOOTS("追风履", EquipmentAttribute.DEFENSE),
    SHADOW_BOOTS("夜影靴", EquipmentAttribute.DEFENSE),
    RAIN_BOOTS("听雨鞋", EquipmentAttribute.DEFENSE),
    MONK_SHOES("罗汉履", EquipmentAttribute.DEFENSE),
    // 饰品（Defense）
    JADE_PENDANT("青玉佩", EquipmentAttribute.DEFENSE),
    TIGER_TOKEN("虎符坠", EquipmentAttribute.DEFENSE),
    BELL_CHARM("镇魂铃", EquipmentAttribute.DEFENSE),
    SWORD_TASSEL("剑穗", EquipmentAttribute.DEFENSE),
    BLOOD_RING("血玉戒", EquipmentAttribute.DEFENSE),
    LOTUS_BEAD("莲心珠", EquipmentAttribute.DEFENSE),
}

/** 敌人名库（匹配原版每个 archetype 下的具体名字） */
object EnemyNames {
    val OFFENSIVE_NORMAL = listOf("飞刀恶徒", "黑风狼卫", "寒岭狼卫", "断剑门徒", "暗弩门徒", "白衣剑客", "夜行刺客", "水寨刀匪", "赤练刀客", "蛮寨斧客", "蛮寨弓手")
    val OFFENSIVE_GUARDIAN = listOf("霸刀·黑寨统领", "枯木·白衣门主", "赤练·刀堂主", "不老·枯木宗师")
    val OFFENSIVE_BOSS = listOf("镇山·铁掌帮主", "龙门·煞罗堂主")

    val DEFENSIVE_NORMAL = listOf("铁布衫石奴", "金甲剑侍", "明王护法", "碧毒刀奴", "黑甲枪卫", "白衣刀客")
    val DEFENSIVE_GUARDIAN = listOf("化血·药坛主", "天钩·星宿护法", "明王·金身罗汉")
    val DEFENSIVE_BOSS = listOf("黑袍·风雷长老")

    val BALANCED_NORMAL = listOf("黑寨喽啰", "化血药人", "金甲剑侍", "蛮寨剑客", "蛮寨斧客", "蛮寨弓手", "毒窟刀奴", "黑甲枪卫", "白衣刀客")
    val BALANCED_GUARDIAN = listOf("龙骑·天摩尊者", "无名·堕落剑王", "白虹·星宿护法")
    val BALANCED_BOSS = listOf("火云·赤袍老祖", "冰魄·寒霜宫主", "索命·阎罗判官")

    val QUICK_NORMAL = listOf("黑寨喽啰", "飞檐刺客", "飞刀恶徒", "荒原狼卫", "黑风狼卫", "寒岭狼卫", "蛮寨剑客", "毒窟刀奴", "赤练刀客", "碧毒刀奴", "白衣剑客", "水寨刀匪", "夜行刺客")
    val QUICK_GUARDIAN = listOf("千丝·夫人", "机括·机关堂主")
    val QUICK_BOSS = listOf("暗影·夺魂使", "盘丝·蛛索长老")

    val LETHAL_NORMAL = listOf("飞檐刺客", "荒原狼卫", "黑风狼卫", "寒岭狼卫", "蛮寨剑客", "蛮寨斧客", "赤练刀客", "白衣剑客", "夜行刺客")
    val LETHAL_GUARDIAN = listOf("天狼·黑风寨主", "铁犬·猎犬使", "三刀·獒王寨主")
    val LETHAL_BOSS = listOf("血煞·疯魔刀圣")

    val MIMIC_CHEST = "机关宝匣"
    val MIMIC_DOOR = "幻阵假门"
}

/** 武学被动技能 */
enum class MartialSkill(val displayName: String, val description: String) {
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

/** 门派成长路线 */
enum class MartialSect(
    val displayName: String,
    val description: String,
    val hpBonus: Float,
    val atkBonus: Float,
    val defBonus: Float,
    val atkSpdBonus: Float,
    val vampBonus: Float,
    val critRateBonus: Float
) {
    WANDERER("江湖散人", "无门无派，成长均衡，适合稳扎稳打", 0.05f, 0.05f, 0.05f, 0.03f, 0f, 0f),
    XUANYUE("玄岳门", "重根骨与护体，气血和防御更高", 0.16f, 0f, 0.10f, 0f, 0f, 0f),
    DUANYUN("断云楼", "讲究一击破敌，攻击和暴击更强", 0f, 0.14f, 0f, 0f, 0f, 3f),
    LIUYING("流影阁", "身法灵动，出手更快并略通吸血", 0f, 0.04f, 0f, 0.12f, 2f, 0f),
    BAICAO("百草谷", "善养气疗伤，气血与吸血兼备", 0.10f, 0f, 0.04f, 0f, 3f, 0f)
}

/** 境界系统 */
enum class CultivationRealm(val displayName: String, val level: Int, val hpBonus: Float, val atkBonus: Float, val defBonus: Float) {
    NONE("初入江湖", 0, 0f, 0f, 0f),
    BODY_REFINING("锻体", 1, 0.10f, 0.05f, 0.05f),
    QI_CONDENSING("练气", 2, 0.20f, 0.10f, 0.10f),
    FOUNDATION("通脉", 3, 0.35f, 0.18f, 0.18f),
    GOLDEN_CORE("开窍", 4, 0.55f, 0.28f, 0.28f),
    NASCENT_SOUL("入微", 5, 0.80f, 0.40f, 0.40f),
    SPIRIT_SEVERING("化劲", 6, 1.10f, 0.55f, 0.55f),
    DAO_COMBINING("宗师", 7, 1.50f, 0.75f, 0.75f),
    IMMORTAL("大宗师", 8, 2.00f, 1.00f, 1.00f)
}

object MartialRealmDisplay {
    private val majorRealms = listOf(
        "初入江湖", "锻体", "练气", "通脉", "开窍", "入微", "化劲", "宗师", "大宗师", "武林名宿",
        "一代豪侠", "绝顶高手", "江湖传奇", "天下无双", "武道神话", "破碎虚空"
    )

    fun fromLevel(level: Int): String {
        val safeLevel = level.coerceAtLeast(1)
        val stageIndex = (safeLevel - 1) / 10
        val layer = (safeLevel - 1) % 10 + 1
        val cycle = stageIndex / majorRealms.size
        val name = majorRealms[stageIndex % majorRealms.size]
        val suffix = if (cycle == 0) "" else " · ${cycle + 1}转"
        return "$name$suffix · $layer 层"
    }

    fun enemyFromLevel(level: Int): String = fromLevel(level)
}

/** 江湖随机事件类型 */
enum class RealmEventType {
    NOTHING, ENEMY, TREASURE, BLESSING, CURSE, MONARCH, NEXT_ROOM
}

/** 升级属性选项 */
data class UpgradeOption(val stat: String, val value: Float, val statKey: String)
