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

package com.russoviller.cryogenic.ui.screens.content

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.game.account.isCryoCapeAllowed
import com.russoviller.cryogenic.game.account.isSkinChangeAllowed
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import com.russoviller.cryogenic.ui.components.cryoTacticalBackdrop
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.context.COPY_LABEL_ACCOUNT_UUID
import com.russoviller.cryogenic.game.account.Account
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.game.account.auth_server.data.AuthServer
import com.russoviller.cryogenic.game.account.isAuthServerAccount
import com.russoviller.cryogenic.game.account.isMicrosoftAccount
import com.russoviller.cryogenic.game.account.isMicrosoftLogging
import com.russoviller.cryogenic.game.account.wardrobe.toArmModel
import com.russoviller.cryogenic.game.account.yggdrasil.PlayerProfile
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedDirection
import com.russoviller.cryogenic.ui.components.BackgroundCard
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.PetModelView
import com.russoviller.cryogenic.ui.components.ScalingActionButton
import com.russoviller.cryogenic.ui.components.ScalingLabel
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.SimpleEditDialog
import com.russoviller.cryogenic.ui.components.SimpleListDialog
import com.russoviller.cryogenic.ui.components.SimpleListItem
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.content.elements.AccountItem
import com.russoviller.cryogenic.ui.screens.content.elements.AccountOperation
import com.russoviller.cryogenic.ui.screens.content.elements.AccountSkinOperation
import com.russoviller.cryogenic.ui.screens.content.elements.ChangeSkinDialog
import com.russoviller.cryogenic.ui.screens.content.elements.LocalLoginDialog
import com.russoviller.cryogenic.ui.screens.content.elements.LocalLoginOperation
import com.russoviller.cryogenic.ui.screens.content.elements.LoginMenuDialog
import com.russoviller.cryogenic.ui.screens.content.elements.LoginMenuOperation
import com.russoviller.cryogenic.ui.screens.content.elements.MicrosoftLoginMethodDialog
import com.russoviller.cryogenic.ui.screens.content.elements.MicrosoftLoginOperation
import com.russoviller.cryogenic.ui.screens.content.elements.MicrosoftLoginTipDialog
import com.russoviller.cryogenic.ui.screens.content.elements.MicrosoftReloginDialog
import com.russoviller.cryogenic.ui.screens.content.elements.OtherAccountReloginDialog
import com.russoviller.cryogenic.ui.screens.content.elements.OtherLoginOperation
import com.russoviller.cryogenic.ui.screens.content.elements.OtherServerLoginDialog
import com.russoviller.cryogenic.ui.screens.content.elements.ServerOperation
import com.russoviller.cryogenic.utils.copyText
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.viewmodel.AccountManageEffect
import com.russoviller.cryogenic.viewmodel.AccountManageIntent
import com.russoviller.cryogenic.viewmodel.AccountManageViewModel
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.LocalBackgroundViewModel
import com.russoviller.cryogenic.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 封装账号界面 UI 交互的回调函数
 * 
 * @property onIntent 发送 MVI Intent 到 ViewModel
 * @property openLink 打开外部链接
 * @property backToMainScreen 返回主界面
 * @property navigateToWeb 导航到应用内浏览器界面
 * @property checkIfInWebScreen 检查当前是否在浏览器界面中（用于微软登录逻辑判断）
 * @property formatError 格式化异常为本地化字符串
 * @property submitError 提交错误到全局错误展示系统
 */
private data class AccountActions(
    val onIntent: (AccountManageIntent) -> Unit,
    val openLink: (url: String) -> Unit,
    val backToMainScreen: () -> Unit,
    val navigateToWeb: (url: String) -> Unit,
    val checkIfInWebScreen: () -> Boolean,
    val formatError: (Throwable) -> AndroidStringText,
    val submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
)

/**
 * 进入账号管理器时，可附加的打开登录菜单选项
 */
enum class FirstLoginMenu {
    /** 不打开菜单 */
    NONE,
    /** 打开微软登录菜单 */
    MICROSOFT,
    /** 打开总登录菜单 */
    NORMAL
}

/**
 * 账号管理主界面
 *
 * @param backStackViewModel 屏幕堆栈管理器
 * @param backToMainScreen 返回主屏幕的回调
 * @param openLink 外部链接跳转回调
 * @param submitError 全局错误提交回调
 */
