package com.corner.ui

import AppTheme
import SiteViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideIn
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.corner.bean.Setting
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.util.Paths
import com.corner.database.Config
import com.corner.database.Db
import com.corner.init.initConfig
import com.corner.ui.decompose.component.DefaultSettingComponent
import com.corner.ui.decompose.component.getSetting
import com.corner.ui.scene.BackRow
import com.corner.ui.scene.Dialog
import com.corner.ui.scene.HoverableText
import com.corner.ui.scene.SnackBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.internal.StringUtil
import java.awt.Desktop
import java.net.URI

@Composable
fun SettingItem(modifier: Modifier, setting: Setting, onClick: (Setting) -> Unit) {
    Row(
        modifier
            .clickable {
                onClick(setting)
            }.shadow(3.dp)
            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(4.dp))
            .padding(start = 20.dp, end = 20.dp)
    ) {
        Text(
            setting.label,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (StringUtil.isBlank(setting.value)) "无" else setting.value ?: "",
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp)
                .weight(0.5f),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingScene(component: DefaultSettingComponent, onClickBack: () -> Unit) {
    val model = component.model.subscribeAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var currentChoose by remember { mutableStateOf<Setting?>(null) }
    DisposableEffect("setting") {
        component.sync()
        onDispose {
            SettingStore.write()
        }
    }

    DisposableEffect(model.value.settingList) {
        println("settingList 修改")
        onDispose { }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            BackRow(Modifier.align(Alignment.Start), onClickBack = { onClickBack() }) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "设置",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    OutlinedButton(
                        onClick = { Desktop.getDesktop().open(Paths.userDataRoot()) },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("打开用户数据目录")
                    }
                }
            }
            LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item {
                    val focusRequester = remember { FocusRequester() }
                    val isExpand = remember { mutableStateOf(false) }
                    val setting = remember { model.value.settingList.getSetting(SettingType.VOD) }
                    var vodConfigList = listOf<String?>()
                    LaunchedEffect(isExpand.value){
                        val list:List<Config> = Db.Config.getAll()
                        vodConfigList = list.filter { it.url?.isNotBlank() ?: false }.map { it.url }.toList()
                    }
                    SettingItemTemplate(setting?.label!!) {
                        Box(Modifier.fillMaxSize()) {
                            TextField(
                                value = setting.value ?: "",
                                onValueChange = {
                                    SettingStore.setValue(SettingType.VOD, it)
                                    component.sync()
                                },
                                maxLines = 1,
                                enabled = true,
                                modifier = Modifier.focusRequester(focusRequester)
                                    .fillMaxHeight(0.6f)
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                                    .onFocusEvent {
                                        isExpand.value = it.isFocused
                                    }
                            )
                            DropdownMenu(isExpand.value, {isExpand.value = false}, modifier = Modifier.fillMaxWidth(0.8f)){
                                vodConfigList.forEach{
                                    DropdownMenuItem(modifier = Modifier.fillMaxWidth(), text = { Text(it ?: "") }, onClick = {
                                        setConfig(it)
                                    })
                                }
                            }
                        }
                    }
                }
                item {
                    SettingItemTemplate("日志"){
                        LogButtonList(Modifier){
                            SettingStore.setValue(SettingType.LOG, it)
                            component.sync()
                            SnackBar.postMsg("重启生效")
                        }
                    }
                }
                item {
                    Row(
                        Modifier
                            .clickable {
                            }.shadow(3.dp)
                            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(4.dp))
                            .padding(start = 20.dp, end = 20.dp)
                    ) {
                        Text(
                            "播放器",
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp)
                                .align(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val playerSetting = derivedStateOf {
                            SettingStore.getPlayerSetting()
                        }
                        Switch(playerSetting.value[0] as Boolean, onCheckedChange = {
                            SettingStore.setValue(SettingType.PLAYER, "$it#${playerSetting.value[1]}")
                            if (it) SnackBar.postMsg("使用内置播放器") else SnackBar.postMsg("使用外部播放器 请配置播放器路径")
                            component.sync()
                        }, Modifier.width(100.dp).padding(end = 20.dp).align(Alignment.CenterVertically),
                            thumbContent = {
                                Box(Modifier.size(80.dp)) {
                                    Text(
                                        if (playerSetting.value[0] as Boolean) "内置" else "外置",
                                        Modifier.fillMaxSize().align(Alignment.Center)
                                    )
                                }
                            })
                        // 只有外部播放器时展示
                        if (!(playerSetting.value[0] as Boolean)) {
                            TextField(
                                value = playerSetting.value[1] as String,
                                onValueChange = {
                                    SettingStore.setValue(SettingType.PLAYER, "${playerSetting.value[0]}#$it")
                                    component.sync()
                                },
                                maxLines = 1,
                                enabled = true,
                                modifier = Modifier.fillMaxHeight(0.8f).fillMaxWidth().align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
                item {
                    Box(Modifier.fillMaxSize().padding(top = 10.dp)) {
                        ElevatedButton(
                            onClick = {
                                SettingStore.reset()
                                component.sync()
                                SnackBar.postMsg("重置设置 重启生效")
                            }, Modifier.fillMaxWidth(0.8f)
                                .align(Alignment.Center)
                        ) {
                            Text("重置")
                        }
                    }
                }
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp)) {
            HoverableText("关于", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) {
                showAboutDialog = true
            }
        }
        AboutDialog(Modifier.fillMaxSize(0.4f), showAboutDialog) { showAboutDialog = false }
        DialogEdit(showEditDialog, onClose = { showEditDialog = false }, currentChoose = currentChoose) {
            component.sync()
        }
    }

}

fun SettingStore.getPlayerSetting(): List<Any> {
    val settingItem = SettingStore.getSettingItem(SettingType.PLAYER.id)
    val split = settingItem.split("#")
    return if (split.size == 1) listOf(false, settingItem) else listOf(split[0].toBoolean(), split[1])
}

@Composable
fun SettingItemTemplate(title: String, content: @Composable () -> Unit) {
    Row(
        Modifier.shadow(3.dp)
            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(4.dp))
            .padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp).align(Alignment.CenterVertically),
            color = MaterialTheme.colorScheme.onBackground
        )
        content()
    }
}

