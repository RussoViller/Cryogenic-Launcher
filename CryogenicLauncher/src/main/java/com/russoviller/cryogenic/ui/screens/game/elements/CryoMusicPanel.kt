package com.russoviller.cryogenic.ui.screens.game.elements

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import com.russoviller.cryogenic.ui.components.CryoDropdownMenu as DropdownMenu
import com.russoviller.cryogenic.ui.components.CryoDropdownMenuItem as DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.music.CryoMusicLibrary
import com.russoviller.cryogenic.music.CryoNowPlaying
import com.russoviller.cryogenic.music.CryoPlayer
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.CryoChipButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSegmentedTabs
import com.russoviller.cryogenic.ui.components.CryoToggleTile
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoTile
import com.russoviller.cryogenic.ui.components.lazyScrollWithBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

fun formatMusicTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private val Accent = CryoGlassColors.Ice
private val Danger = Color(0xFFFF8A80)

/** Onde o lado direito do painel está. */
private sealed interface MusicView {
    data object Library : MusicView
    data class PlaylistDetail(val dir: File) : MusicView
    /** Importar para [target] (pasta principal ou playlist); [back] = para onde voltar. */
    data class Import(val target: File, val targetName: String, val back: MusicView) : MusicView
}

/** Estado do painel (continua igual ao fechar e abrir o menu). */
private object MusicUiState {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var view by mutableStateOf<MusicView>(MusicView.Library)
    var tab by mutableIntStateOf(0)

    /** Pergunta de confirmação no rodapé (texto, botão, ação). */
    var confirm by mutableStateOf<Triple<String, String, () -> Unit>?>(null)

    /** Busca da aba Músicas e da playlist aberta. */
    var songFilter by mutableStateOf("")
    var playlistFilter by mutableStateOf("")

    //importar
    var device by mutableStateOf<List<CryoMusicLibrary.DeviceSong>?>(null)
    var deviceLoading by mutableStateOf(false)
    /** Escolhidas para importar (de "Todas as músicas" e de qualquer pasta). */
    var selected by mutableStateOf<Map<String, CryoMusicLibrary.DeviceSong>>(emptyMap())
    var filter by mutableStateOf("")
    var importing by mutableStateOf<Pair<Int, Int>?>(null)
    /** 0 = Todas as músicas, 1 = Pastas. */
    var importMode by mutableIntStateOf(0)
    var folders by mutableStateOf<List<CryoMusicLibrary.DeviceFolder>?>(null)
    var foldersLoading by mutableStateOf(false)
    var openFolder by mutableStateOf<CryoMusicLibrary.DeviceFolder?>(null)
    var folderSongs by mutableStateOf<List<CryoMusicLibrary.DeviceSong>?>(null)
}

/** Painel "Música" do menu do jogo: player + gerenciador da pasta Cryogenic Music. */
@Composable
fun CryoMusicPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CryoMusicLibrary.refresh(context) }
    Row(
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NowPlayingColumn(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            if (!CryoMusicLibrary.canAccess(context)) {
                MessageBox(
                    text = stringResource(R.string.cryo_music_storage_permission),
                    button = stringResource(R.string.cryo_music_permission_button),
                    onClick = { openStorageSettings(context) }
                )
            } else {
                when (val view = MusicUiState.view) {
                    MusicView.Library -> LibraryView()
                    is MusicView.PlaylistDetail -> PlaylistView(view.dir)
                    is MusicView.Import -> ImportView(view)
                }
            }
            MusicUiState.confirm?.let { (text, button, action) ->
                ConfirmBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    text = text,
                    button = button,
                    onCancel = { MusicUiState.confirm = null },
                    onConfirm = {
                        MusicUiState.confirm = null
                        action()
                    }
                )
            }
        }
    }
}

private fun openStorageSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Músicas que batem com a busca (nome ou artista; ignora maiúsculas e acentos). */
private fun filterSongs(songs: List<CryoPlayer.Song>, query: String): List<CryoPlayer.Song> {
    val q = normalize(query.trim())
    if (q.isEmpty()) return songs
    val words = q.split(' ').filter { it.isNotEmpty() }
    return songs.filter { song ->
        val text = normalize(song.title + " " + song.artist)
        words.all { text.contains(it) }
    }
}

