/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.russoviller.cryogenic.ui.screens.content.elements

import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.russoviller.cryogenic.game.account.wardrobe.SkinLibrary
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import com.russoviller.cryogenic.ui.components.CryoDropdownMenu as DropdownMenu
import com.russoviller.cryogenic.ui.components.CryoDropdownMenuItem as DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.petlib.ArmModel
import com.russoviller.petlib.PetAnimation
import com.russoviller.cryogenic.BuildKeys
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.account.Account
import com.russoviller.cryogenic.game.account.AccountType
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.game.account.accountErrorText
import com.russoviller.cryogenic.game.account.accountUUID
import com.russoviller.cryogenic.game.account.auth_server.data.AuthServer
import com.russoviller.cryogenic.game.account.auth_server.models.AuthResult
import com.russoviller.cryogenic.game.account.getAccountTypeName
import com.russoviller.cryogenic.game.account.getUUIDFromUserName
import com.russoviller.cryogenic.game.account.isLocalAccount
import com.russoviller.cryogenic.game.account.isMicrosoftAccount
import com.russoviller.cryogenic.game.account.isCryoCapeAllowed
import com.russoviller.cryogenic.game.account.isSkinChangeAllowed
import com.russoviller.cryogenic.game.account.wardrobe.EmptyCape
import com.russoviller.cryogenic.game.account.wardrobe.SkinModelType
import com.russoviller.cryogenic.game.account.wardrobe.capeLocalRes
import com.russoviller.cryogenic.game.account.wardrobe.toArmModel
import com.russoviller.cryogenic.game.account.yggdrasil.PlayerProfile
import com.russoviller.cryogenic.game.account.yggdrasil.findUsing
import com.russoviller.cryogenic.game.account.yggdrasil.getFile
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.path.URL_MINECRAFT_PURCHASE
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.components.BaseIconTextButton
import com.russoviller.cryogenic.ui.components.ButtonPosition
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.ImePanContainer
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.OwnOutlinedTextField
import com.russoviller.cryogenic.ui.components.PetModelView
import com.russoviller.cryogenic.ui.components.PositionButton
import com.russoviller.cryogenic.ui.components.PositionFilledTonalButton
import com.russoviller.cryogenic.ui.components.RadioCard
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.SimpleListDialog
import com.russoviller.cryogenic.ui.components.SingleLineTextCheck
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.content.elements.ChangeSkin.SkinModel
import com.russoviller.cryogenic.ui.screens.main.control_editor.InfoLayoutTextItem
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.itemColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.ui.theme.onItemColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import com.russoviller.cryogenic.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.regex.Pattern
import kotlin.math.roundToInt
import com.russoviller.cryogenic.ui.components.safeRadialGradient

private const val TAG = "AccountElements"

/** 账号登录菜单操作状态 */
sealed interface LoginMenuOperation {
    data object None : LoginMenuOperation

    /** 呼出登陆账号菜单，将所有登录方式放到一个对话框中展示 */
    data object Login : LoginMenuOperation
}

/**
 * 微软登录的操作状态
 */
sealed interface MicrosoftLoginOperation {
    data object None : MicrosoftLoginOperation

    /** 微软账号相关提示Dialog流程 */
    data object Tip : MicrosoftLoginOperation

    /** 选择微软登录验证方式的Dialog流程 */
    data object SelectBrowser : MicrosoftLoginOperation
}

/**
 * 离线登陆的操作状态
 */
sealed interface LocalLoginOperation {
    data object None : LocalLoginOperation

    /** 编辑用户名流程 */
    data object Edit : LocalLoginOperation

    /** 创建账号流程 */
    data class Create(val userName: String, val userUUID: String?) : LocalLoginOperation

    /** 警告非法用户名流程 */
    data class Alert(val userName: String, val userUUID: String?) : LocalLoginOperation
}

/**
 * 添加认证服务器时的状态
 */
sealed interface ServerOperation {
    data object None : ServerOperation
    /** 添加认证服务器对话框 */
    data object AddNew : ServerOperation
    /** 删除认证服务器对话框 */
    data class Delete(val server: AuthServer) : ServerOperation
    data class OnThrowable(val throwable: Throwable) : ServerOperation
}

/**
 * 账号操作的状态
 */
sealed interface AccountOperation {
    data object None : AccountOperation
    data class Delete(val account: Account) : AccountOperation
    data class OnFailed(val th: Throwable) : AccountOperation

    /** 账号凭据已被服务端拒绝，需要重新登录 */
    data class OnRelogin(
        val account: Account,
        val logging: Boolean = false,
        val error: Throwable? = null
    ) : AccountOperation
}

/**
 * 更换账号皮肤的状态
 */
sealed interface AccountSkinOperation {
    data object None : AccountSkinOperation

    /** 修改皮肤主对话框 */
    data class ChangeSkin(val account: Account) : AccountSkinOperation
}

/**
 * 认证服务器登陆时的状态
 */
sealed interface OtherLoginOperation {
    data object None : OtherLoginOperation

    /** 账号登陆（输入账号密码Dialog）流程 */
    data class OnLogin(val server: AuthServer) : OtherLoginOperation

    /** 登陆失败流程 */
    data class OnFailed(val th: Throwable) : OtherLoginOperation

    /** 账号存在多角色的情况，多角色处理流程 */
    data class SelectRole(
        val profiles: List<AuthResult.AvailableProfiles>,
        val selected: (AuthResult.AvailableProfiles) -> Unit
    ) : OtherLoginOperation
}

