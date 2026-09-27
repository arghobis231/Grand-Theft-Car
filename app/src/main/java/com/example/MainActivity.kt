package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.grandtheftcar.engine.GameEngine
import com.example.grandtheftcar.ui.GameSurfaceView

class MainActivity : ComponentActivity() {

    private lateinit var gameEngine: GameEngine
    private lateinit var gameSurfaceView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during racing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enable edge-to-edge fullscreen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Initialize Game
        gameEngine = GameEngine(this)
        gameSurfaceView = GameSurfaceView(this, gameEngine)
        setContentView(gameSurfaceView)

        // Back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val handled = gameEngine.handleBackPress()
                if (!handled) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Re-hide system bars when returning to app
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        gameSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        gameSurfaceView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameSurfaceView.onDestroy()
    }
}