private fun normalize(text: String): String =
    java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

@Composable
private fun SongSearch(value: String, onValueChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    SearchField(
        value = value,
        onValueChange = { onValueChange(it.take(100)) },
        hint = stringResource(R.string.cryo_music_search_library_hint),
        onSearch = { focus.clearFocus() }
    )
}

@Composable
private fun NoResults() {
    MessageBox(text = stringResource(R.string.cryo_music_no_results))
}

// ------------------------------------------------------------------ biblioteca (Músicas / Playlists)

@Composable
private fun LibraryView() {
    val context = LocalContext.current
    val library = CryoMusicLibrary.library
    val songs = library?.songs.orEmpty()
    val playlists = library?.playlists.orEmpty()
    val songsName = stringResource(R.string.cryo_music_tab_songs_plain)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CryoSegmentedTabs(
                modifier = Modifier.weight(1f),
                titles = listOf(
                    stringResource(R.string.cryo_music_tab_songs, songs.size),
                    stringResource(R.string.cryo_music_tab_playlists, playlists.size)
                ),
                selected = MusicUiState.tab,
                onSelect = { MusicUiState.tab = it }
            )
            SmallIconButton(R.drawable.ic_refresh, loading = CryoMusicLibrary.scanning) { CryoMusicLibrary.refresh(context) }
        }
        if (library == null) {
            Loading()
        } else if (MusicUiState.tab == 0) {
            SongsHeader(
                songs = songs,
                onImport = { MusicUiState.view = MusicView.Import(CryoMusicLibrary.root(), songsName, MusicView.Library) }
            )
            if (songs.isEmpty()) {
                MessageBox(text = stringResource(R.string.cryo_music_empty_main, CryoMusicLibrary.FOLDER_NAME))
            } else {
                SongSearch(MusicUiState.songFilter) { MusicUiState.songFilter = it }
                val query = MusicUiState.songFilter
                val shown = remember(songs, query) { filterSongs(songs, query) }
                if (shown.isEmpty()) NoResults() else SongList(songs = shown, inPlaylist = null, playlists = playlists)
            }
        } else {
            PlaylistsList(playlists)
        }
    }
}

@Composable
private fun SongsHeader(songs: List<CryoPlayer.Song>, onImport: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PrimaryChip(text = stringResource(R.string.cryo_music_import), iconRes = R.drawable.ic_download, onClick = onImport)
        CryoChipButton(
            text = stringResource(R.string.cryo_music_play_all),
            iconRes = R.drawable.ic_play_arrow_filled,
            enabled = songs.isNotEmpty(),
            onClick = {
                if (CryoPlayer.shuffle) CryoPlayer.toggleShuffle()
                CryoPlayer.playQueue(context, songs, 0)
            }
        )
        CryoChipButton(
            text = stringResource(R.string.cryo_music_shuffle_all),
            iconRes = R.drawable.ic_cryo_shuffle,
            enabled = songs.isNotEmpty(),
            onClick = {
                if (!CryoPlayer.shuffle) CryoPlayer.toggleShuffle()
                CryoPlayer.playQueue(context, songs, songs.indices.random())
            }
        )
    }
}

