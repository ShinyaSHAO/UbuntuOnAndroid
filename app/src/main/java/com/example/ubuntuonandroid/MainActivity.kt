package com.example.ubuntuonandroid

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ubuntuonandroid.theme.UbuntuOnAndroidTheme
import com.termux.view.TerminalView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            UbuntuOnAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppScreen()
                }
            }
        }
    }

    @Composable
    fun AppScreen() {
        var isInstalled by remember { mutableStateOf(EnvironmentInstaller.isInstalled(this)) }
        var progressText by remember { mutableStateOf("Initializing...") }

        if (!isInstalled) {
            LaunchedEffect(Unit) {
                thread {
                    EnvironmentInstaller.install(this@MainActivity) { progress ->
                        Handler(Looper.getMainLooper()).post {
                            progressText = progress
                        }
                    }
                    Handler(Looper.getMainLooper()).post {
                        isInstalled = true
                    }
                }
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = progressText)
            }
        } else {
            TerminalScreen()
        }
    }

    @Composable
    fun TerminalScreen() {
        AndroidView(
            factory = { context ->
                val terminalView = TerminalView(context, null)
                terminalView.keepScreenOn = true
                terminalView.isFocusable = true
                terminalView.isFocusableInTouchMode = true
                terminalView.setBackgroundColor(android.graphics.Color.BLACK)
                
                terminalView.setOnTouchListener { v, event ->
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        v.requestFocus()
                        v.post {
                            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.toggleSoftInput(android.view.inputmethod.InputMethodManager.SHOW_FORCED, 0)
                        }
                    }
                    false
                }
                
                // Initialize TerminalView required properties to avoid NPE in TerminalRenderer
                terminalView.setTextSize(24)
                
                terminalView.setTerminalViewClient(object : com.termux.view.TerminalViewClient {
                    override fun onScale(scale: Float): Float = 40f
                    override fun onSingleTapUp(e: android.view.MotionEvent) {
                        android.util.Log.e("UbuntuOnAndroid", "onSingleTapUp called!")
                        terminalView.requestFocus()
                        terminalView.post {
                            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.showSoftInput(terminalView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
                    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                    override fun shouldEnforceCharBasedInput(): Boolean = false
                    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
                    override fun onLongPress(event: android.view.MotionEvent): Boolean = false
                    
                    override fun isTerminalViewSelected(): Boolean = true
                    override fun copyModeChanged(copyMode: Boolean) {}
                    override fun onKeyDown(keyCode: Int, e: android.view.KeyEvent, session: TerminalSession): Boolean = false
                    override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent): Boolean = false
                    override fun readControlKey(): Boolean = false
                    override fun readAltKey(): Boolean = false
                    override fun readShiftKey(): Boolean = false
                    override fun readFnKey(): Boolean = false
                    override fun onEmulatorSet() {}
                    override fun logError(tag: String, message: String) {}
                    override fun logWarn(tag: String, message: String) {}
                    override fun logInfo(tag: String, message: String) {}
                    override fun logDebug(tag: String, message: String) {}
                    override fun logVerbose(tag: String, message: String) {}
                    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                    override fun logStackTrace(tag: String, e: Exception) {}
                })

                val args = arrayOf(File(context.filesDir, "start-ubuntu.sh").absolutePath)
                val env = arrayOf("HOME=" + context.filesDir.absolutePath, "TERM=xterm-256color")
                
                val executablePath = "/system/bin/sh"
                
                val session = TerminalSession(
                    executablePath,
                    context.filesDir.absolutePath,
                    args,
                    env,
                    250,
                    object : TerminalSessionClient {
                        override fun onTextChanged(session: TerminalSession) {
                            terminalView.onScreenUpdated()
                        }
                        override fun onTitleChanged(session: TerminalSession) {}
                        override fun onSessionFinished(session: TerminalSession) {}
                        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}
                        override fun onPasteTextFromClipboard(session: TerminalSession) {}
                        override fun onBell(session: TerminalSession) {}
                        override fun onColorsChanged(session: TerminalSession) {}
                        override fun onTerminalCursorStateChange(state: Boolean) {}
                        override fun getTerminalCursorStyle(): Int = 0
                        override fun logError(tag: String, message: String) {}
                        override fun logWarn(tag: String, message: String) {}
                        override fun logInfo(tag: String, message: String) {}
                        override fun logDebug(tag: String, message: String) {}
                        override fun logVerbose(tag: String, message: String) {}
                        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                        override fun logStackTrace(tag: String, e: Exception) {}
                    }
                )
                
                terminalView.attachSession(session)
                terminalView.requestFocus()
                terminalView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