@Composable
fun AccountManageScreen(
    key: NormalNavKey.AccountManager,
    backStackViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    openLink: (url: String) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    eventViewModel: EventViewModel
) {
    val viewModel: AccountManageViewModel = hiltViewModel { factory: AccountManageViewModel.Factory ->
        factory.create(eventViewModel)
    }

    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val profileUiState by viewModel.profileUiState.collectAsStateWithLifecycle()
    val operationUiState by viewModel.operationUiState.collectAsStateWithLifecycle()

    val actions = remember(
        viewModel,
        backToMainScreen,
        openLink,
        backStackViewModel,
        submitError
    ) {
        AccountActions(
            onIntent = viewModel::onIntent,
            openLink = openLink,
            backToMainScreen = backToMainScreen,
            navigateToWeb = { url -> backStackViewModel.mainScreen.backStack.navigateToWeb(url) },
            checkIfInWebScreen = { backStackViewModel.mainScreen.currentKey is NormalNavKey.WebScreen },
            formatError = { th -> viewModel.formatAccountError(th) },
            submitError = submitError,
        )
    }

    LaunchedEffect(Unit) {
        when (key.loginMenu) {
            FirstLoginMenu.NONE -> {}
            FirstLoginMenu.MICROSOFT -> {
                actions.onIntent(AccountManageIntent.UpdateMicrosoftLoginOp(MicrosoftLoginOperation.Tip))
            }
            FirstLoginMenu.NORMAL -> {
                actions.onIntent(AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.Login))
            }
        }

        viewModel.effect.collect { effect ->
            when (effect) {
                is AccountManageEffect.ShowError -> {
                    submitError(ErrorViewModel.ThrowableMessage(effect.title, effect.message))
                }
            }
        }
    }

    var wardrobeHandled by remember { mutableStateOf(false) }
    LaunchedEffect(key.openWardrobe, profileUiState.currentAccount) {
        val current = profileUiState.currentAccount
        if (key.openWardrobe && !wardrobeHandled && current != null) {
            wardrobeHandled = true
            viewModel.onIntent(AccountManageIntent.UpdateAccountSkinOp(AccountSkinOperation.ChangeSkin(current)))
        }
    }

    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        AccountManageContent(
            isVisible = isVisible,
            loginUiState = loginUiState,
            profileUiState = profileUiState,
            operationUiState = operationUiState,
            actions = actions
        )
    }
}

/**
 * 账号管理界面的实际内容布局
 */
@Composable
private fun AccountManageContent(
    isVisible: Boolean,
    loginUiState: AccountManageViewModel.LoginUiState,
    profileUiState: AccountManageViewModel.ProfileUiState,
    operationUiState: AccountManageViewModel.OperationUiState,
    actions: AccountActions,
) {
    com.russoviller.cryogenic.ui.screens.content.elements.AccountPanelHost(
        panel = { drag, panelModifier ->
            ActionsLayout(
                isVisible = isVisible,
                modifier = panelModifier,
                currentAccount = profileUiState.currentAccount,
                dragging = drag.dragging,
                actions = actions
            )
        },
        content = { contentModifier ->
            AccountsLayout(
                isVisible = isVisible,
                modifier = contentModifier,
                accounts = profileUiState.accounts,
                currentAccount = profileUiState.currentAccount,
                accountOperation = operationUiState.accountOp,
                accountSkinOperation = operationUiState.accountSkinOp,
                accountSkinDialogState = operationUiState.accountSkinDialogState,
                accountCapes = profileUiState.accountCapeOpMap,
                actions = actions
            )
        }
    )

    LoginMenuOperation(loginUiState.menuOp, actions, profileUiState.authServers)
    MicrosoftLoginOperation(loginUiState.microsoftOp, actions)
    LocalLoginOperation(loginUiState.localOp, actions)
    OtherLoginOperation(loginUiState.otherOp, actions)
    ServerTypeOperation(operationUiState.serverOp, actions)
}

/**
 * 左侧登录方式菜单组件
 */
