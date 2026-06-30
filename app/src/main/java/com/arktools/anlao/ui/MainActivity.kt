package com.arktools.anlao.ui

import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arktools.anlao.TapSdkInitializer
import com.arktools.anlao.adsdk.TosinAdInitializer
import com.arktools.anlao.adsdk.ui.PrivacyPolicyDialog
import com.arktools.anlao.ui.login.ComplianceManager
import com.arktools.anlao.ui.login.TapTapLoginScreen
import com.arktools.anlao.ui.screens.CreationScreen
import com.arktools.anlao.ui.screens.MainScreen
import com.arktools.anlao.ui.screens.PrologueScreen
import com.arktools.anlao.ui.screens.TitleScreen
import com.arktools.anlao.ui.theme.WuxiaTypography
import com.arktools.anlao.viewmodel.GameViewModel
import kotlinx.coroutines.delay

/**
 * 预游戏状态：
 * 0 = 等待隐私政策同意
 * 1 = SDK 初始化中 / 等待 TapTap 登录
 * 2 = 防沉迷认证中
 * 3 = 已就绪，显示游戏标题画面
 * 4 = 主界面
 * 5 = 创建角色
 * 6 = 序章
 */
private const val PRE_PRIVACY = 0
private const val PRE_LOGIN = 1
private const val PRE_COMPLIANCE = 2
private const val GAME_TITLE = 3
private const val GAME_MAIN = 4
private const val GAME_CREATION = 5
private const val GAME_PROLOGUE = 6

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            val viewModel: GameViewModel = viewModel()
            val engine = viewModel.engine

            var screen by remember { mutableIntStateOf(PRE_PRIVACY) }

            LaunchedEffect(screen) {
                if (screen != GAME_MAIN) engine.soundManager.stopBgm()
            }

            // 后台自动存档
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        engine.soundManager.onEnterBackground()
                        if (engine.player.value.isAllocated) {
                            engine.trySafeSave()
                        }
                    } else if (event == Lifecycle.Event.ON_START) {
                        engine.soundManager.onEnterForeground()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Surface(color = Color(0xFF0D0D0D), modifier = Modifier.fillMaxSize()) {
                MaterialTheme(typography = WuxiaTypography) {
                    when (screen) {
                        // ===== 第0步：隐私政策 =====
                        PRE_PRIVACY -> {
                            PrivacyPolicyDialog(
                                appName = "暗牢江湖行",
                                onAccepted = {
                                    Log.i("MainActivity", "Privacy policy accepted")
                                    // 隐私同意后初始化所有 SDK
                                    initAllSdks()
                                    screen = PRE_LOGIN
                                },
                                onDismiss = {
                                    // 不同意隐私政策，退出应用
                                    finishAffinity()
                                }
                            )
                        }

                        // ===== 第1步：TapTap 登录 =====
                        PRE_LOGIN -> {
                            TapTapLoginScreen(
                                onLoginSuccess = { account ->
                                    Log.i("MainActivity", "TapTap login success: ${account.openId}")
                                    ComplianceManager.startup(
                                        this@MainActivity,
                                        account.openId ?: account.unionId ?: "unknown"
                                    )
                                    screen = PRE_COMPLIANCE
                                }
                            )
                        }

                        // ===== 第2步：防沉迷认证 =====
                        PRE_COMPLIANCE -> {
                            ComplianceScreen(
                                onAllowEnter = {
                                    screen = GAME_TITLE
                                },
                                onRetryLogin = {
                                    ComplianceManager.exit()
                                    screen = PRE_LOGIN
                                }
                            )
                        }

                        // ===== 游戏标题画面 =====
                        GAME_TITLE -> TitleScreen(
                            viewModel = viewModel,
                            onNewGame = {
                                engine.deleteSave()
                                screen = GAME_CREATION
                            },
                            onContinue = {
                                if (engine.loadGame()) {
                                    screen = if (engine.player.value.prologueSeen) GAME_MAIN else GAME_PROLOGUE
                                } else {
                                    engine.deleteSave()
                                    screen = GAME_TITLE
                                }
                            }
                        )

                        // ===== 游戏主界面 =====
                        GAME_MAIN -> MainScreen(
                            viewModel = viewModel,
                            onDeath = {
                                // 死亡复活广告将在 MainScreen 内部处理
                                screen = GAME_TITLE
                            }
                        )

                        // ===== 角色创建 =====
                        GAME_CREATION -> CreationScreen(
                            viewModel = viewModel,
                            onCreated = { screen = GAME_PROLOGUE }
                        )

                        // ===== 序章 =====
                        GAME_PROLOGUE -> PrologueScreen(
                            viewModel = viewModel,
                            onFinished = { screen = GAME_MAIN }
                        )
                    }
                }
            }
        }
    }

    /**
     * 隐私政策同意后，初始化所有 SDK
     */
    private fun initAllSdks() {
        try {
            // 1. TapTap SDK
            TapSdkInitializer.ensureInitialized(this)

            // 2. Tosin 广告 SDK
            TosinAdInitializer.getInstance().init(application as android.app.Application)

            Log.i("MainActivity", "All SDKs initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "SDK init failed", e)
        }
    }
}