@Composable
private fun PlaylistsList(playlists: List<CryoMusicLibrary.Playlist>) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var creating by remember { mutableStateOf(false) }
    val exists = stringResource(R.string.cryo_music_playlist_exists)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item(key = "new") {
            if (creating) {
                NameEditor(
                    initial = "",
                    hint = stringResource(R.string.cryo_music_playlist_name),
                    confirm = stringResource(R.string.cryo_music_create),
                    onCancel = { creating = false },
                    onConfirm = { name ->
                        MusicUiState.scope.launch {
                            if (CryoMusicLibrary.createPlaylist(name)) creating = false
                            else Toast.makeText(context, exists, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable { creating = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(modifier = Modifier.size(20.dp), painter = painterResource(R.drawable.ic_add), contentDescription = null, tint = Accent)
                    Text(stringResource(R.string.cryo_music_new_playlist), color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (playlists.isEmpty()) {
            item(key = "empty") { MessageBox(text = stringResource(R.string.cryo_music_playlists_empty, CryoMusicLibrary.FOLDER_NAME)) }
        }
        items(playlists, key = { it.dir.absolutePath }) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { MusicUiState.view = MusicView.PlaylistDetail(playlist.dir) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.5f), CryoGlassColors.Deep))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(modifier = Modifier.size(22.dp), painter = painterResource(R.drawable.ic_cryo_library_music), contentDescription = null, tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.cryo_music_local_count, playlist.songs.size), color = CryoGlassColors.Frost.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                SmallIconButton(R.drawable.ic_play_arrow_filled, enabled = playlist.songs.isNotEmpty()) {
                    CryoPlayer.playQueue(context, playlist.songs, 0)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ playlist aberta

@Composable
private fun PlaylistView(dir: File) {
    val context = LocalContext.current
    val library = CryoMusicLibrary.library
    val playlist = library?.playlists?.firstOrNull { it.dir.absolutePath == dir.absolutePath }
    if (library != null && playlist == null) {
        //apagada ou renomeada por fora
        LaunchedEffect(Unit) { MusicUiState.view = MusicView.Library }
        return
    }
    if (playlist == null) {
        Loading()
        return
    }
    var renaming by remember(dir) { mutableStateOf(false) }
    LaunchedEffect(dir) { MusicUiState.playlistFilter = "" }
    val exists = stringResource(R.string.cryo_music_playlist_exists)
    val deleteText = stringResource(R.string.cryo_music_delete_playlist_confirm, playlist.name)
    val deleteButton = stringResource(R.string.cryo_music_delete)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallIconButton(R.drawable.ic_arrow_back) { MusicUiState.view = MusicView.Library }
            if (renaming) {
                Box(modifier = Modifier.weight(1f)) {
                    NameEditor(
                        initial = playlist.name,
                        hint = stringResource(R.string.cryo_music_playlist_name),
                        confirm = stringResource(R.string.cryo_music_save),
                        onCancel = { renaming = false },
                        onConfirm = { name ->
                            MusicUiState.scope.launch {
                                val newName = name.trim()
                                if (newName == playlist.name) {
                                    renaming = false
                                } else {
                                    val dest = CryoMusicLibrary.renamePlaylist(playlist, newName)
                                    if (dest != null) {
                                        renaming = false
                                        MusicUiState.view = MusicView.PlaylistDetail(dest)
                                    } else {
                                        Toast.makeText(context, exists, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.cryo_music_local_count, playlist.songs.size), color = CryoGlassColors.Frost.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                SmallIconButton(R.drawable.ic_edit_outlined) { renaming = true }
                SmallIconButton(R.drawable.ic_delete_outlined, tint = Danger) {
                    MusicUiState.confirm = Triple(deleteText, deleteButton) {
                        MusicUiState.scope.launch { CryoMusicLibrary.deletePlaylist(playlist) }
                        MusicUiState.view = MusicView.Library
                    }
                }
            }
        }
        SongsHeader(
            songs = playlist.songs,
            onImport = { MusicUiState.view = MusicView.Import(playlist.dir, playlist.name, MusicView.PlaylistDetail(playlist.dir)) }
        )
        if (playlist.songs.isEmpty()) {
            MessageBox(text = stringResource(R.string.cryo_music_playlist_songs_empty))
        } else {
            SongSearch(MusicUiState.playlistFilter) { MusicUiState.playlistFilter = it }
            val query = MusicUiState.playlistFilter
            val shown = remember(playlist.songs, query) { filterSongs(playlist.songs, query) }
            if (shown.isEmpty()) NoResults() else SongList(songs = shown, inPlaylist = playlist, playlists = library.playlists)
        }
    }
}

// ------------------------------------------------------------------ lista de músicas

@Composable
private fun SongList(songs: List<CryoPlayer.Song>, inPlaylist: CryoMusicLibrary.Playlist?, playlists: List<CryoMusicLibrary.Playlist>) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val currentId = CryoPlayer.current?.id
    val playing = CryoPlayer.playing
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
            SongRow(
                song = song,
                current = song.id == currentId,
                playing = playing,
                inPlaylist = inPlaylist,
                playlists = playlists,
                onClick = { CryoPlayer.playQueue(context, songs, index) }
            )
        }
    }
}

@Composable
private fun SongRow(
    song: CryoPlayer.Song,
    current: Boolean,
    playing: Boolean,
    inPlaylist: CryoMusicLibrary.Playlist?,
    playlists: List<CryoMusicLibrary.Playlist>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val moved = stringResource(R.string.cryo_music_moved)
    val deleteText = stringResource(R.string.cryo_music_delete_song_confirm, song.title)
    val deleteButton = stringResource(R.string.cryo_music_delete)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (current) Accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.035f))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, top = 5.dp, bottom = 5.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (current) Accent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(if (current && playing) R.drawable.ic_cryo_equalizer else R.drawable.ic_music_note),
                contentDescription = null,
                tint = if (current) Accent else Color.White.copy(alpha = 0.6f)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, shadow = null),
                color = if (current) Accent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = listOfNotNull(song.artist.ifBlank { null }, song.durationMs.takeIf { it > 0 }?.let { formatMusicTime(it) }).joinToString("  ·  ")
            if (sub.isNotEmpty()) {
                Text(text = sub, style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box {
            SmallIconButton(R.drawable.ic_more_vert) { menu = true }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                containerColor = CryoGlassColors.Navy,
                shape = RoundedCornerShape(14.dp)
            ) {
                val targets = playlists.filter { it.dir.absolutePath != inPlaylist?.dir?.absolutePath }
                MenuLabel(stringResource(if (inPlaylist == null) R.string.cryo_music_send_to_playlist else R.string.cryo_music_move_to_playlist))
                if (targets.isEmpty()) {
                    MenuItem(text = stringResource(R.string.cryo_music_create_playlist_first), iconRes = R.drawable.ic_add) {
                        menu = false
                        MusicUiState.tab = 1
                        MusicUiState.view = MusicView.Library
                    }
                }
                targets.forEach { target ->
                    MenuItem(text = target.name, iconRes = R.drawable.ic_cryo_library_music) {
                        menu = false
                        MusicUiState.scope.launch {
                            if (CryoMusicLibrary.move(song, target.dir)) Toast.makeText(context, "$moved ${target.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                if (inPlaylist != null) {
                    MenuItem(text = stringResource(R.string.cryo_music_remove_from_playlist), iconRes = R.drawable.ic_close) {
                        menu = false
                        MusicUiState.scope.launch { CryoMusicLibrary.move(song, CryoMusicLibrary.root()) }
                    }
                }
                MenuItem(text = deleteButton, iconRes = R.drawable.ic_delete_outlined, tint = Danger) {
                    menu = false
                    MusicUiState.confirm = Triple(deleteText, deleteButton) {
                        MusicUiState.scope.launch { CryoMusicLibrary.delete(song) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuLabel(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
        color = Accent
    )
}

@Composable
private fun MenuItem(text: String, iconRes: Int, tint: Color = Color.White, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(modifier = Modifier.size(18.dp), painter = painterResource(iconRes), contentDescription = null, tint = tint) },
        onClick = onClick
    )
}

// ------------------------------------------------------------------ importar

@Composable
private fun ImportView(view: MusicView.Import) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val state = MusicUiState
    LaunchedEffect(view.target) {
        state.selected = emptyMap()
        state.filter = ""
        state.openFolder = null
        state.folderSongs = null
        //relê ao abrir (músicas baixadas depois aparecem)
        if (!state.deviceLoading) state.device = null
        if (!state.foldersLoading) state.folders = null
    }
    //cada lista é lida só quando a pessoa abre o modo dela
    LaunchedEffect(state.importMode, view.target) {
        if (state.importMode == 0 && state.device == null && !state.deviceLoading) {
            state.deviceLoading = true
            state.device = CryoMusicLibrary.deviceSongs(context)
            state.deviceLoading = false
        }
        if (state.importMode == 1 && state.folders == null && !state.foldersLoading) {
            state.foldersLoading = true
            state.folders = CryoMusicLibrary.deviceFolders()
            state.foldersLoading = false
        }
    }
    val openFolder = state.openFolder
    LaunchedEffect(openFolder) {
        state.folderSongs = null
        if (openFolder != null) state.folderSongs = CryoMusicLibrary.folderSongs(openFolder.dir)
    }
    val importing = state.importing
    val doneText = stringResource(R.string.cryo_music_import_done)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallIconButton(R.drawable.ic_arrow_back, enabled = importing == null) {
                if (state.importMode == 1 && state.openFolder != null) {
                    state.openFolder = null
                    state.filter = ""
                } else {
                    state.view = view.back
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.importMode == 1 && openFolder != null) openFolder.name else stringResource(R.string.cryo_music_import),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.cryo_music_import_to, view.targetName),
                    color = Accent,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PrimaryChip(
                text = if (importing != null) stringResource(R.string.cryo_music_importing, importing.first + 1, importing.second)
                else stringResource(R.string.cryo_music_import_count, state.selected.size),
                iconRes = R.drawable.ic_download,
                enabled = importing == null && state.selected.isNotEmpty(),
                onClick = {
                    focus.clearFocus()
                    val chosen = state.selected.values.toList()
                    state.importing = 0 to chosen.size
                    state.scope.launch {
                        val result = CryoMusicLibrary.import(context, chosen, view.target) { done, total ->
                            state.importing = done.coerceAtMost(total - 1).coerceAtLeast(0) to total
                        }
                        state.importing = null
                        Toast.makeText(context, String.format(doneText, result.copied, result.skipped + result.failed), Toast.LENGTH_SHORT).show()
                        state.view = view.back
                    }
                }
            )
        }
        CryoSegmentedTabs(
            titles = listOf(stringResource(R.string.cryo_music_import_all), stringResource(R.string.cryo_music_import_folders)),
            selected = state.importMode,
            onSelect = {
                focus.clearFocus()
                state.importMode = it
                state.filter = ""
            }
        )
        SearchField(
            value = state.filter,
            onValueChange = { state.filter = it.take(100) },
            hint = stringResource(
                if (state.importMode == 1 && openFolder == null) R.string.cryo_music_folder_filter_hint else R.string.cryo_music_filter_hint
            ),
            onSearch = { focus.clearFocus() }
        )
        val query = state.filter.trim()
        if (state.importMode == 1 && openFolder == null) {
            FolderList(query)
        } else {
            val source = if (state.importMode == 0) state.device else state.folderSongs
            val loading = if (state.importMode == 0) state.deviceLoading || source == null else source == null
            val all = source.orEmpty()
            val shown = remember(all, query) {
                if (query.isEmpty()) all else {
                    val q = normalize(query)
                    all.filter { normalize(it.title + " " + it.artist + " " + it.folder).contains(q) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CryoChipButton(
                    text = stringResource(R.string.cryo_music_select_all),
                    iconRes = R.drawable.ic_check,
                    enabled = shown.isNotEmpty() && importing == null,
                    onClick = { state.selected = state.selected + shown.associateBy { it.key } }
                )
                CryoChipButton(
                    text = stringResource(R.string.cryo_music_clear_selection),
                    enabled = state.selected.isNotEmpty() && importing == null,
                    onClick = { state.selected = emptyMap() }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(stringResource(R.string.cryo_music_local_count, shown.size), style = timeStyle(), color = CryoGlassColors.Frost.copy(alpha = 0.7f))
            }
            when {
                loading -> Loading()
                all.isEmpty() -> MessageBox(text = stringResource(R.string.cryo_music_device_empty))
                shown.isEmpty() -> NoResults()
                else -> DeviceSongList(shown, enabled = importing == null)
            }
        }
    }
}

@Composable
private fun FolderList(query: String) {
    val state = MusicUiState
    val all = state.folders
    if (all == null || state.foldersLoading) {
        Loading()
        return
    }
    if (all.isEmpty()) {
        MessageBox(text = stringResource(R.string.cryo_music_folders_empty))
        return
    }
    val shown = remember(all, query) {
        if (query.isEmpty()) all else {
            val q = normalize(query)
            all.filter { normalize(it.path).contains(q) }
        }
    }
    if (shown.isEmpty()) {
        NoResults()
        return
    }
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(shown, key = { it.dir.absolutePath }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.035f))
                    .clickable {
                        state.filter = ""
                        state.openFolder = folder
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(modifier = Modifier.size(20.dp), painter = painterResource(R.drawable.ic_folder_filled), contentDescription = null, tint = Accent)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(folder.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        folder.path,
                        color = CryoGlassColors.Frost.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(stringResource(R.string.cryo_music_folder_mp3_count, folder.count), style = timeStyle(), color = Accent)
            }
        }
    }
}

@Composable
private fun DeviceSongList(songs: List<CryoMusicLibrary.DeviceSong>, enabled: Boolean) {
    val state = MusicUiState
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(songs, key = { it.key }) { song ->
            val checked = song.key in state.selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (checked) Accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.035f))
                    .clickable(enabled = enabled) {
                        state.selected = if (checked) state.selected - song.key else state.selected + (song.key to song)
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (checked) Accent else Color.Transparent)
                        .border(1.5.dp, if (checked) Accent else Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) Icon(modifier = Modifier.size(16.dp), painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = CryoGlassColors.Deep)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    val sub = listOfNotNull(
                        song.artist.ifBlank { null },
                        song.durationMs.takeIf { it > 0 }?.let { formatMusicTime(it) },
                        song.folder.ifBlank { null }
                    ).joinToString("  ·  ")
                    if (sub.isNotEmpty()) {
                        Text(sub, color = CryoGlassColors.Frost.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ tocando agora

@Composable
private fun NowPlayingColumn(modifier: Modifier) {
    val source = CryoNowPlaying.source
    Column(
        modifier = modifier
            .cryoTile(active = source != null, accent = Accent)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MusicArt(model = CryoNowPlaying.art, size = 72.dp, accent = Accent, glow = source != null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = CryoNowPlaying.title?.ifBlank { null } ?: stringResource(R.string.cryo_music_nothing),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, shadow = null),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (source == null) stringResource(R.string.cryo_music_nothing_sub) else CryoNowPlaying.artist.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (source == null) CryoGlassColors.Frost.copy(alpha = 0.7f) else Accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        SeekBar()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundControl(R.drawable.ic_cryo_shuffle, size = 34.dp, active = CryoPlayer.shuffle, onClick = { CryoPlayer.toggleShuffle() })
            RoundControl(R.drawable.ic_cryo_skip_previous, size = 38.dp, enabled = source != null, onClick = { CryoNowPlaying.previous() })
            PlayButton(size = 50.dp, enabled = source != null)
            RoundControl(R.drawable.ic_cryo_skip_next, size = 38.dp, enabled = source != null, onClick = { CryoNowPlaying.next() })
            RoundControl(
                iconRes = if (CryoPlayer.repeat == CryoPlayer.Repeat.ONE) R.drawable.ic_cryo_repeat_one else R.drawable.ic_cryo_repeat,
                size = 34.dp,
                active = CryoPlayer.repeat != CryoPlayer.Repeat.OFF,
                onClick = { CryoPlayer.cycleRepeat() }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_cryo_volume), contentDescription = null, tint = Accent)
                Slider(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp),
                    value = CryoNowPlaying.volume,
                    onValueChange = { CryoNowPlaying.setVolume(it) },
                    onValueChangeFinished = { CryoNowPlaying.saveVolume() },
                    valueRange = 0f..1f,
                    colors = sliderColors()
                )
                Text(text = "${(CryoNowPlaying.volume * 100).toInt()}%", style = timeStyle(), color = Color.White)
            }
            Text(
                text = stringResource(R.string.cryo_music_player_volume_note),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.65f)
            )
        }

        if (source != null && CryoPlayer.error != null && !CryoPlayer.playing) {
            Text(text = stringResource(R.string.cryo_music_play_error), style = MaterialTheme.typography.labelSmall, color = Danger)
        }

        CryoToggleTile(
            modifier = Modifier.fillMaxWidth(),
            iconRes = R.drawable.ic_cryo_pip,
            title = stringResource(R.string.cryo_music_floating),
            subtitle = stringResource(R.string.cryo_music_floating_sub),
            checked = AllSettings.cryoMusicFloating.state,
            onCheckedChange = { AllSettings.cryoMusicFloating.save(it) }
        )
        //estilo do player flutuante (só aparece com ele ligado)
        if (AllSettings.cryoMusicFloating.state) {
            CryoSegmentedTabs(
                titles = listOf(
                    stringResource(R.string.cryo_music_style_default),
                    stringResource(R.string.cryo_music_style_square),
                    stringResource(R.string.cryo_music_style_disc)
                ),
                selected = AllSettings.cryoMusicFloatingStyle.state,
                onSelect = { AllSettings.cryoMusicFloatingStyle.save(it) }
            )
        }
    }
}

/** Barra de tempo: o relógio (2x por segundo, só enquanto toca) redesenha só ela. */
@Composable
private fun SeekBar() {
    val playing = CryoNowPlaying.playing
    val key = CryoNowPlaying.key
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playing, key) {
        now = CryoNowPlaying.positionMs()
        while (playing) {
            delay(500)
            now = CryoNowPlaying.positionMs()
        }
    }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = CryoNowPlaying.durationMs
    val position = if (dragging) dragValue.toLong() else now
    Column {
        Slider(
            modifier = Modifier.height(26.dp),
            enabled = key != null && duration > 0,
            value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                CryoNowPlaying.seekTo(dragValue.toLong())
                now = dragValue.toLong()
                dragging = false
            },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            colors = sliderColors()
        )
        Row {
            Text(formatMusicTime(position), style = timeStyle(), color = CryoGlassColors.Frost)
            Spacer(modifier = Modifier.weight(1f))
            if (CryoNowPlaying.buffering) {
                Text(stringResource(R.string.cryo_music_loading), style = timeStyle(), color = Accent)
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(if (duration > 0) formatMusicTime(duration) else "--:--", style = timeStyle(), color = CryoGlassColors.Frost)
        }
    }
}

@Composable
private fun PlayButton(size: Dp, enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .cryoBackground(cryoGradient())
            .clickable(enabled = enabled) { CryoNowPlaying.togglePlay() },
        contentAlignment = Alignment.Center
    ) {
        if (CryoNowPlaying.buffering) {
            CircularProgressIndicator(modifier = Modifier.size(size * 0.45f), strokeWidth = 2.5.dp, color = CryoGlassColors.Deep)
        } else {
            Icon(
                modifier = Modifier.size(size * 0.52f),
                painter = painterResource(if (CryoNowPlaying.playing) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
    }
}

// ------------------------------------------------------------------ peças

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, hint: String, onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Accent.copy(alpha = 0.5f), CircleShape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_cryo_search), contentDescription = null, tint = Accent)
        TextInput(modifier = Modifier.weight(1f), value = value, onValueChange = onValueChange, hint = hint, imeAction = ImeAction.Search, onAction = onSearch)
        if (value.isNotEmpty()) SmallIconButton(R.drawable.ic_close) { onValueChange("") }
    }
}

/** Campo para o nome da playlist (criar ou renomear). */
@Composable
private fun NameEditor(initial: String, hint: String, confirm: String, onCancel: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TextInput(
            modifier = Modifier.weight(1f),
            value = name,
            onValueChange = { name = it.take(60) },
            hint = hint,
            imeAction = ImeAction.Done,
            onAction = { if (name.isNotBlank()) onConfirm(name) }
        )
        SmallIconButton(R.drawable.ic_close, onClick = onCancel)
        PrimaryChip(text = confirm, enabled = name.isNotBlank(), onClick = { onConfirm(name) })
    }
}

@Composable
private fun TextInput(modifier: Modifier, value: String, onValueChange: (String) -> Unit, hint: String, imeAction: ImeAction, onAction: () -> Unit) {
    Box(modifier = modifier.padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(text = hint, color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onSearch = { onAction() }, onDone = { onAction() })
        )
    }
}

@Composable
private fun ConfirmBar(modifier: Modifier, text: String, button: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CryoGlassColors.Navy)
            .border(1.dp, Danger.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(modifier = Modifier.weight(1f), text = text, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        CryoChipButton(text = stringResource(R.string.cryo_music_cancel), onClick = onCancel)
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Danger)
                .clickable(onClick = onConfirm)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(button, color = CryoGlassColors.Deep, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PrimaryChip(text: String, iconRes: Int? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .cryoBackground(cryoGradient())
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        iconRes?.let { Icon(modifier = Modifier.size(16.dp), painter = painterResource(it), contentDescription = null, tint = CryoGlassColors.Deep) }
        Text(text, color = CryoGlassColors.Deep, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp, color = Accent)
    }
}

@Composable
private fun MessageBox(text: String, button: String? = null, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        button?.let { PrimaryChip(text = it, onClick = onClick) }
    }
}

@Composable
private fun SmallIconButton(iconRes: Int, enabled: Boolean = true, loading: Boolean = false, tint: Color = CryoGlassColors.Frost, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
        } else {
            Icon(modifier = Modifier.size(17.dp), painter = painterResource(iconRes), contentDescription = null, tint = tint)
        }
    }
}

