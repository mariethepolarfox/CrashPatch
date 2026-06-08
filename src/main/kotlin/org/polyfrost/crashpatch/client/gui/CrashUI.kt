package org.polyfrost.crashpatch.client.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.minecraft.CrashReport
import net.minecraft.ReportType
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.language.I18n
import org.polyfrost.crashpatch.client.LogUploader
import org.polyfrost.crashpatch.client.crashes.CrashScan
import org.polyfrost.crashpatch.client.crashes.CrashScanner
import org.polyfrost.crashpatch.hooks.CrashReportHook
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.internal.OneConfig
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import java.awt.Desktop
import java.io.File
import java.net.URI

class CrashUI @JvmOverloads constructor(
    private val scanText: String,
    private val file: File?,
    private val susThing: String,
    private val type: GuiType = GuiType.NORMAL,
    val throwable: Throwable? = null
) : ComposeScreen() {

    @JvmOverloads
    constructor(report: CrashReport, type: GuiType = GuiType.NORMAL) : this(
        report.getFriendlyReport(ReportType.CRASH),
        report.saveFile?.toFile(),
        (report as CrashReportHook).suspectedMod,
        type,
        report.exception
    )

    companion object {
        var leaveWorldCrash = false
        var currentInstance: Screen? = null
            private set
        var currentUI: CrashUI? = null
            private set
    }

    init {
        try {
            val initialized = OneConfig::class.java.getDeclaredField("initialized")
            initialized.isAccessible = true
            if (!initialized.getBoolean(OneConfig.INSTANCE)) {
                val registerEventHandlers = OneConfig::class.java.getDeclaredMethod("registerEventHandlers")
                registerEventHandlers.isAccessible = true
                registerEventHandlers.invoke(OneConfig.INSTANCE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val crashScan: CrashScan? by lazy {
        CrashScanner.scan(scanText, type == GuiType.DISCONNECT)
            ?.takeIf { it.solutions.isNotEmpty() }
    }

    var shouldCrash = false

    fun create(): Screen {
        currentUI = this
        currentInstance = this
        return this
    }

    override fun removed() {
        leaveWorldCrash = false
        currentInstance = null
        currentUI = null
        super.removed()
    }

    @Composable
    override fun compose() {
        val clipboardManager = LocalClipboardManager.current
        var selectedSolution by remember(crashScan) {
            mutableStateOf(crashScan?.solutions?.firstOrNull())
        }
        var statusText by remember { mutableStateOf<String?>(null) }
        val tabScroll = rememberScrollState()
        val solutionScroll = rememberScrollState()
        val pageScroll = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF151515))
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(pageScroll)
                    .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF454545), RoundedCornerShape(12.dp))
                    .padding(24.dp),
            ) {
                BasicText(
                    text = tr(type.title),
                    style = TextStyle(color = Color.White, fontSize = 26.sp),
                )
                Spacer(Modifier.height(12.dp))
                BasicText(
                    text = tr("${type.title}.desc.1"),
                    style = TextStyle(color = Color(0xFFD0D0D0), fontSize = 14.sp),
                )
                if (type != GuiType.INIT || crashScan != null) {
                    BasicText(
                        text = tr("${type.title}.desc.2"),
                        style = TextStyle(color = Color(0xFFD0D0D0), fontSize = 14.sp),
                    )
                }

                Spacer(Modifier.height(24.dp))
                BasicText(
                    text = tr(if (type == GuiType.DISCONNECT) "crashpatch.disconnect.cause" else "crashpatch.crash.cause"),
                    style = TextStyle(color = Color.White, fontSize = 17.sp),
                )
                Spacer(Modifier.height(8.dp))
                BasicText(
                    text = susThing,
                    style = TextStyle(color = Color(0xFF6BA6FF), fontSize = 19.sp),
                )

                if (!crashScan?.solutions.isNullOrEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF4A4A4A), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tabScroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            crashScan?.solutions?.forEach { solution ->
                                val selected = selectedSolution == solution
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (selected) Color(0xFF4D8BFF) else Color(0xFF393939),
                                            shape = RoundedCornerShape(6.dp),
                                        )
                                        .clickable { selectedSolution = solution }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    BasicText(
                                        text = solution.name,
                                        style = TextStyle(
                                            color = if (selected) Color.White else Color(0xFFE0E0E0),
                                            fontSize = 13.sp,
                                        ),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(Color(0xFF1B1B1B), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                        ) {
                            BasicText(
                                text = selectedSolution?.solutions?.joinToString("\n").orEmpty(),
                                style = TextStyle(color = Color(0xFFD9D9D9), fontSize = 12.sp),
                                modifier = Modifier.verticalScroll(solutionScroll),
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionButton("Copy solutions") {
                                val text = selectedSolution?.solutions?.joinToString("\n")
                                if (text.isNullOrBlank()) return@ActionButton
                                clipboardManager.setText(AnnotatedString(text))
                                statusText = "Copied solutions to clipboard."
                            }
                            ActionButton("Upload solutions") {
                                val solution = selectedSolution ?: return@ActionButton
                                val body = solution.solutions.joinToString(separator = "\n") + "\n\n" +
                                        (if (!solution.isCrashReport) scanText else "")
                                val link = LogUploader.upload(body)
                                clipboardManager.setText(AnnotatedString(link))
                                val opened = runCatching { DesktopHelper.browse(URI.create(link)) }.getOrDefault(false)
                                statusText = if (opened) {
                                    "Uploaded and opened link. Copied to clipboard."
                                } else {
                                    "Uploaded link copied to clipboard."
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                BasicText(
                    text = tr("crashpatch.discord.prompt"),
                    style = TextStyle(color = Color.White, fontSize = 15.sp),
                )
                Spacer(Modifier.height(8.dp))
                BasicText(
                    text = tr("crashpatch.link.discord.polyfrost"),
                    style = TextStyle(color = Color(0xFF6BA6FF), fontSize = 15.sp),
                    modifier = Modifier.clickable {
                        runCatching { DesktopHelper.browse(URI.create(tr("crashpatch.link.discord.polyfrost"))) }
                    },
                )

                if (!statusText.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    BasicText(
                        text = statusText!!,
                        style = TextStyle(color = Color(0xFF9ED9A2), fontSize = 13.sp),
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(tr("crashpatch.continue"), primary = true) {
                        if (type == GuiType.INIT) {
                            shouldCrash = true
                        } else {
                            client.setScreen(null)
                        }
                    }
                    ActionButton(tr("crashpatch.log")) {
                        val target = file ?: return@ActionButton
                        runCatching {
                            DesktopHelper.executeIfDesktop(Desktop.Action.OPEN) { desktop ->
                                desktop.open(target)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionButton(
        text: String,
        primary: Boolean = false,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (primary) Color(0xFF4D8BFF) else Color(0xFF2D2D2D),
                    RoundedCornerShape(8.dp),
                )
                .border(1.dp, if (primary) Color(0xFF4D8BFF) else Color(0xFF4F4F4F), RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            BasicText(
                text = text,
                style = TextStyle(color = Color.White, fontSize = 14.sp),
            )
        }
    }

    private fun tr(key: String): String = runCatching { I18n.get(key) }.getOrDefault(key)

    enum class GuiType(val title: String) {
        INIT("crashpatch.init"), NORMAL("crashpatch.crash"), DISCONNECT("crashpatch.disconnect")
    }
}