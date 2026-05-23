package com.englishspeakingpartner.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.englishspeakingpartner.app.data.UserSettings
import com.englishspeakingpartner.app.speech.SpeechRecognitionManager
import com.englishspeakingpartner.app.speech.TtsPlayerManager
import com.englishspeakingpartner.app.vm.AppViewModelFactory
import com.englishspeakingpartner.app.vm.HistoryViewModel
import com.englishspeakingpartner.app.vm.PracticeViewModel
import com.englishspeakingpartner.app.vm.SettingsViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Orange = Color(0xFFF6A15A)
private val OrangeLight = Color(0xFFFFF3E4)
private val Ink = Color(0xFF253238)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as EnglishSpeakingPartnerApp
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = Orange,
                    secondary = Color(0xFF6FB7A8),
                    background = Color(0xFFFFFBF7),
                    surface = Color.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFFBF7)) {
                    AppNavGraph(AppViewModelFactory(app.container))
                }
            }
        }
    }
}

@Composable
fun AppNavGraph(factory: AppViewModelFactory) {
    val nav = rememberNavController()
    val practiceViewModel: PracticeViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStart = { nav.navigate("level") },
                onHistory = { nav.navigate("history") },
                onSettings = { nav.navigate("settings") }
            )
        }
        composable("level") {
            LevelSelectScreen(
                onBack = { nav.popBackStack() },
                onLevel = { nav.navigate("topic/$it") }
            )
        }
        composable(
            route = "topic/{level}",
            arguments = listOf(navArgument("level") { type = NavType.StringType })
        ) { entry ->
            val level = entry.arguments?.getString("level").orEmpty()
            TopicSelectScreen(
                level = level,
                onBack = { nav.popBackStack() },
                onTopic = { topic -> nav.navigate("practice/$level/${Uri.encode(topic)}") }
            )
        }
        composable(
            route = "practice/{level}/{topic}",
            arguments = listOf(
                navArgument("level") { type = NavType.StringType },
                navArgument("topic") { type = NavType.StringType }
            )
        ) { entry ->
            PracticeCallScreen(
                level = entry.arguments?.getString("level").orEmpty(),
                topic = Uri.decode(entry.arguments?.getString("topic").orEmpty()),
                viewModel = practiceViewModel,
                onBack = { nav.popBackStack("home", false) }
            )
        }
        composable("history") {
            HistoryScreen(viewModel = historyViewModel, onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                historyViewModel = historyViewModel,
                onBack = { nav.popBackStack() }
            )
        }
    }
}

@Composable
fun HomeScreen(onStart: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrangeLight)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Orange),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.RecordVoiceOver, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("AI 英语口语搭子", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))
        Text(
            "像打电话一样练英语，每句话都有中文翻译、评分和地道表达建议",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5C676C)
        )
        Spacer(Modifier.height(36.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
            Text("开始练习")
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onHistory, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6FB7A8))) {
                Icon(Icons.Default.History, null)
                Spacer(Modifier.width(6.dp))
                Text("历史记录")
            }
            Button(onClick = onSettings, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF546E7A))) {
                Icon(Icons.Default.Settings, null)
                Spacer(Modifier.width(6.dp))
                Text("设置")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(onBack: () -> Unit, onLevel: (String) -> Unit) {
    ScreenScaffold(title = "选择难度", onBack = onBack) {
        val levels = listOf(
            Triple("junior", "初中", "使用简单词汇、简单句型和生活话题，适合基础口语。"),
            Triple("senior", "高中", "使用校园、兴趣和观点表达，适合考试口语和日常交流。"),
            Triple("college", "大学", "更自然完整，包含学习、实习、社交和职业规划等真实话题。")
        )
        LazyColumn(
            modifier = Modifier.padding(it).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(levels) { level ->
                SelectCard(title = level.second, body = level.third) { onLevel(level.first) }
            }
        }
    }
}

@Composable
fun TopicSelectScreen(level: String, onBack: () -> Unit, onTopic: (String) -> Unit) {
    var custom by remember { mutableStateOf("") }
    val topics = listOf("Daily Life 日常生活", "School Life 校园生活", "Hobbies 兴趣爱好", "Travel 旅行", "Food 食物", "Shopping 购物", "Interview 面试", "Free Talk 自由聊天")
    ScreenScaffold(title = "选择话题", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("当前难度：${levelName(level)}", fontWeight = FontWeight.SemiBold, color = Ink) }
            items(topics) { topic -> SelectCard(title = topic, body = "点击进入通话式口语练习") { onTopic(topic) } }
            item {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("自定义话题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Button(
                    onClick = { if (custom.isNotBlank()) onTopic(custom.trim()) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) { Text("使用自定义话题") }
            }
        }
    }
}