@Composable
fun PlayerFace(
    modifier: Modifier = Modifier,
    account: Account,
    avatarSize: Dp = 64.dp,
    refreshKey: Any? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
    val avatarBitmap by produceState<ImageBitmap?>(null, account, refreshKey, refreshWardrobe, density) {
        value = withContext(Dispatchers.IO) {
            getSkinAvatarFromAccount(
                context = context,
                account = account,
                size = avatarSize,
                density = density
            ).asImageBitmap()
        }
    }

    val avatar = avatarBitmap
    if (avatar != null) {
        Image(
            modifier = modifier.size(avatarSize),
            bitmap = avatar,
            contentDescription = null
        )
    } else {
        Spacer(modifier = modifier.size(avatarSize))
    }
}

@Composable
fun AccountItem(
    modifier: Modifier = Modifier,
    currentAccount: Account?,
    account: Account,
    avatarSize: Dp = 46.dp,
    color: Color = itemColor(),
    contentColor: Color = onItemColor(),
    enabled: Boolean = true,
    refreshKey: Any? = null,
    onSelected: (Account) -> Unit = {},
    openChangeSkinDialog: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onCopyUUID: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val selected = currentAccount?.uniqueUUID == account.uniqueUUID
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .graphicsLayer { scaleY = scale.value; scaleX = scale.value }
            .then(if (selected) Modifier.cryoGlow(20.dp, selected = true, strength = 0.8f) else Modifier)
            .cryoGlass(shape = shape, glow = false, cornerRadius = 20.dp, selected = selected)
            .clip(shape)
            .clickable(enabled = enabled && !selected) { onSelected(account) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize + 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .cryoBackground(
                    if (selected) cryoGradient()
                    else Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.35f), CryoGlassColors.Navy.copy(alpha = 0.6f)))
                )
                .padding(2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CryoGlassColors.Deep),
            contentAlignment = Alignment.Center
        ) {
            PlayerFace(
                account = account,
                avatarSize = avatarSize,
                refreshKey = refreshKey
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = account.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccountTag(text = getAccountTypeName(account), accent = CryoGlassColors.Ice)
                if (selected) {
                    AccountTag(text = stringResource(R.string.cryo_account_active), accent = Color(0xFF7CF5A8))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AccountActionButton(
                iconRes = R.drawable.ic_refresh,
                description = stringResource(R.string.generic_refresh),
                enabled = account.accountType != AccountType.LOCAL.tag,
                onClick = onRefreshClick
            )
            AccountActionButton(
                iconRes = R.drawable.ic_copy_all_outlined,
                description = stringResource(R.string.account_local_uuid_copy),
                onClick = onCopyUUID
            )
            AccountActionButton(
                iconRes = R.drawable.ic_delete_outlined,
                description = stringResource(R.string.generic_delete),
                danger = true,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun AccountTag(text: String, accent: Color) {
    com.russoviller.cryogenic.ui.components.CryoTag(text = text, accent = accent)
}

@Composable
private fun AccountActionButton(
    iconRes: Int,
    description: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (danger) Color(0xFFFF6B8A) else CryoGlassColors.Ice
    Box(
        modifier = Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.45f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(19.dp),
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = tint
        )
    }
}

@Composable
fun LoginMenuDialog(
    onDismissRequest: () -> Unit,
    onMicrosoftLogin: () -> Unit,
    onLocalLogin: () -> Unit,
    authServers: List<AuthServer>,
    onAuthServerLogin: (server: AuthServer) -> Unit,
    onAddAuthServer: () -> Unit,
    onDeleteAuthServer: (server: AuthServer) -> Unit,
    onElyByLogin: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(all = 16.dp)
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight()
                .fillMaxWidth(0.6f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .fillMaxWidth()
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight())),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp)
                                .padding(start = 12.dp, end = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            //微软登录
                            LoginItem(
                                modifier = Modifier.fillMaxWidth(),
                                title = stringResource(R.string.account_type_microsoft),
                                onClick = {
                                    onMicrosoftLogin()
                                    onDismissRequest()
                                }
                            )
                            //离线登录
                            LoginItem(
                                modifier = Modifier.fillMaxWidth(),
                                title = stringResource(R.string.account_type_local),
                                onClick = {
                                    onLocalLogin()
                                    onDismissRequest()
                                }
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                start = 6.dp,
                                top = 12.dp,
                                end = 12.dp,
                                bottom = 12.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                //添加认证服务器
                                InfoLayoutTextItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    title = stringResource(R.string.account_add_new_server_button),
                                    showArrow = true,
                                    onClick = {
                                        onAddAuthServer()
                                        onDismissRequest()
                                    }
                                )
                            }

                            item {
                                LoginItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    title = stringResource(R.string.cryo_elyby_login),
                                    onClick = {
                                        onElyByLogin()
                                        onDismissRequest()
                                    }
                                )
                            }

                            items(authServers.filterNot { it.baseUrl.contains("ely.by", ignoreCase = true) }) { server ->
                                LoginItem(
                                    title = server.serverName,
                                    icon = {
                                        IconButton(
                                            modifier = Modifier.size(22.dp),
                                            onClick = {
                                                onDeleteAuthServer(server)
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete_outlined),
                                                contentDescription = stringResource(R.string.generic_delete)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onAuthServerLogin(server)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }

                    CryoButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                        onClick = onDismissRequest
                    ) {
                        Text(stringResource(R.string.generic_close))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480)
@Composable
private fun PreviewLoginMenuDialog() {
    MaterialExpressiveTheme {
        LoginMenuDialog(
            onDismissRequest = {},
            onMicrosoftLogin = {},
            onLocalLogin = {},
            authServers = emptyList(),
            onAuthServerLogin = {},
            onAddAuthServer = {},
            onDeleteAuthServer = {}
        )
    }
}

@Composable
fun LoginItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit = @Composable {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(R.drawable.ic_login),
            contentDescription = null
        )
    },
    onClick: () -> Unit
) {
    InfoLayoutTextItem(
        modifier = modifier,
        title = title,
        icon = icon,
        onClick = onClick
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 120)
@Composable
private fun PreviewLoginItem() {
    MaterialExpressiveTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LoginItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 32.dp),
                title = stringResource(R.string.account_type_microsoft),
                onClick = {}
            )
        }
    }
}