/** Capa: imagem embutida no arquivo, ou nota musical. */
@Composable
fun MusicArt(model: Any?, size: Dp, accent: Color, glow: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size / 5)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.45f), CryoGlassColors.Deep)))
            .then(if (glow) Modifier.border(1.5.dp, accent.copy(alpha = 0.8f), shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (model is ImageBitmap) {
            Image(bitmap = model, contentDescription = null, contentScale = ContentScale.Crop, filterQuality = androidx.compose.ui.graphics.FilterQuality.High, modifier = Modifier.size(size))
        } else {
            Icon(modifier = Modifier.size(size / 2), painter = painterResource(R.drawable.ic_music_note), contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun RoundControl(
    iconRes: Int,
    size: Dp,
    active: Boolean = false,
    enabled: Boolean = true,
    accent: Color = Accent,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .then(
                if (active) Modifier.background(accent.copy(alpha = 0.2f)).border(1.5.dp, accent, CircleShape)
                else Modifier.background(Color.White.copy(alpha = 0.07f)).border(1.dp, Accent.copy(alpha = 0.3f), CircleShape)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(modifier = Modifier.size(size * 0.5f), painter = painterResource(iconRes), contentDescription = null, tint = if (active) accent else Color.White)
    }
}

@Composable
fun sliderColors(accent: Color = Accent) = SliderDefaults.colors(
    thumbColor = Color.White,
    activeTrackColor = accent,
    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
    disabledThumbColor = Color.White.copy(alpha = 0.3f),
    disabledActiveTrackColor = Color.White.copy(alpha = 0.15f),
    disabledInactiveTrackColor = Color.White.copy(alpha = 0.1f)
)

@Composable
private fun timeStyle() = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, shadow = null)

/** Só para as prévias de tela nos testes: tela de importar no modo Pastas. */
@androidx.annotation.VisibleForTesting
@Composable
internal fun ImportPreviewHost() {
    LaunchedEffect(Unit) {}
    MusicUiState.importMode = 1
    MusicUiState.folders = listOf(
        CryoMusicLibrary.DeviceFolder(File("/s/ÁUDIO"), "ÁUDIO", "ÁUDIO", 42),
        CryoMusicLibrary.DeviceFolder(File("/s/Download/Muka"), "Muka", "Download/Muka", 17),
        CryoMusicLibrary.DeviceFolder(File("/s/Music"), "Music", "Music", 230)
    )
    Box(Modifier.fillMaxSize().background(Color(0xFF0B1424)).padding(12.dp)) {
        Box(Modifier.fillMaxSize().padding(start = 300.dp)) {
            FolderList("")
        }
    }
}