@Composable
private fun ActionsLayout(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    currentAccount: Account?,
    dragging: Boolean,
    actions: AccountActions
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)

    val density = androidx.compose.ui.platform.LocalDensity.current
    val panelShape = remember(density) {
        with(density) { com.russoviller.cryogenic.ui.components.cryoTacticalPanelShape(true, 22.dp.toPx(), 12.dp.toPx()) }
    }
    val fill = 0.55f + 0.45f * com.russoviller.cryogenic.ui.components.cryoFillFactor().coerceAtMost(1f)
    val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
    val armModel = currentAccount?.skinModelType?.toArmModel()
    val skinStamp = currentAccount?.getSkinFile()?.lastModified()
    val capeStamp = currentAccount?.getDisplayCapeFile()?.let { if (it.exists()) it.lastModified() else 0L }
    val skinBytes by produceState<ByteArray?>(
        null, currentAccount, refreshWardrobe, skinStamp, armModel
    ) {
        value = withContext(Dispatchers.IO) {
            currentAccount?.getSkinFile()?.takeIf { it.exists() }
                ?.inputStream()?.use { it.readBytes() }
        }
    }
    val capeBytes by produceState<ByteArray?>(null, currentAccount, refreshWardrobe, capeStamp) {
        value = withContext(Dispatchers.IO) {
            currentAccount?.getDisplayCapeFile()?.takeIf { it.exists() }
                ?.inputStream()?.use { it.readBytes() }
        }
    }
    Box(
        modifier = modifier.animated(ctx, from = AnimatedDirection.Left)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(panelShape)
                .cryoTacticalBackdrop(panelShape, true, fill)
                .padding(start = 12.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.russoviller.cryogenic.ui.screens.content.elements.OperatorHeader(
                name = currentAccount?.username ?: stringResource(R.string.account_no_account),
                dragging = dragging
            )
            com.russoviller.cryogenic.ui.screens.content.elements.OperatorSkinStage(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                skin = skinBytes,
                cape = capeBytes,
                arm = armModel,
                dragging = dragging
            )
            com.russoviller.cryogenic.ui.screens.content.elements.OperatorActionButton(
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_add,
                text = stringResource(R.string.account_add_new_account),
                primary = true,
                onClick = {
                    actions.onIntent(AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.Login))
                }
            )
            currentAccount?.takeIf { it.isCryoCapeAllowed() }?.let { account ->
                com.russoviller.cryogenic.ui.screens.content.elements.OperatorActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.ic_checkroom,
                    text = stringResource(R.string.account_wardrobe),
                    primary = false,
                    onClick = {
                        actions.onIntent(AccountManageIntent.UpdateAccountSkinOp(AccountSkinOperation.ChangeSkin(account)))
                    }
                )
            }
        }
    }
}

@Composable
private fun LoginMenuOperation(
    operation: LoginMenuOperation,
    actions: AccountActions,
    authServers: List<AuthServer>
) {
    when (operation) {
        LoginMenuOperation.None -> {}
        LoginMenuOperation.Login -> {
            LoginMenuDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.None)
                    )
                },
                authServers = authServers,
                onMicrosoftLogin = {
                    if (!isMicrosoftLogging()) {
                        actions.onIntent(
                            AccountManageIntent.UpdateMicrosoftLoginOp(
                                MicrosoftLoginOperation.Tip
                            )
                        )
                    }
                },
                onLocalLogin = {
                    actions.onIntent(AccountManageIntent.UpdateLocalLoginOp(LocalLoginOperation.Edit))
                },
                onAuthServerLogin = { server ->
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.OnLogin(server)
                        )
                    )
                },
                onAddAuthServer = {
                    actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.AddNew))
                },
                onElyByLogin = {
                    actions.onIntent(AccountManageIntent.LoginElyBy)
                },
                onDeleteAuthServer = { server ->
                    actions.onIntent(
                        AccountManageIntent.UpdateServerOp(
                            ServerOperation.Delete(server)
                        )
                    )
                }
            )
        }
    }
}

/**
 * 微软登录相关逻辑处理
 */