@Composable
fun MicrosoftLoginTipDialog(
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {},
    openLink: (url: String) -> Unit = {}
) {
    SimpleAlertDialog(
        title = stringResource(R.string.account_supporting_microsoft_tip_title),
        text = {
            Text(
                text = stringResource(R.string.account_supporting_microsoft_tip_link_text),
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow {
                IconTextButton(
                    onClick = {
                        openLink(URL_MINECRAFT_PURCHASE)
                    },
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = null,
                    text = stringResource(R.string.account_supporting_microsoft_tip_link_purchase)
                )
                IconTextButton(
                    onClick = {
                        openLink("https://www.minecraft.net/msaprofile/mygames/editprofile")
                    },
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = null,
                    text = stringResource(R.string.account_supporting_microsoft_tip_link_make_gameid)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.account_supporting_microsoft_tip_hint_t1),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t2))
                    append(
                        stringResource(
                            R.string.account_supporting_microsoft_tip_hint_t3,
                            BuildKeys.LAUNCHER_NAME
                        )
                    )
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t4))
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t5))
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t6))
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.account_supporting_microsoft_tip_hint_t7))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.account_supporting_microsoft_tip_hint_t8))
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmText = stringResource(R.string.account_login),
        onConfirm = onConfirm,
        onCancel = onDismissRequest,
        onDismissRequest = onDismissRequest
    )
}

@Composable
fun MicrosoftLoginMethodDialog(
    onDismissRequest: () -> Unit,
    onSystemBrowser: () -> Unit,
    onInternalBrowser: () -> Unit
) {
    LaunchedEffect(Unit) {
        onInternalBrowser()
    }
}

private val localNamePattern = Pattern.compile("[^a-zA-Z0-9_]")

