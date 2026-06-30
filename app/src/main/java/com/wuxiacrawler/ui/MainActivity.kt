package com.wuxiacrawler.ui

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuxiacrawler.ui.screens.CreationScreen
import com.wuxiacrawler.ui.screens.MainScreen
import com.wuxiacrawler.ui.screens.PrologueScreen
import com.wuxiacrawler.ui.screens.TitleScreen
import com.wuxiacrawler.ui.theme.WuxiaTypography
import com.wuxiacrawler.viewmodel.GameViewModel

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

            var screen by remember {
                mutableIntStateOf(if (engine.hasSave()) 0 else 0)
            }

            LaunchedEffect(screen) {
                if (screen != 1) engine.soundManager.stopBgm()
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
                        0 -> TitleScreen(
                            viewModel = viewModel,
                            onNewGame = {
                                engine.deleteSave()
                                screen = 2
                            },
                            onContinue = {
                                if (engine.loadGame()) screen = if (engine.player.value.prologueSeen) 1 else 3
                                else {
                                    engine.deleteSave()
                                    screen = 0
                                }
                            }
                        )
                        1 -> MainScreen(
                            viewModel = viewModel,
                            onDeath = { screen = 0 }
                        )
                        2 -> CreationScreen(
                            viewModel = viewModel,
                            onCreated = { screen = 3 }
                        )
                        3 -> PrologueScreen(
                            viewModel = viewModel,
                            onFinished = { screen = 1 }
                        )
                    }
                }
            }
        }
    }
}