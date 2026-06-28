package com.wuxiacrawler.data

data class RealmState(
    val id: Int = 1,
    var floor: Int = 1,
    var room: Int = 1,
    val roomsPerFloor: Int = 5,
    var isExploring: Boolean = false,
    var isPaused: Boolean = true,
    var isEventActive: Boolean = false,
    var actionCounter: Int = 0,
    var enemyBaseLevel: Int = 1,
    var enemyLevelGap: Int = 5,
    var enemyScaling: Float = 1.1f,
    var currentKills: Int = 0,
    var runTime: Long = 0L
)