@Composable
private fun MicrosoftLoginOperation(
    operation: MicrosoftLoginOperation,
    actions: AccountActions
) {
    when (operation) {
        is MicrosoftLoginOperation.None -> {}
        is MicrosoftLoginOperation.Tip -> {
            MicrosoftLoginTipDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateMicrosoftLoginOp(
                            MicrosoftLoginOperation.None
                        )
                    )
                },
                onConfirm = {
                    actions.onIntent(
                        AccountManageIntent.UpdateMicrosoftLoginOp(
                            MicrosoftLoginOperation.SelectBrowser
                        )
                    )
                },
                openLink = actions.openLink
            )
        }
        is MicrosoftLoginOperation.SelectBrowser -> {
            MicrosoftLoginMethodDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateMicrosoftLoginOp(
                            MicrosoftLoginOperation.None
                        )
                    )
                },
                onSystemBrowser = {
                    actions.onIntent(
                        AccountManageIntent.PerformMicrosoftLogin(
                            useSystemBrowser = true,
                            toWeb = actions.navigateToWeb,
                            toSystemBrowser = actions.openLink,
                            backToMain = actions.backToMainScreen,
                            checkIfInWebScreen = actions.checkIfInWebScreen
                        )
                    )
                },
                onInternalBrowser = {
                    actions.onIntent(
                        AccountManageIntent.PerformMicrosoftLogin(
                            useSystemBrowser = false,
                            toWeb = actions.navigateToWeb,
                            toSystemBrowser = actions.openLink,
                            backToMain = actions.backToMainScreen,
                            checkIfInWebScreen = actions.checkIfInWebScreen
                        )
                    )
                }
            )
        }
    }
}

/**
 * 离线账号登录相关逻辑处理
 */
@Composable
private fun LocalLoginOperation(
    operation: LocalLoginOperation,
    actions: AccountActions
) {
    when (operation) {
        is LocalLoginOperation.None -> {}
        is LocalLoginOperation.Edit -> {
            LocalLoginDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.None
                        )
                    )
                },
                onConfirm = { isInvalid, name, uuid ->
                    val nextOp = if (isInvalid) LocalLoginOperation.Alert(
                        name,
                        uuid
                    ) else LocalLoginOperation.Create(name, uuid)
                    actions.onIntent(AccountManageIntent.UpdateLocalLoginOp(nextOp))
                },
                openLink = actions.openLink
            )
        }

        is LocalLoginOperation.Create -> {
            LaunchedEffect(operation) {
                actions.onIntent(
                    AccountManageIntent.CreateLocalAccount(
                        operation.userName,
                        operation.userUUID
                    )
                )
            }
        }

        is LocalLoginOperation.Alert -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_supporting_username_invalid_title),
                text = {
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint1))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_supporting_username_invalid_local_message_hint2),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint3))
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint4))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_supporting_username_invalid_local_message_hint5),
                        fontWeight = FontWeight.Bold
                    )
                },
                confirmText = stringResource(R.string.account_supporting_username_invalid_still_use),
                onConfirm = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.Create(operation.userName, operation.userUUID)
                        )
                    )
                },
                onCancel = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.None
                        )
                    )
                }
            )
        }
    }
}

/**
 * 第三方验证服务器登录逻辑处理
 */
@Composable
private fun OtherLoginOperation(
    operation: OtherLoginOperation,
    actions: AccountActions
) {
    when (operation) {
        is OtherLoginOperation.None -> {}
        is OtherLoginOperation.OnLogin -> {
            OtherServerLoginDialog(
                server = operation.server,
                onRegisterClick = { url ->
                    actions.openLink(url)
                    actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
                },
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.None
                        )
                    )
                },
                onConfirm = { email, password ->
                    actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
                    actions.onIntent(
                        AccountManageIntent.LoginWithOtherServer(
                            operation.server,
                            email,
                            password
                        )
                    )
                }
            )
        }

        is OtherLoginOperation.OnFailed -> {
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.account_logging_in_failed),
                        message = actions.formatError(operation.th)
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
            }
        }

        is OtherLoginOperation.SelectRole -> {
            SimpleListDialog(
                title = stringResource(R.string.account_other_login_select_role),
                items = operation.profiles,
                onItemSelected = { operation.selected(it) },
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.None
                        )
                    )
                },
                itemLayout = { item, isCurrent, onClick ->
                    SimpleListItem(
                        selected = isCurrent,
                        itemName = item.name,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClick
                    )
                }
            )
        }
    }
}

/**
 * 验证服务器管理操作逻辑处理
 */
