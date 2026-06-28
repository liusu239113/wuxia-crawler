package com.wuxiacrawler.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuxiacrawler.ui.screens.CreationScreen
import com.wuxiacrawler.ui.screens.MainScreen
import com.wuxiacrawler.ui.screens.TitleScreen
import com.wuxiacrawler.ui.theme.InkBlack
import com.wuxiacrawler.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameViewModel = viewModel()
            var screen by androidx.compose.runtime.mutableIntStateOf(
                if (viewModel.engine.hasSave() && !viewModel.engine.player.value.isAllocated) 2
                else if (viewModel.engine.hasSave()) 0 else 0
            )

            Surface(color = InkBlack, modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                when (screen) {
                    0 -> TitleScreen(
                        viewModel = viewModel,
                        onNewGame = { screen = 2 },
                        onContinue = {
                            viewModel.engine.loadGame()
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