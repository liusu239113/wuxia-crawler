package com.wuxiacrawler.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuxiacrawler.ui.screens.CreationScreen
import com.wuxiacrawler.ui.screens.MainScreen
import com.wuxiacrawler.ui.screens.TitleScreen
import com.wuxiacrawler.ui.theme.DefaultTextStyle
import com.wuxiacrawler.ui.theme.WuxiaTypography
import com.wuxiacrawler.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameViewModel = viewModel()
            val engine = viewModel.engine

            // 修复：有存档就直接进标题页，不再误判 isAllocated
            var screen by remember {
                mutableIntStateOf(if (engine.hasSave()) 0 else 0)
            }

            // 后台自动存档：onStop 时保存
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        if (engine.player.value.isAllocated) {
                            engine.saveGame()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Surface(color = Color(0xFF0D0D0D), modifier = Modifier.fillMaxSize()) {
                MaterialTheme(typography = WuxiaTypography) {
                CompositionLocalProvider(LocalTextStyle provides DefaultTextStyle) {
                when (screen) {
                    0 -> TitleScreen(
                        viewModel = viewModel,
                        onNewGame = { screen = 2 },
                        onContinue = {
                            engine.loadGame()
                            screen = 1
                        }
                    )
                    1 -> MainScreen(
                        viewModel = viewModel,
                        onDeath = { screen = 0 }
                    )
                    2 -> CreationScreen(
                        viewModel = viewModel,
                        onCreated = { screen = 1 }
                    )
                }
                }
                }
            }
        }
    }
}