@Composable
fun PracticeCallScreen(level: String, topic: String, viewModel: PracticeViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val player = remember { TtsPlayerManager(context) }
    var showAiChinese by remember { mutableStateOf(true) }
    val playTts: (String?, String?, String?) -> Unit = { url, base64, mime ->
        when {
            !url.isNullOrBlank() -> player.playUrl(url) { viewModel.showMessage(it) }
            !base64.isNullOrBlank() -> player.playBase64(base64, mime) { viewModel.showMessage(it) }
            else -> viewModel.showMessage("后端没有返回可播放的 TTS 音频")
        }
    }
    val speech = remember {
        SpeechRecognitionManager(
            context = context,
            onResult = { viewModel.setListening(false); viewModel.setInput(it); viewModel.send("voice", playTts) },
            onError = { viewModel.setListening(false); viewModel.showMessage(it) }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setListening(true)
            speech.start()
        } else {
            viewModel.showMessage("需要麦克风权限才能语音输入")
        }
    }

    LaunchedEffect(level, topic) { viewModel.start(level, topic) }
    LaunchedEffect(state.isPaused) {
        while (true) {
            delay(1000)
            viewModel.tick()
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            speech.destroy()
            player.release()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${levelName(level)} · $topic", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { Text(formatSeconds(state.elapsedSeconds), modifier = Modifier.padding(end = 12.dp)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = OrangeLight), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, null, tint = Orange)
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isLoading) "AI 正在思考..." else "AI 搭子", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(state.aiReply.english, style = MaterialTheme.typography.titleMedium, color = Ink)
                    if (settings.showChinese) {
                        TextButton(onClick = { showAiChinese = !showAiChinese }) {
                            Text(if (showAiChinese) "收起中文翻译" else "显示中文翻译")
                        }
                        if (showAiChinese) Text(state.aiReply.chinese, color = Color(0xFF617178))
                    }
                    if (state.isLoading) CircularProgressIndicator(color = Orange, modifier = Modifier.padding(top = 8.dp).size(24.dp))
                }
            }

            state.lastResponse?.let { response ->
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("上一句话反馈", fontWeight = FontWeight.Bold, color = Ink)
                                Text("原句：${response.userOriginal}")
                                if (settings.showChinese) Text("中文意思：${response.userTranslationZh}")
                                Text("评分：${response.score.total} / 100", color = Orange, fontWeight = FontWeight.Bold)
                                Text("自然度 ${response.score.naturalness}/30 · 清楚度 ${response.score.clarity}/25 · 语法 ${response.score.grammar}/20")
                                Text("问题：", fontWeight = FontWeight.SemiBold)
                                if (response.problems.isEmpty()) Text("这句表达已经很清楚，继续保持。") else response.problems.forEach {
                                    Text("- ${it.originalPart}: ${it.explanationZh}")
                                }
                                Text("更地道表达：", fontWeight = FontWeight.SemiBold)
                                response.betterExpressions.forEach {
                                    Text(it.english, fontWeight = FontWeight.Medium)
                                    Text("${it.chinese}。${it.whyZh}", color = Color(0xFF617178))
                                }
                                Text(response.encouragementZh, color = Color(0xFF3F7F72))
                            }
                        }
                    }
                }
            } ?: Spacer(Modifier.weight(1f))

            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::setInput,
                label = { Text("输入英文回复") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.setListening(true)
                            speech.start()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.isListening) Color(0xFFE57373) else Color(0xFF6FB7A8))
                ) {
                    Icon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isListening) "聆听中" else "说话")
                }
                Button(onClick = { viewModel.send("text", playTts) }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(6.dp))
                    Text("发送")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { viewModel.replay(playTts) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Replay, "重播") }
                IconButton(onClick = { viewModel.setPaused(!state.isPaused); if (state.isPaused) player.resume() else player.pause() }, modifier = Modifier.weight(1f)) {
                    Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, "暂停继续")
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val rows by viewModel.history.collectAsState()
    var expandedId by remember { mutableStateOf<Long?>(null) }
    ScreenScaffold(title = "历史记录", onBack = onBack, action = {
        IconButton(onClick = viewModel::clear) { Icon(Icons.Default.Delete, null) }
    }) { padding ->
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("还没有练习记录") }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rows) { row ->
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { expandedId = if (expandedId == row.id) null else row.id }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${formatTime(row.timestamp)} · ${levelName(row.level)} · ${row.topic}", color = Color(0xFF617178))
                            Text(row.userOriginal, fontWeight = FontWeight.SemiBold)
                            Text("评分 ${row.scoreTotal}/100 · 地道表达 ${row.aiReplyEnglish}", maxLines = if (expandedId == row.id) 4 else 1, overflow = TextOverflow.Ellipsis)
                            if (expandedId == row.id) {
                                Text("中文：${row.userTranslationZh}")
                                Text("AI：${row.aiReplyEnglish}")
                                Text("AI 中文：${row.aiReplyChinese}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, historyViewModel: HistoryViewModel, onBack: () -> Unit) {
    val settings by settingsViewModel.settings.collectAsState()
    ScreenScaffold(title = "设置", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = settings.backendBaseUrl,
                    onValueChange = { settingsViewModel.update(settings.copy(backendBaseUrl = it)) },
                    label = { Text("后端 API 地址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item { SegmentRow("AI 音色", settings.voice, listOf("default_female", "default_male", "custom")) { settingsViewModel.update(settings.copy(voice = it)) } }
            item { SegmentRow("语速", settings.speed, listOf("slow", "normal", "fast")) { settingsViewModel.update(settings.copy(speed = it)) } }
            item { SettingSwitch("自动播放 AI 英文语音", settings.autoPlay) { settingsViewModel.update(settings.copy(autoPlay = it)) } }
            item { SettingSwitch("显示中文翻译", settings.showChinese) { settingsViewModel.update(settings.copy(showChinese = it)) } }
            item { SettingSwitch("保存历史记录", settings.saveHistory) { settingsViewModel.update(settings.copy(saveHistory = it)) } }
            item {
                Button(onClick = historyViewModel::clear, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(6.dp))
                    Text("清空历史记录")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(title: String, onBack: () -> Unit, action: @Composable () -> Unit = {}, content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { action() }
            )
        },
        content = content
    )
}

@Composable
fun SelectCard(title: String, body: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
            Text(body, color = Color(0xFF617178))
        }
    }
}

@Composable
fun SettingSwitch(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun SegmentRow(title: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (option == value) Orange else Color(0xFFE7ECEE), contentColor = if (option == value) Color.White else Ink)
                ) { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

fun levelName(level: String): String = when (level) {
    "junior" -> "初中"
    "senior" -> "高中"
    "college" -> "大学"
    else -> level
}

fun formatSeconds(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)

fun formatTime(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))