@Composable
fun LocalLoginDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (isUserNameInvalid: Boolean, userName: String, userUUID: String?) -> Unit,
    openLink: (url: String) -> Unit
) {
    /** 用户输入的用户名 */
    var userName by rememberSaveable { mutableStateOf("") }

    /** 用户名是否无效 */
    var isUserNameInvalid by rememberSaveable { mutableStateOf(false) }

    /** 用户编辑了UUID */
    var userEditedUUID by rememberSaveable { mutableStateOf(false) }

    /** 用户输入的UUID */
    var userUUID by rememberSaveable { mutableStateOf("") }

    /** 根据用户名生成的待定UUID */
    val pendingUUID = remember(userName) {
        runCatching {
            getUUIDFromUserName(userName).toString()
        }.getOrElse {
            ""
        }.also { uuid ->
            if (!userEditedUUID) userUUID = uuid
        }
    }

    /** 用户UUID是否无效 */
    val isUserUUIDInvalid: Boolean = remember(userUUID) {
        userUUID.isNotEmpty() && runCatching {
            accountUUID(userUUID)
            false
        }.getOrElse {
            true
        }
    }

    var editUUID by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        ImePanContainer(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.account_local_create_account),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.size(16.dp))

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .weight(1f, fill = false)
                            .verticalScrollWithBar(state = scrollState)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SingleLineTextCheck(
                            text = userName,
                            onSingleLined = { userName = it }
                        )

                        OwnOutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = userName,
                            onValueChange = {
                                userName = it
                            },
                            isError = isUserNameInvalid,
                            label = { Text(text = stringResource(R.string.account_label_username)) },
                            supportingText = {
                                val errorText = when {
                                    userName.isEmpty() -> stringResource(R.string.account_supporting_username_invalid_empty)
                                    userName.length <= 2 -> stringResource(R.string.account_supporting_username_invalid_short)
                                    userName.length > 16 -> stringResource(R.string.account_supporting_username_invalid_long)
                                    localNamePattern.matcher(userName)
                                        .find() -> stringResource(R.string.account_supporting_username_invalid_illegal_characters)

                                    else -> ""
                                }.also {
                                    isUserNameInvalid = it.isNotEmpty()
                                }
                                if (isUserNameInvalid) {
                                    Text(text = errorText)
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconTextButton(
                                onClick = {
                                    openLink(URL_MINECRAFT_PURCHASE)
                                },
                                painter = painterResource(R.drawable.ic_link),
                                contentDescription = null,
                                text = stringResource(R.string.account_supporting_microsoft_tip_link_purchase)
                            )

                            //打开高级设置
                            BaseIconTextButton(
                                onClick = {
                                    editUUID = !editUUID
                                },
                                icon = { iconModifier ->
                                    val rotate by animateFloatAsState(
                                        if (editUUID) 0f
                                        else 180f
                                    )

                                    Icon(
                                        modifier = iconModifier
                                            .size(24.dp)
                                            .rotate(rotate),
                                        painter = painterResource(R.drawable.ic_arrow_drop_up_rounded),
                                        contentDescription = null
                                    )
                                },
                                text = stringResource(R.string.account_advanced)
                            )
                        }

                        //编辑自定义 UUID
                        AnimatedVisibility(
                            visible = editUUID
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Spacer(modifier = Modifier.size(8.dp))

                                SingleLineTextCheck(
                                    text = userUUID,
                                    onSingleLined = { userUUID = it }
                                )

                                OwnOutlinedTextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = userUUID,
                                    onValueChange = {
                                        userUUID = it
                                        userEditedUUID = true
                                    },
                                    isError = isUserUUIDInvalid,
                                    label = { Text(text = stringResource(R.string.account_local_uuid)) },
                                    supportingText = {
                                        if (isUserUUIDInvalid) {
                                            Text(text = stringResource(R.string.account_local_uuid_invalid))
                                        }
                                    },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large
                                )

                                //关于 UUID 的提示
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(all = 8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.account_local_uuid_tip_1),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.account_local_uuid_tip_2),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.account_local_uuid_tip_3),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.account_local_uuid_tip_4),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CryoTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismissRequest
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                        CryoButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (userName.isNotEmpty()) {
                                    if (userUUID.isNotEmpty()) {
                                        runCatching {
                                            val uuid = accountUUID(userUUID)
                                            val uuidString = accountUUID(uuid)
                                            onConfirm(isUserNameInvalid, userName, uuidString)
                                        }
                                    } else {
                                        //如果未填写UUID，则默认使用待定UUID
                                        onConfirm(
                                            isUserNameInvalid,
                                            userName,
                                            pendingUUID.takeIf { it.isNotEmpty() })
                                    }
                                }
                            }
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtherServerLoginDialog(
    server: AuthServer,
    onRegisterClick: (url: String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onConfirm: (email: String, password: String) -> Unit = { _, _ -> }
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val confirmAction = { //确认操作
        if (email.isNotEmpty() && password.isNotEmpty()) {
            onConfirm(email, password)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        ImePanContainer(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = server.serverName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.size(16.dp))

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .weight(1f, fill = false)
                            .verticalScrollWithBar(state = scrollState)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val passwordFocus = remember { FocusRequester() }
                        val focusManager = LocalFocusManager.current

                        SingleLineTextCheck(
                            text = email,
                            onSingleLined = { email = it }
                        )

                        OwnOutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = email,
                            onValueChange = {
                                email = it
                            },
                            isError = email.isEmpty(),
                            label = { Text(text = stringResource(R.string.account_label_email)) },
                            supportingText = {
                                if (email.isEmpty()) {
                                    Text(text = stringResource(R.string.account_supporting_email_invalid_empty))
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    //自动跳到密码输入框，无缝衔接
                                    passwordFocus.requestFocus()
                                }
                            ),
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )

                        Spacer(modifier = Modifier.size(8.dp))
                        /** 是否显示密码 */
                        var showPassword by rememberSaveable { mutableStateOf(false) }

                        SingleLineTextCheck(
                            text = password,
                            onSingleLined = { password = it }
                        )

                        OwnOutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(passwordFocus),
                            value = password,
                            onValueChange = {
                                password = it
                            },
                            isError = password.isEmpty(),
                            label = { Text(text = stringResource(R.string.account_label_password)) },
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Transparent,
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        painter = painterResource(
                                            if (showPassword) {
                                                R.drawable.ic_visibility_outlined
                                            } else {
                                                R.drawable.ic_visibility_off_outlined
                                            }
                                        ),
                                        contentDescription = stringResource(R.string.account_label_password)
                                    )
                                }
                            },
                            supportingText = {
                                if (password.isEmpty()) {
                                    Text(text = stringResource(R.string.account_supporting_password_invalid_empty))
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Password
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    //用户按下返回，甚至可以在这里直接进行登陆
                                    focusManager.clearFocus(true)
                                    confirmAction()
                                }
                            ),
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )
                        if (!server.register.isNullOrEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                IconTextButton(
                                    onClick = {
                                        onRegisterClick(server.register!!)
                                    },
                                    painter = painterResource(R.drawable.ic_link),
                                    contentDescription = null,
                                    text = stringResource(R.string.account_other_login_register)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CryoTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismissRequest
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                        CryoButton(
                            modifier = Modifier.weight(1f),
                            onClick = confirmAction
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MicrosoftReloginDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    SimpleAlertDialog(
        title = stringResource(R.string.account_relogin_title),
        text = {
            Text(text = stringResource(R.string.account_relogin_microsoft_message))
        },
        confirmText = stringResource(R.string.account_relogin),
        onConfirm = onConfirm,
        onCancel = onDismissRequest,
        onDismissRequest = onDismissRequest
    )
}

@Composable
fun OtherAccountReloginDialog(
    account: Account,
    logging: Boolean,
    error: Throwable?,
    onDismissRequest: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!logging) onDismissRequest() },
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        ImePanContainer(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = account.accountType ?: stringResource(R.string.account_relogin_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.size(16.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.account_relogin_password_message, account.username),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.size(12.dp))

                    OwnOutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = password,
                        onValueChange = { password = it },
                        enabled = !logging,
                        label = { Text(text = stringResource(R.string.account_label_password)) },
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Transparent,
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    painter = painterResource(
                                        if (showPassword) {
                                            R.drawable.ic_visibility_outlined
                                        } else {
                                            R.drawable.ic_visibility_off_outlined
                                        }
                                    ),
                                    contentDescription = stringResource(R.string.account_label_password)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (password.isNotEmpty() && !logging) onConfirm(password)
                            }
                        ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )

                    error?.let { th ->
                        Spacer(modifier = Modifier.size(8.dp))
                        AndroidStringText(
                            text = accountErrorText(th),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CryoTonalButton(
                            modifier = Modifier.weight(1f),
                            enabled = !logging,
                            onClick = onDismissRequest
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                        CryoButton(
                            modifier = Modifier.weight(1f),
                            enabled = !logging && password.isNotEmpty(),
                            onClick = { onConfirm(password) }
                        ) {
                            MarqueeText(text = stringResource(R.string.account_relogin))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 更改皮肤流程需要让 uri 与皮肤模型深度绑定
 * 重置或者确认更改时，能更方便的处理数据
 */
sealed interface ChangeSkin {
    data object None : ChangeSkin

    data class ChangeSkinData(
        val cacheFile: File,
        val data: SkinModel
    ) : ChangeSkin

    /**
     * 重置离线皮肤
     */
    data object ResetSkin : ChangeSkin

    sealed interface SkinModel {
        /** 拒绝选择皮肤模型 */
        data object Rejected : SkinModel
        data class Model(val model: SkinModelType): SkinModel
        /** @return 获取模型类型 */
        fun getModelType(): SkinModelType = when (this) {
            is Rejected -> SkinModelType.STEVE
            is Model -> model
        }

        /** @return 获取模型类型 */
        fun getModelTypeNullable(): SkinModelType? = when (this) {
            is Rejected -> null
            is Model -> model
        }
    }
}

/**
 * 更改披风流程
 */
sealed interface ChangeCape {
    data object None : ChangeCape
    data class ChangeCapeData(
        val cape: PlayerProfile.Cape
    ) : ChangeCape
}

@Composable
fun ChangeSkinDialog(
    account: Account,
    availableCapes: List<PlayerProfile.Cape>? = null,
    skinState: ChangeSkin,
    onSkinStateChange: (ChangeSkin) -> Unit,
    capeState: ChangeCape,
    onCapeStateChange: (ChangeCape) -> Unit,
    isImportingSkin: Boolean,
    onSkinPicked: (Uri) -> Unit,
    onDismissRequest: () -> Unit,
    onResetSkin: () -> Unit,
    onApplySkin: (File, SkinModelType) -> Unit,
    onApplyCape: (PlayerProfile.Cape) -> Unit,
    onFetchCapes: () -> Unit
) {
    var showCapeSelector by remember { mutableStateOf(false) }
    var showOfflineCapes by remember { mutableStateOf(false) }
    var offlineCapeVersion by remember { mutableStateOf(0) }
    var showMsCapes by remember { mutableStateOf(false) }
    if (showOfflineCapes) {
        OfflineCapeDialog(
            account = account,
            onDismiss = { showOfflineCapes = false },
            onChanged = { offlineCapeVersion++ }
        )
    }

    var isFetchingCapes by remember { mutableStateOf(false) }

    var currentCapeToLoad by remember { mutableStateOf(EmptyCape) }
    var currentUsingCape by remember { mutableStateOf(EmptyCape) }

    LaunchedEffect(availableCapes) {
        if (account.isMicrosoftAccount()) {
            if (availableCapes != null) {
                isFetchingCapes = false
                val currentUsingCape0 = availableCapes.findUsing() ?: EmptyCape
                currentUsingCape = currentUsingCape0
                currentCapeToLoad = currentUsingCape0
            } else {
                isFetchingCapes = true
                onFetchCapes()
            }
        }
    }

    // 加载披风
    val previewCape by produceState<ByteArray?>(null, currentCapeToLoad, availableCapes, offlineCapeVersion) {
        if (account.isLocalAccount()) {
            value = withContext(Dispatchers.IO) {
                runCatching { account.getCapeFile().takeIf { it.exists() }?.readBytes() }.getOrNull()
            }
            return@produceState
        }
        if (!account.isMicrosoftAccount()) return@produceState

        val overrideCape = withContext(Dispatchers.IO) {
            runCatching { account.getCapeOverrideFile().takeIf { it.exists() }?.readBytes() }.getOrNull()
        }
        if (overrideCape != null) {
            value = overrideCape
            return@produceState
        }

        value = withContext(Dispatchers.IO) {
            if (currentCapeToLoad != EmptyCape) {
                // 列表已加载完成才会走到这里
                runCatching {
                    currentCapeToLoad
                        .getFile(PathManager.DIR_ACCOUNT_CAPE)
                        .inputStream()
                        .use { it.readBytes() }
                }.getOrNull()
            } else if (availableCapes == null && account.getCapeFile().exists()) {
                // 列表就绪前若本地有缓存披风则优先显示，列表加载完或切换时更新 ↑
                runCatching {
                    account.getCapeFile()
                        .inputStream()
                        .use { it.readBytes() }
                }.getOrNull()
            } else {
                null
            }
        }
    }

    val skinPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let(onSkinPicked)
        }

    // 皮肤状态变化时重载预览皮肤
    val previewSkin by produceState<ByteArray?>(null, skinState) {
        value = withContext(Dispatchers.IO) {
            when (skinState) {
                ChangeSkin.None -> account.getSkinFile()
                    .takeIf { it.exists() }
                    ?.inputStream()
                    ?.use { it.readBytes() }
                is ChangeSkin.ChangeSkinData -> runCatching {
                    skinState.cacheFile
                        .inputStream()
                        .use { it.readBytes() }
                }.getOrNull()
                ChangeSkin.ResetSkin -> null
            }
        }
    }
    val previewArm = when (skinState) {
        ChangeSkin.None -> account.skinModelType.toArmModel()
        is ChangeSkin.ChangeSkinData -> skinState.data.getModelType().toArmModel()
        ChangeSkin.ResetSkin -> ArmModel.Classic
    }

    val scope = rememberCoroutineScope()
    var libraryVersion by remember { mutableStateOf(0) }
    val library by produceState(emptyList<SkinLibrary.Entry>(), libraryVersion) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val current = account.getSkinFile()
                if (current.exists() && account.skinModelType != SkinModelType.NONE) {
                    SkinLibrary.save(current, account.skinModelType, touch = false)
                }
                SkinLibrary.dedupe()
                SkinLibrary.list()
            }.getOrDefault(emptyList())
        }
    }
    var selectedLibraryFile by remember { mutableStateOf<File?>(null) }
    var showMcSkins by remember { mutableStateOf(false) }
    if (showMcSkins) {
        McSkinsDialog(
            onDismiss = { showMcSkins = false },
            onPick = { bytes, slim ->
                scope.launch {
                    val model = if (slim) SkinModelType.ALEX else SkinModelType.STEVE
                    val file = withContext(Dispatchers.IO) {
                        runCatching { SkinLibrary.saveBytes(bytes, model) }.getOrNull()
                    } ?: return@launch
                    selectedLibraryFile = null
                    libraryVersion++
                    onSkinStateChange(
                        ChangeSkin.ChangeSkinData(
                            cacheFile = file,
                            data = ChangeSkin.SkinModel.Model(model)
                        )
                    )
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Row(
            modifier = Modifier
                .padding(all = 14.dp)
                .fillMaxWidth(0.82f)
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight()
                .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, opaque = true)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)
                        )
                    )
                    .drawBehind {
                        drawCircle(
                            brush = safeRadialGradient(
                                listOf(CryoGlassColors.Ice.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(size.width / 2f, size.height * 0.9f),
                                radius = size.width * 0.55f
                            ),
                            radius = size.width * 0.55f,
                            center = Offset(size.width / 2f, size.height * 0.9f)
                        )
                    }
                    .border(1.dp, cryoBorderBrush(), RoundedCornerShape(20.dp))
            ) {
                PetModelView(
                    modifier = Modifier.fillMaxSize(),
                    skin = previewSkin,
                    cape = previewCape,
                    arm = previewArm,
                    animation = if (AllSettings.cryoWardrobeFrozen.state) null else PetAnimation.Walking,
                    animationSpeed = 0.8f,
                    gazeEnabled = false
                )
                val frozen = AllSettings.cryoWardrobeFrozen.state
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (frozen) Modifier.cryoBackground(cryoGradient())
                            else Modifier.background(CryoGlassColors.Deep.copy(alpha = 0.75f))
                        )
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable { AllSettings.cryoWardrobeFrozen.save(!frozen) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(if (frozen) R.drawable.ic_play_arrow_filled else R.drawable.ic_pause_filled),
                        contentDescription = null,
                        tint = if (frozen) CryoGlassColors.Deep else CryoGlassColors.Ice
                    )
                    Text(
                        text = stringResource(if (frozen) R.string.cryo_wardrobe_animate else R.string.cryo_wardrobe_freeze),
                        style = MaterialTheme.typography.labelMedium.copy(shadow = null),
                        fontWeight = FontWeight.Black,
                        color = if (frozen) CryoGlassColors.Deep else CryoGlassColors.Ice
                    )
                }
                Text(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CryoGlassColors.Deep.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    text = stringResource(R.string.cryo_wardrobe_preview).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = CryoGlassColors.Ice
                )
            }

            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlayerFace(account = account, avatarSize = 34.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cryo_wardrobe_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                            )
                        )
                        Text(
                            text = account.username,
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .cryoGlow(18.dp, selected = true, strength = 0.7f)
                            .clip(RoundedCornerShape(50))
                            .cryoBackground(cryoGradient())
                            .clickable { showMcSkins = true }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null,
                            tint = CryoGlassColors.Deep
                        )
                        Text(
                            text = stringResource(R.string.cryo_mcskins_title),
                            style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                            fontWeight = FontWeight.Black,
                            color = CryoGlassColors.Deep,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WardrobeSection(stringResource(R.string.cryo_wardrobe_saved)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WardrobeAddTile(
                                loading = isImportingSkin,
                                onClick = {
                                    selectedLibraryFile = null
                                    skinPicker.launch(arrayOf("image/png"))
                                }
                            )
                            library.forEach { entry ->
                                WardrobeSkinTile(
                                    entry = entry,
                                    selected = selectedLibraryFile == entry.file,
                                    onClick = {
                                        scope.launch {
                                            val prepared = withContext(Dispatchers.IO) {
                                                runCatching { SkinLibrary.prepare(entry) }.getOrNull()
                                            } ?: return@launch
                                            selectedLibraryFile = entry.file
                                            onSkinStateChange(
                                                ChangeSkin.ChangeSkinData(
                                                    cacheFile = prepared,
                                                    data = ChangeSkin.SkinModel.Model(entry.model)
                                                )
                                            )
                                        }
                                    },
                                    onDelete = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { SkinLibrary.delete(entry) }
                                            if (selectedLibraryFile == entry.file) selectedLibraryFile = null
                                            libraryVersion++
                                        }
                                    }
                                )
                            }
                        }
                        if (library.isEmpty()) {
                            Text(
                                text = stringResource(R.string.cryo_wardrobe_empty),
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (skinState is ChangeSkin.ChangeSkinData) {
                        val model = skinState.data.getModelTypeNullable()
                        val enabled = skinState.data !is ChangeSkin.SkinModel.Rejected
                        WardrobeSection(stringResource(R.string.account_change_skin_arm_style)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WardrobeChip(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(R.string.account_change_skin_arm_wide),
                                    selected = model == SkinModelType.STEVE,
                                    enabled = enabled
                                ) {
                                    onSkinStateChange(skinState.copy(data = ChangeSkin.SkinModel.Model(SkinModelType.STEVE)))
                                }
                                WardrobeChip(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(R.string.account_change_skin_arm_slim),
                                    selected = model == SkinModelType.ALEX,
                                    enabled = enabled
                                ) {
                                    onSkinStateChange(skinState.copy(data = ChangeSkin.SkinModel.Model(SkinModelType.ALEX)))
                                }
                            }
                        }
                    }

                    if (account.isMicrosoftAccount() || account.isLocalAccount()) {
                        WardrobeSection(stringResource(R.string.cryo_wardrobe_more)) {
                            if (account.isLocalAccount()) {
                                WardrobeActionRow(
                                    iconRes = R.drawable.ic_styler,
                                    text = stringResource(R.string.cryo_capes_title),
                                    loading = false,
                                    onClick = { showOfflineCapes = true }
                                )
                            }
                            if (account.isMicrosoftAccount()) {
                                WardrobeActionRow(
                                    iconRes = R.drawable.ic_styler,
                                    text = if (isFetchingCapes) stringResource(R.string.account_change_cape_fetch_all)
                                    else stringResource(R.string.cryo_capes_title),
                                    loading = isFetchingCapes,
                                    onClick = { showMsCapes = true }
                                )
                            }
                            if (account.isLocalAccount() && account.hasSkinFile && skinState != ChangeSkin.ResetSkin) {
                                WardrobeActionRow(
                                    iconRes = R.drawable.ic_restart_alt,
                                    text = stringResource(R.string.generic_reset),
                                    loading = false,
                                    onClick = {
                                        selectedLibraryFile = null
                                        onSkinStateChange(ChangeSkin.ResetSkin)
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CryoTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest
                    ) {
                        Text(text = stringResource(R.string.generic_cancel))
                    }
                    CryoButton(
                        modifier = Modifier.weight(1f),
                        enabled = skinState != ChangeSkin.None || capeState != ChangeCape.None,
                        onClick = {
                            when (skinState) {
                                is ChangeSkin.ChangeSkinData -> onApplySkin(skinState.cacheFile, skinState.data.getModelType())
                                is ChangeSkin.ResetSkin -> onResetSkin()
                                ChangeSkin.None -> {}
                            }
                            if (capeState is ChangeCape.ChangeCapeData) {
                                onApplyCape(capeState.cape)
                            }
                            onDismissRequest()
                        }
                    ) {
                        Text(text = stringResource(R.string.cryo_wardrobe_apply))
                    }
                }
            }
        }
    }

    if (showMsCapes) {
        val selected = if (capeState is ChangeCape.ChangeCapeData) capeState.cape else currentUsingCape
        OfflineCapeDialog(
            account = account,
            onDismiss = { showMsCapes = false },
            onChanged = { offlineCapeVersion++ },
            owned = availableCapes.orEmpty(),
            selectedOwned = selected,
            onSelectOwned = { cape ->
                onCapeStateChange(if (cape != currentUsingCape) ChangeCape.ChangeCapeData(cape) else ChangeCape.None)
                currentCapeToLoad = cape
            }
        )
    }

    if (showCapeSelector) {
        //若当前未更改披风，则使用使用中的披风
        val cape = if (capeState is ChangeCape.ChangeCapeData) {
            capeState.cape
        } else {
            currentUsingCape
        }

        SelectCapeDialog(
            capes = buildList {
                add(EmptyCape)
                addAll(availableCapes ?: emptyList())
            },
            selectedCape = cape,
            onSelected = { cape ->
                //检查是否已经为正在使用的披风
                val state = if (cape != currentUsingCape) {
                    ChangeCape.ChangeCapeData(cape)
                } else {
                    ChangeCape.None
                }
                onCapeStateChange(state)
                currentCapeToLoad = cape
                showCapeSelector = false
            },
            onDismiss = {
                showCapeSelector = false
            }
        )
    }
}

