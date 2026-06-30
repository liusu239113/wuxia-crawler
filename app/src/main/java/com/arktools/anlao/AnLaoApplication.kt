package com.arktools.anlao

import android.app.Application
import android.util.Log
import com.arktools.anlao.adsdk.AdSdkConfig

/**
 * 暗牢江湖行 Application 类
 * 负责全局初始化：广告 SDK 配置、崩溃检测、数据库修复
 */
class AnLaoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 连续崩溃检测
        handleConsecutiveCrashes()

        // 数据库完整性检查
        repairCorruptedDatabaseIfNeeded()

        // 安装全局异常处理器
        installCrashHandler()

        // TapTap SDK 延迟初始化：必须在用户同意隐私政策后才能初始化
        // 实际初始化在 MainActivity 的隐私政策同意回调中执行

        // 配置广告 SDK
        AdSdkConfig.configure(
            appId = 2071828737590747137L,
            rewardVideoId = "2071835473475198977",
            privacyPolicyUrl = "http://yanyususu.online:5555/xiaozhang.html",
            isDebug = BuildConfig.DEBUG
        )
    }

    /**
     * 连续崩溃检测与恢复
     */
    private fun handleConsecutiveCrashes() {
        val prefs = getSharedPreferences("crash_guard", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastStart1 = prefs.getLong("last_start_1", 0L)
        val lastStart2 = prefs.getLong("last_start_2", 0L)

        val crash1 = lastStart1 > 0 && (lastStart2 - lastStart1) < 15_000L
        val crash2 = lastStart2 > 0 && (now - lastStart2) < 15_000L

        if (crash1 && crash2) {
            Log.e("AnLaoApplication", "Detected 3 consecutive rapid crashes! Resetting database.")
            val dbFile = getDatabasePath("anlao_db")
            if (dbFile.exists()) {
                try {
                    val backupFile = java.io.File(filesDir, "anlao_db.crash_reset_backup")
                    dbFile.copyTo(backupFile, overwrite = true)
                } catch (_: Exception) { }
                dbFile.delete()
                java.io.File(dbFile.absolutePath + "-wal").delete()
                java.io.File(dbFile.absolutePath + "-shm").delete()
                java.io.File(dbFile.absolutePath + "-journal").delete()
            }
            prefs.edit().putLong("last_start_1", 0L).putLong("last_start_2", 0L).apply()
        } else {
            prefs.edit()
                .putLong("last_start_1", lastStart2)
                .putLong("last_start_2", now)
                .apply()
        }

        // 启动成功后15秒清除崩溃计数
        android.os.Handler(mainLooper).postDelayed({
            prefs.edit().putLong("last_start_1", 0L).putLong("last_start_2", 0L).apply()
        }, 15_000L)
    }

    /**
     * 检查数据库文件完整性
     */
    private fun repairCorruptedDatabaseIfNeeded() {
        val dbFile = getDatabasePath("anlao_db")
        if (!dbFile.exists()) return

        if (isDatabaseHealthy(dbFile)) return

        Log.w("AnLaoApplication", "Database health check failed, attempting WAL repair...")
        deleteWalFiles(dbFile)

        if (isDatabaseHealthy(dbFile)) {
            Log.i("AnLaoApplication", "Database repaired by removing WAL files (no data loss)")
            return
        }

        Log.e("AnLaoApplication", "Database corrupted beyond WAL repair, backing up")
        backupCorruptedDatabase(dbFile)
    }

    private fun isDatabaseHealthy(dbFile: java.io.File): Boolean {
        return try {
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            val cursor = db.rawQuery("PRAGMA quick_check", null)
            val isOk = cursor.use { it.moveToFirst() && it.getString(0) == "ok" }
            db.close()
            isOk
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteWalFiles(dbFile: java.io.File) {
        java.io.File(dbFile.absolutePath + "-wal").delete()
        java.io.File(dbFile.absolutePath + "-shm").delete()
        java.io.File(dbFile.absolutePath + "-journal").delete()
    }

    private fun backupCorruptedDatabase(dbFile: java.io.File) {
        try {
            val backupFile = java.io.File(filesDir, "anlao_db.corrupted_backup")
            dbFile.copyTo(backupFile, overwrite = true)
            dbFile.delete()
            deleteWalFiles(dbFile)
        } catch (e: Exception) {
            Log.e("AnLaoApplication", "Failed to backup corrupted DB", e)
            dbFile.delete()
            deleteWalFiles(dbFile)
        }
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AnLaoApplication", "FATAL: ${throwable.message}", throwable)
            try {
                val crashLog = java.io.File(filesDir, "crash_log.txt")
                crashLog.appendText(
                    "\n=== ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())} ===\n" +
                    "Thread: ${thread.name}\n" +
                    "Exception: ${throwable.javaClass.name}: ${throwable.message}\n" +
                    throwable.stackTraceToString() + "\n"
                )
            } catch (_: Exception) { }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