/**
 * 防沉迷认证等待界面
 * 根据 ComplianceManager 的回调显示不同状态
 */
@Composable
private fun ComplianceScreen(
    onAllowEnter: () -> Unit,
    onRetryLogin: () -> Unit
) {
    var statusText by remember { mutableStateOf("正在进行防沉迷认证...") }
    var statusColor by remember { mutableStateOf(Color(0xFFD4A853)) }

    // 注册防沉迷回调
    DisposableEffect(Unit) {
        ComplianceManager.register(object : ComplianceManager.ComplianceListener {
            override fun onLoginSuccess() {
                statusText = "认证通过，准备踏入江湖..."
                statusColor = Color(0xFF4CAF50)
            }

            override fun onExited() {
                statusText = "认证已取消，返回登录"
                statusColor = Color(0xFFE53935)
            }

            override fun onSwitchAccount() {
                statusText = "切换账号中..."
                statusColor = Color(0xFF2196F3)
            }

            override fun onPeriodRestrict() {
                statusText = "当前为宵禁时段（22:00-8:00），无法进入游戏"
                statusColor = Color(0xFFE53935)
            }

            override fun onDurationLimit() {
                statusText = "今日游戏时长已用完，请明天再来"
                statusColor = Color(0xFFE53935)
            }

            override fun onAgeLimit() {
                statusText = "根据国家相关规定，你的年龄暂时无法进入游戏"
                statusColor = Color(0xFFE53935)
            }

            override fun onRealNameStop() {
                statusText = "需要完成实名认证才能进入游戏"
                statusColor = Color(0xFFE53935)
            }

            override fun onError(message: String) {
                statusText = "认证失败：$message"
                statusColor = Color(0xFFE53935)
            }
        })
        onDispose { }
    }

    // 监听认证成功
    LaunchedEffect(Unit) {
        // 等待一段时间后自动检查（防沉迷SDK的认证界面是它自己弹出的）
        delay(2000)
        // 如果防沉迷 SDK 没有弹出界面（例如漏掉了startup调用），直接通过
        // onLoginSuccess 会在 SDK 认证成功回调时触发 onAllowEnter
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⚔",
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            // 如果认证成功，延迟后进入游戏
            if (statusColor == Color(0xFF4CAF50)) {
                LaunchedEffect(Unit) {
                    delay(1500)
                    onAllowEnter()
                }
            }
            // 如果需要重新登录
            if (statusText.contains("返回登录")) {
                androidx.compose.material3.TextButton(
                    onClick = onRetryLogin
                ) {
                    Text("重新登录", color = Color(0xFFD4A853))
                }
            }
        }
    }
}