@Composable
fun SelectCapeDialog(
    capes: List<PlayerProfile.Cape>,
    selectedCape: PlayerProfile.Cape?,
    onSelected: (PlayerProfile.Cape) -> Unit,
    onDismiss: () -> Unit,
    capeSize: Dp = 32.dp,
) {
    val density = LocalDensity.current

    val capeLocals = remember(capes) {
        buildMap {
            capes.forEach { cape ->
                cape.capeLocalRes()?.let { local ->
                    put(cape, androidText(local))
                }
            }
        }
    }

    SimpleListDialog(
        title = stringResource(R.string.account_change_cape_select_cape),
        items = capes,
        onItemSelected = { cape ->
            onSelected(cape)
        },
        current = selectedCape,
        itemLayout = { cape, isCurrent, onClick ->
            val name = capeLocals[cape] ?: androidText(cape.alias)
            CapeListItem(
                modifier = Modifier.fillMaxWidth(),
                selected = isCurrent,
                cape = cape,
                name = name,
                size = capeSize,
                density = density,
                onClick = onClick,
            )
        },
        onDismissRequest = { selected ->
            if (!selected) {
                onDismiss()
            }
        }
    )
}

@Composable
fun CapeListItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    cape: PlayerProfile.Cape,
    name: AndroidStringText,
    size: Dp,
    density: Density,
    onClick: () -> Unit,
) {
    val avatar by produceState<ImageBitmap?>(null, cape, density) {
        value = withContext(Dispatchers.IO) {
            if (cape != EmptyCape) {
                getCapeAvatar(
                    cape = cape,
                    size = size,
                    density = density
                )?.asImageBitmap()
            } else null
        }
    }

    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        val avatarBitmap = avatar
        if (avatarBitmap != null) {
            val displayWidth = with(density) { avatarBitmap.width.toDp() }
            val displayHeight = with(density) { avatarBitmap.height.toDp() }
            Image(
                modifier = Modifier
                    .width(displayWidth)
                    .height(displayHeight),
                bitmap = avatarBitmap,
                contentDescription = null
            )

            Spacer(Modifier.width(12.dp))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AndroidStringText(
                text = name,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private val avatarPaint = Paint().apply { isFilterBitmap = false }

private fun getCapeAvatar(
    cape: PlayerProfile.Cape,
    size: Dp,
    density: Density
): Bitmap? {
    val capeFile = cape.getFile(PathManager.DIR_ACCOUNT_CAPE)
    if (capeFile.exists()) {
        runCatching {
            Files.newInputStream(capeFile.toPath()).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                    ?: throw IOException("Failed to read the cape picture and try to parse it to a bitmap")
                return getCapeAvatar(bitmap, size, density)
            }
        }.onFailure { e ->
            Logger.error(TAG, "Failed to load cape avatar from locally!", e)
        }
    }
    return null
}

private fun getCapeAvatar(
    cape: Bitmap,
    size: Dp,
    density: Density
): Bitmap {
    val pixelSize = with(density) { size.roundToPx() }
    val scaleFactor = cape.width / 64.0f
    val start = scaleFactor.roundToInt()
    val capeWidth = (10 * scaleFactor).roundToInt()
    val capeHeight = (16 * scaleFactor).roundToInt()
    val targetWidth = (pixelSize.toFloat() * capeWidth / capeHeight).roundToInt()
    val avatar = createBitmap(targetWidth, pixelSize)
    val canvas = Canvas(avatar)

    canvas.drawBitmap(
        cape,
        Rect(start, start, start + capeWidth, start + capeHeight),
        RectF(0f, 0f, targetWidth.toFloat(), pixelSize.toFloat()),
        avatarPaint
    )
    return avatar
}

private fun getSkinAvatarFromAccount(
    context: Context,
    account: Account,
    size: Dp,
    density: Density
): Bitmap {
    val skin = account.getSkinFile()
    if (skin.exists()) {
        runCatching {
            Files.newInputStream(skin.toPath()).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                    ?: throw IOException("Failed to read the skin picture and try to parse it to a bitmap")
                return getSkinAvatar(bitmap, size, density)
            }
        }.onFailure { e ->
            Logger.error(TAG, "Failed to load skin avatar from locally!", e)
        }
    }
    return getDefaultAvatar(context, size, density)
}

@Throws(Exception::class)
private fun getDefaultAvatar(
    context: Context,
    size: Dp,
    density: Density
): Bitmap {
    return getSkinAvatar(
        skin = BitmapFactory.decodeStream(
            context.assets.open("skin/steve.png")
        ),
        size = size,
        density = density
    )
}

private fun getSkinAvatar(
    skin: Bitmap,
    size: Dp,
    density: Density
): Bitmap {
    val pixelSize = with(density) { size.roundToPx() }
    val faceOffset = (pixelSize / 18.0).roundToInt().toFloat()
    val scaleFactor = skin.width / 64.0f
    val faceSize = (8 * scaleFactor).roundToInt()
    val faceEndY = faceSize * 2
    val hatSrcX = (40 * scaleFactor).roundToInt()
    val avatar = createBitmap(pixelSize, pixelSize)
    val canvas = Canvas(avatar)

    val innerEnd = pixelSize - faceOffset
    canvas.drawBitmap(
        skin,
        Rect(faceSize, faceSize, faceEndY, faceEndY),
        RectF(faceOffset, faceOffset, innerEnd, innerEnd),
        avatarPaint
    )

    canvas.drawBitmap(
        skin,
        Rect(hatSrcX, faceSize, hatSrcX + faceSize, faceEndY),
        RectF(0f, 0f, pixelSize.toFloat(), pixelSize.toFloat()),
        avatarPaint
    )
    return avatar
}

@Composable
private fun WardrobeSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
            color = CryoGlassColors.Ice
        )
        content()
    }
}

