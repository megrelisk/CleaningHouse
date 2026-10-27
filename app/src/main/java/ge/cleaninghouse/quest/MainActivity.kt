package ge.cleaninghouse.quest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.cleaninghouse.quest.ui.AppViewModel
import ge.cleaninghouse.quest.ui.LocalKa
import ge.cleaninghouse.quest.ui.Screen
import ge.cleaninghouse.quest.ui.components.CelebrationOverlay
import ge.cleaninghouse.quest.ui.screens.CaptureScreen
import ge.cleaninghouse.quest.ui.screens.HomeScreen
import ge.cleaninghouse.quest.ui.screens.SessionScreen
import ge.cleaninghouse.quest.ui.screens.SettingsScreen
import ge.cleaninghouse.quest.ui.screens.StoreScreen
import ge.cleaninghouse.quest.ui.theme.CleanQuestTheme
import ge.cleaninghouse.quest.ui.tr

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleanQuestTheme {
                App()
            }
        }
    }
}

@Composable
private fun App(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalKa provides (state.settings.uiLang == "ka")) {
        Box(Modifier.fillMaxSize()) {
            when (val screen = vm.screen) {
                Screen.Capture -> CaptureScreen(state, vm)
                is Screen.Session -> SessionScreen(state, vm, screen.id)
                else -> {
                    if (screen != Screen.Home) BackHandler { vm.screen = Screen.Home }
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            NavigationBar(containerColor = Color.White) {
                                NavItem(screen == Screen.Home, "🏠", tr("Quests", "ქვესტები")) { vm.screen = Screen.Home }
                                NavItem(screen == Screen.Store, "🎁", tr("Store", "მაღაზია")) { vm.screen = Screen.Store }
                                NavItem(screen == Screen.Settings, "⚙️", tr("Settings", "პარამეტრები")) { vm.screen = Screen.Settings }
                            }
                        },
                    ) { padding ->
                        when (screen) {
                            Screen.Store -> StoreScreen(state, vm, padding)
                            Screen.Settings -> SettingsScreen(state, vm, padding)
                            else -> HomeScreen(state, vm, padding)
                        }
                    }
                }
            }
            vm.celebration?.let { c ->
                CelebrationOverlay(c, state.settings) { vm.celebration = null }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    selected: Boolean,
    emoji: String,
    label: String,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Text(emoji, fontSize = 22.sp) },
        label = { Text(label) },
    )
}