@Composable
private fun ServerTypeOperation(
    operation: ServerOperation,
    actions: AccountActions
) {
    when (operation) {
        is ServerOperation.AddNew -> {
            var serverUrl by rememberSaveable { mutableStateOf("") }
            SimpleEditDialog(
                title = stringResource(R.string.account_add_new_server),
                value = serverUrl,
                onValueChange = { serverUrl = it.trim() },
                label = { Text(text = stringResource(R.string.account_label_server_url)) },
                singleLine = true,
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateServerOp(
                            ServerOperation.None
                        )
                    )
                },
                onConfirm = {
                    if (serverUrl.isNotEmpty()) {
                        actions.onIntent(AccountManageIntent.AddServer(serverUrl))
                    }
                }
            )
        }

        is ServerOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_other_login_delete_server_title),
                text = stringResource(
                    R.string.account_other_login_delete_server_message,
                    operation.server.serverName
                ),
                onDismiss = { actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.None)) },
                onConfirm = { actions.onIntent(AccountManageIntent.DeleteServer(operation.server)) }
            )
        }

        is ServerOperation.OnThrowable -> {
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.account_other_login_adding_failure),
                        message = androidText(operation.throwable.getMessageOrToString())
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.None))
            }
        }

        is ServerOperation.None -> {}
    }
}

/**
 * 账号列表组件
 */
@Composable
private fun AccountsLayout(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    accounts: List<Account>,
    currentAccount: Account?,
    accountOperation: AccountOperation,
    accountSkinOperation: AccountSkinOperation,
    accountSkinDialogState: AccountManageViewModel.AccountSkinDialogState,
    accountCapes: Map<String, List<PlayerProfile.Cape>>,
    actions: AccountActions
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    val context = LocalContext.current

    AccountOperation(accountOperation, actions)

    AccountSkinOperation(
        accountSkinOperation = accountSkinOperation,
        skinDialogState = accountSkinDialogState,
        accountCapes = accountCapes,
        actions = actions
    )

    Column(
        modifier = modifier.animated(ctx, from = AnimatedDirection.Right),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .cryoGlass(shape = androidx.compose.foundation.shape.RoundedCornerShape(50), glow = false, cornerRadius = 24.dp)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .cryoBackground(com.russoviller.cryogenic.ui.components.cryoGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_person_outlined),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cryo_account_list_title),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 1
            )
            Text(
                modifier = Modifier.padding(end = 10.dp),
                text = "%02d".format(accounts.size),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = CryoGlassColors.Ice
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (accounts.isNotEmpty()) {
            val scrollState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nonInteractiveScrollbar(
                        state = scrollState.scrollIndicatorState!!,
                        orientation = Orientation.Vertical,
                    )
                    .clip(MaterialTheme.shapes.extraLarge),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                state = scrollState,
            ) {
                items(accounts, key = { it.uniqueUUID }) { account ->
                    AccountItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        currentAccount = currentAccount,
                        account = account,
                        onSelected = { AccountsManager.setCurrentAccount(it) },
                        openChangeSkinDialog = {
                            if (account.isCryoCapeAllowed()) {
                                actions.onIntent(
                                    AccountManageIntent.UpdateAccountSkinOp(
                                        AccountSkinOperation.ChangeSkin(account)
                                    )
                                )
                            }
                        },
                        onRefreshClick = {
                            actions.onIntent(
                                AccountManageIntent.RefreshAccount(
                                    account
                                )
                            )
                        },
                        onCopyUUID = {
                            copyText(COPY_LABEL_ACCOUNT_UUID, account.profileId, context, true)
                        },
                        onDeleteClick = {
                            actions.onIntent(
                                AccountManageIntent.UpdateAccountOp(
                                    AccountOperation.Delete(account)
                                )
                            )
                        }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                ScalingLabel(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.account_no_account)
                )
            }
        }
        }
    }
}

/**
 * 账号皮肤操作逻辑处理
 */