private val logLevel = listOf("INFO", "DEBUG")
@Composable
fun LogButtonList(modifier: Modifier, onClick: (String) -> Unit) {
    val current = derivedStateOf { SettingStore.getSettingItem(SettingType.LOG.id) }
    Box(
        Modifier.padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row {
            logLevel.forEachIndexed { i,t ->
                if(i == 0){
                    SideButton(current.value == t, text = t, type = SideButtonType.LEFT){
                        onClick(it)
                    }
                }else if(i == logLevel.size - 1){
                    SideButton(current.value == t, text = t, type = SideButtonType.RIGHT){
                        onClick(it)

                    }
                }else{
                    SideButton(current.value == t, text = t){
                        onClick(it)
                    }
                }
            }
        }
    }
}

enum class SideButtonType{
    LEFT,MID,RIGHT
}

@Composable
fun SideButton(
    choosed: Boolean,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors().copy(disabledContainerColor = MaterialTheme.colorScheme.background),
    type: SideButtonType = SideButtonType.MID,
    text: String,
    onClick: (String) -> Unit
) {
    val textColor = if(choosed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Text(text = text, modifier = Modifier.clickable { onClick(text) }
        .defaultMinSize(50.dp)
        .drawWithCache {
//            val width = size.width * 1.1f
//            val height = size.height * 1.1f
            val width = size.width
            val height = size.height
            val color = if (choosed) buttonColors.containerColor else buttonColors.disabledContainerColor

            onDrawBehind {
                val rectOffset = when(type){
                    SideButtonType.LEFT -> Offset(height / 2, 0f)
                    SideButtonType.MID -> Offset.Zero
                    SideButtonType.RIGHT -> Offset.Zero
                }
                if (type == SideButtonType.LEFT) {
                    drawCircle(
                        color = color,
                        radius = height / 2,
                        center = Offset(height / 2, height / 2),
                        style = Fill
                    )
                }
                val rectSize = Size(width - height / 2, height)
                drawRect(
                    color = color,
                    topLeft = rectOffset,
                    size = rectSize,
                    style = Fill,
                )
                if (type == SideButtonType.RIGHT) {
                    drawCircle(
                        color = color,
                        radius = height / 2,
                        center = Offset(size.width - height / 2, height / 2),
                        style = Fill
                    )
                }
            }
        }, textAlign = TextAlign.Center, color = textColor)
}

@Preview
@Composable
fun previewSideButton() {
    AppTheme {
        Row(Modifier.fillMaxSize()) {
            LogButtonList(Modifier){}
//            SideButton(true, text = "test12312", type = SideButtonType.LEFT) {}
//
//            SideButton(false, text = "test1j计划熊㩐动甮的", type = SideButtonType.RIGHT) {}
        }
//        Column(Modifier.fillMaxSize()) {
//            SideButton(Color.Blue, false, "test1") {}
//        }
    }
}

@Preview
@Composable
fun previewLogButtonList() {
    AppTheme {
        LogButtonList(Modifier){}
    }
}

fun setConfig(textFieldValue: String?){
    if (textFieldValue == null || textFieldValue == "") {
        SnackBar.postMsg("不可为空")
        return
    }
    val config = Db.Config.find(textFieldValue, ConfigType.SITE.ordinal.toLong())
    if (config == null) {
        Db.Config.save(
            type = ConfigType.SITE.ordinal.toLong(),
            url = textFieldValue
        )
    } else {
        Db.Config.updateUrl(config.id, textFieldValue)
    }
    SettingStore.setValue(SettingType.VOD, textFieldValue)
    ApiConfig.api.cfg.value = Db.Config.find(textFieldValue, ConfigType.SITE.ordinal.toLong())
    initConfig()
}

@Composable
fun DialogEdit(
    showEditDialog: Boolean,
    onClose: () -> Unit,
    currentChoose: Setting?,
    onValueChange: (v: String) -> Unit
) {
    var textFieldValue by remember { mutableStateOf(currentChoose?.value) }
    val focusRequester = remember { FocusRequester() }
    if (showEditDialog) {
        LaunchedEffect(Unit) {
            textFieldValue = currentChoose?.value
            focusRequester.requestFocus()
        }
    }
    Dialog(
        modifier = Modifier.fillMaxWidth(0.5f),
        showEditDialog,
        onClose = {
            onClose()
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 25.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(currentChoose?.label ?: ""/*, color = MaterialTheme.colors.onBackground*/)
                Spacer(modifier = Modifier.size(35.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(0.8f).focusRequester(focusRequester),
                    enabled = true,
                    value = textFieldValue ?: "",
                    onValueChange = { text -> textFieldValue = text },
                    interactionSource = MutableInteractionSource(),
                    maxLines = 2
                )
            }
            Button(modifier = Modifier.align(Alignment.End), onClick = {
                SiteViewModel.viewModelScope.launch {
                    when (currentChoose!!.id) {
                        "vod" -> {
                            setConfig(textFieldValue)
                        }
                    }
                }.invokeOnCompletion {
                    onValueChange(textFieldValue ?: "")
                    onClose()
                }
            }) {
                Text("确认")
            }
        }
    }
}

@Composable
fun AboutDialog(modifier: Modifier, showAboutDialog: Boolean, onClose: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    Dialog(modifier, showAboutDialog,
        onClose = {
            visible = false
            onClose()
        }) {
        LaunchedEffect(Unit) {
            delay(500)
            visible = true
        }
        Box(modifier.padding(20.dp).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    modifier = Modifier.fillMaxWidth(),
                    visible = visible,
                    enter = fadeIn() + slideIn(animationSpec = tween(800), initialOffset = { i -> IntOffset(0, -20) })
                ) {
                    Column {
                        Image(
                            painter = painterResource("/pic/avatar.png"),
                            contentDescription = "avatar",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.padding(8.dp)
                                .size(100.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(50))
                        )
                        AboutItem("作者", Modifier.align(Alignment.CenterHorizontally)) {
                            HoverableText("Greatwallcorner") {
                                openBrowser("https://github.com/Greatwallcorner")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(25.dp))
            }
            Column(Modifier.align(Alignment.BottomCenter)) {
                OutlinedButton(
                    onClick = { openBrowser("https://t.me/tv_multiplatform") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("TG讨论群")
                }

                Row(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 15.dp)) {
                    Icon(
                        Icons.Default.Code,
                        "source code",
                        modifier = Modifier.padding(5.dp).align(Alignment.CenterVertically)
                    )
                    HoverableText("源代码") {
                        openBrowser("https://github.com/Greatwallcorner/TV-Multiplatform")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun previewAboutDialog() {
    AppTheme {
        AboutDialog(Modifier, true) {}
    }
}

fun openBrowser(url: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}

@Composable
fun AboutItem(title: String, modifier: Modifier, content: @Composable (Modifier) -> Unit) {
    Row(modifier.padding(vertical = 5.dp, horizontal = 15.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 5.dp)
        )
        content(Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
@Preview
fun SettingItem() {
    AppTheme(useDarkTheme = false) {
//        SettingItem(
//            Modifier,
//            "点播", "PeopleInSpaceTheme"
//        ) {
//
//        }
    }
}