@Composable
private fun WardrobeChip(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Text(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(50))
            .cryoBackground(
                if (selected) cryoGradient()
                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
            )
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.9f else 0.3f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        text = text,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        style = MaterialTheme.typography.labelLarge.copy(shadow = null),
        fontWeight = FontWeight.Bold,
        color = if (selected) CryoGlassColors.Deep else Color.White,
        maxLines = 1
    )
}

@Composable
private fun WardrobeActionRow(iconRes: Int, text: String, loading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CryoGlassColors.Ice.copy(alpha = 0.08f))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = CryoGlassColors.Ice
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 1
        )
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = null,
            tint = CryoGlassColors.Frost.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun WardrobeAddTile(loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CryoGlassColors.Ice.copy(alpha = 0.10f))
            .border(1.5.dp, cryoBorderBrush(), RoundedCornerShape(14.dp))
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = CryoGlassColors.Ice
                )
                Text(
                    text = stringResource(R.string.cryo_wardrobe_add),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WardrobeSkinTile(
    entry: SkinLibrary.Entry,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val face by produceState<ImageBitmap?>(null, entry.file) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val src = android.graphics.BitmapFactory.decodeFile(entry.file.absolutePath) ?: return@runCatching null
                val out = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(out)
                canvas.drawBitmap(src, android.graphics.Rect(8, 8, 16, 16), android.graphics.Rect(0, 0, 8, 8), null)
                if (src.height >= 64) {
                    canvas.drawBitmap(src, android.graphics.Rect(40, 8, 48, 16), android.graphics.Rect(0, 0, 8, 8), null)
                }
                out.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(modifier = Modifier.size(64.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (selected) Modifier.cryoGlow(14.dp, selected = true) else Modifier)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) cryoBorderBrush(1.5f) else Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.25f), CryoGlassColors.Ice.copy(alpha = 0.25f))),
                    RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            face?.let {
                Image(
                    modifier = Modifier.size(40.dp),
                    bitmap = it,
                    contentDescription = null,
                    filterQuality = FilterQuality.None
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                text = if (entry.model == SkinModelType.ALEX) "SLIM" else "WIDE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.sp),
                color = CryoGlassColors.Ice
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(12.dp),
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