@Composable
private fun AccountSkinOperation(
    accountSkinOperation: AccountSkinOperation,
    skinDialogState: AccountManageViewModel.AccountSkinDialogState,
    accountCapes: Map<String, List<PlayerProfile.Cape>>,
    actions: AccountActions
) {
    when (accountSkinOperation) {
        is AccountSkinOperation.None -> {}
        is AccountSkinOperation.ChangeSkin -> {
            val account = accountSkinOperation.account
            if (account.isAuthServerAccount()) {
                if (account.isCryoCapeAllowed()) {
                    com.russoviller.cryogenic.ui.screens.content.elements.OfflineCapeDialog(
                        account = account,
                        onDismiss = {
                            actions.onIntent(AccountManageIntent.ResetAccountSkinDialogState)
                            actions.onIntent(AccountManageIntent.UpdateAccountSkinOp(AccountSkinOperation.None))
                        },
                        onChanged = {}
                    )
                } else {
                    LaunchedEffect(account) {
                        actions.onIntent(AccountManageIntent.UpdateAccountSkinOp(AccountSkinOperation.None))
                    }
                }
                return
            }
            ChangeSkinDialog(
                account = account,
                availableCapes = accountCapes[account.uniqueUUID],
                skinState = skinDialogState.pendingSkinData,
                onSkinStateChange = { skinState ->
                    actions.onIntent(
                        AccountManageIntent.UpdatePendingSkinData(
                            skinState
                        )
                    )
                },
                capeState = skinDialogState.pendingCapeData,
                onCapeStateChange = { capeState ->
                    actions.onIntent(
                        AccountManageIntent.UpdatePendingCapeData(
                            capeState
                        )
                    )
                },
                isImportingSkin = skinDialogState.importingSkin,
                onSkinPicked = { uri ->
                    actions.onIntent(
                        AccountManageIntent.OnSkinPicked(uri)
                    )
                },
                onDismissRequest = {
                    actions.onIntent(AccountManageIntent.ResetAccountSkinDialogState)
                    actions.onIntent(AccountManageIntent.UpdateAccountSkinOp(AccountSkinOperation.None))
                },
                onResetSkin = {
                    actions.onIntent(AccountManageIntent.ResetSkin(account))
                },
                onFetchCapes = {
                    actions.onIntent(AccountManageIntent.FetchMicrosoftCapes(account))
                },
                onApplySkin = { file, model ->
                    actions.onIntent(AccountManageIntent.ApplySkin(account, file, model))
                },
                onApplyCape = { cape ->
                    actions.onIntent(AccountManageIntent.ApplyMicrosoftCape(account, cape))
                }
            )
        }
    }
}

/**
 * 通用账号管理操作逻辑处理（如删除确认）
 */
@Composable
private fun AccountOperation(
    operation: AccountOperation,
    actions: AccountActions
) {
    when (operation) {
        is AccountOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_delete_title),
                text = stringResource(R.string.account_delete_message, operation.account.username),
                onConfirm = { actions.onIntent(AccountManageIntent.DeleteAccount(operation.account)) },
                onDismiss = { actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None)) }
            )
        }

        is AccountOperation.OnFailed -> {
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.account_logging_in_failed),
                        message = actions.formatError(operation.th)
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
            }
        }

        is AccountOperation.OnRelogin -> {
            if (operation.account.isMicrosoftAccount()) {
                MicrosoftReloginDialog(
                    onDismissRequest = {
                        actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
                    },
                    onConfirm = {
                        actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
                        actions.onIntent(
                            AccountManageIntent.UpdateMicrosoftLoginOp(
                                MicrosoftLoginOperation.SelectBrowser
                            )
                        )
                    }
                )
            } else {
                OtherAccountReloginDialog(
                    account = operation.account,
                    logging = operation.logging,
                    error = operation.error,
                    onDismissRequest = {
                        actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
                    },
                    onConfirm = { password ->
                        actions.onIntent(
                            AccountManageIntent.UpdateAccountOp(
                                AccountOperation.OnRelogin(operation.account, logging = true)
                            )
                        )
                        actions.onIntent(
                            AccountManageIntent.ReloginOtherAccount(
                                account = operation.account,
                                password = password
                            )
                        )
                    }
                )
            }
        }

        is AccountOperation.None -> {}
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480)
@Composable
private fun AccountManageContentPreview() {
    CompositionLocalProvider(LocalBackgroundViewModel provides null) {
        MaterialExpressiveTheme {
            Surface {
                AccountManageContent(
                    isVisible = true,
                    loginUiState = AccountManageViewModel.LoginUiState(),
                    profileUiState = AccountManageViewModel.ProfileUiState(),
                    operationUiState = AccountManageViewModel.OperationUiState(),
                    actions = AccountActions(
                        onIntent = {},
                        openLink = {},
                        backToMainScreen = {},
                        navigateToWeb = {},
                        checkIfInWebScreen = { false },
                        formatError = { AndroidStringText.Text("") },
                        submitError = {},
                    )
                )
            }
        }
    }
}
