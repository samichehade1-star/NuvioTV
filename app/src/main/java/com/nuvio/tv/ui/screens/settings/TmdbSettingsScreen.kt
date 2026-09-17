@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import com.nuvio.tv.ui.theme.NuvioTheme

import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.build.AppFeaturePolicy
import com.nuvio.tv.data.local.AVAILABLE_TMDB_LANGUAGES
import com.nuvio.tv.data.local.displayName
import com.nuvio.tv.ui.components.NuvioDialog

@Composable
fun TmdbSettingsScreen(
    viewModel: TmdbSettingsViewModel = hiltViewModel(),
    onBackPress: () -> Unit
) {
    BackHandler { onBackPress() }

    SettingsStandaloneScaffold(
        title = stringResource(R.string.tmdb_title),
        subtitle = stringResource(R.string.tmdb_subtitle)
    ) {
        TmdbSettingsContent(viewModel = viewModel)
    }
}

@Composable
fun TmdbSettingsContent(
    viewModel: TmdbSettingsViewModel = hiltViewModel(),
    initialFocusRequester: FocusRequester? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsDetailHeader(
            title = stringResource(R.string.tmdb_title),
            subtitle = stringResource(R.string.tmdb_subtitle)
        )

        SettingsGroupCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val tmdbListState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = tmdbListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = NuvioTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "tmdb_enabled") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_enable_title),
                        subtitle = stringResource(R.string.tmdb_enable_subtitle),
                        checked = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleEnabled(!uiState.enabled)) },
                        modifier = Modifier
                            .padding(top = NuvioTheme.spacing.xxs)
                            .then(
                                if (initialFocusRequester != null) {
                                    Modifier.focusRequester(initialFocusRequester)
                                } else {
                                    Modifier
                                })
                    )
                }

                item(key = "tmdb_api_key") {
                    SettingsActionRow(
                        title = stringResource(R.string.tmdb_api_key_title),
                        subtitle = stringResource(R.string.tmdb_api_key_subtitle),
                        value = if (uiState.apiKey.isBlank()) {
                            stringResource(R.string.tmdb_api_key_not_set)
                        } else {
                            stringResource(R.string.tmdb_api_key_set)
                        },
                        enabled = uiState.enabled,
                        onClick = { showApiKeyDialog = true }
                    )
                }

                item(key = "tmdb_modern_home_enabled") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_modern_home_title),
                        subtitle = stringResource(R.string.tmdb_modern_home_subtitle),
                        checked = uiState.modernHomeEnabled,
                        enabled = uiState.enabled,
                        onToggle = {
                            viewModel.onEvent(
                                TmdbSettingsEvent.ToggleModernHomeEnabled(!uiState.modernHomeEnabled)
                            )
                        }
                    )
                }

                item(key = "tmdb_enrich_continue_watching") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_enrich_continue_watching_title),
                        subtitle = stringResource(R.string.tmdb_enrich_continue_watching_subtitle),
                        checked = uiState.enrichContinueWatching,
                        enabled = uiState.enabled,
                        onToggle = {
                            viewModel.onEvent(
                                TmdbSettingsEvent.ToggleEnrichContinueWatching(!uiState.enrichContinueWatching)
                            )
                        }
                    )
                }

                item(key = "tmdb_language") {
                    val languageName = AVAILABLE_TMDB_LANGUAGES
                        .find { it.code == uiState.language }
                        ?.displayName
                        ?: uiState.language.uppercase()
                    SettingsActionRow(
                        title = stringResource(R.string.tmdb_language_title),
                        subtitle = stringResource(R.string.tmdb_language_subtitle),
                        value = languageName,
                        enabled = uiState.enabled,
                        onClick = { showLanguageDialog = true }
                    )
                }

                item(key = "tmdb_artwork") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_artwork_title),
                        subtitle = stringResource(R.string.tmdb_artwork_subtitle),
                        checked = uiState.useArtwork,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleArtwork(!uiState.useArtwork)) }
                    )
                }

                item(key = "tmdb_basic_info") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_basic_info_title),
                        subtitle = stringResource(R.string.tmdb_basic_info_subtitle),
                        checked = uiState.useBasicInfo,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleBasicInfo(!uiState.useBasicInfo)) }
                    )
                }

                item(key = "tmdb_details") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_details_title),
                        subtitle = stringResource(R.string.tmdb_details_subtitle),
                        checked = uiState.useDetails,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleDetails(!uiState.useDetails)) }
                    )
                }

                item(key = "tmdb_credits") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_credits_title),
                        subtitle = stringResource(R.string.tmdb_credits_subtitle),
                        checked = uiState.useCredits,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleCredits(!uiState.useCredits)) }
                    )
                }

                item(key = "tmdb_productions") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_productions_title),
                        subtitle = stringResource(R.string.tmdb_productions_subtitle),
                        checked = uiState.useProductions,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleProductions(!uiState.useProductions)) }
                    )
                }

                item(key = "tmdb_networks") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_networks_title),
                        subtitle = stringResource(R.string.tmdb_networks_subtitle),
                        checked = uiState.useNetworks,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleNetworks(!uiState.useNetworks)) }
                    )
                }

                item(key = "tmdb_episodes") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_episodes_title),
                        subtitle = stringResource(R.string.tmdb_episodes_subtitle),
                        checked = uiState.useEpisodes,
                        enabled = uiState.enabled,
                        onToggle = { viewModel.onEvent(TmdbSettingsEvent.ToggleEpisodes(!uiState.useEpisodes)) }
                    )
                }

                if (AppFeaturePolicy.inAppTrailerPlaybackEnabled) {
                    item(key = "tmdb_trailers") {
                        SettingsToggleRow(
                            title = stringResource(R.string.tmdb_trailers_title),
                            subtitle = stringResource(R.string.tmdb_trailers_subtitle),
                            checked = uiState.useTrailers,
                            enabled = uiState.enabled,
                            onToggle = {
                                viewModel.onEvent(
                                    TmdbSettingsEvent.ToggleTrailers(!uiState.useTrailers)
                                )
                            }
                        )
                    }
                }

                item(key = "tmdb_more_like_this") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_more_like_this_title),
                        subtitle = stringResource(R.string.tmdb_more_like_this_subtitle),
                        checked = uiState.useMoreLikeThis,
                        enabled = uiState.enabled,
                        onToggle = {
                            viewModel.onEvent(
                                TmdbSettingsEvent.ToggleMoreLikeThis(!uiState.useMoreLikeThis)
                            )
                        }
                    )
                }

                item(key = "tmdb_collections") {
                    SettingsToggleRow(
                        title = stringResource(R.string.tmdb_collections_title),
                        subtitle = stringResource(R.string.tmdb_collections_subtitle),
                        checked = uiState.useCollections,
                        enabled = uiState.enabled,
                        onToggle = {
                            viewModel.onEvent(
                                TmdbSettingsEvent.ToggleCollections(!uiState.useCollections)
                            )
                        }
                    )
                }

            }
            SettingsVerticalScrollIndicators(state = tmdbListState)
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            title = stringResource(R.string.tmdb_language_dialog_title),
            selectedLanguage = uiState.language,
            showNoneOption = false,
            onLanguageSelected = { language ->
                viewModel.onEvent(TmdbSettingsEvent.SetLanguage(language ?: "en"))
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showApiKeyDialog) {
        TmdbApiKeyDialog(
            currentValue = uiState.apiKey,
            onSave = { value ->
                viewModel.onEvent(TmdbSettingsEvent.SetApiKey(value))
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
private fun TmdbApiKeyDialog(
    currentValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    var isInputFocused by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val submit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onSave(value.trim())
        Toast.makeText(context, context.getString(R.string.tmdb_api_key_saved), Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.tmdb_api_key_dialog_title),
        subtitle = stringResource(R.string.tmdb_api_key_dialog_subtitle),
        width = 700.dp,
        suppressFirstKeyUp = false
    ) {
        Card(
            onClick = { inputFocusRequester.requestFocus() },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isInputFocused = it.isFocused || it.hasFocus },
            colors = CardDefaults.colors(
                containerColor = NuvioTheme.colors.BackgroundElevated,
                focusedContainerColor = NuvioTheme.colors.BackgroundElevated
            ),
            border = CardDefaults.border(
                border = Border(
                    border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border),
                    shape = RoundedCornerShape(10.dp)
                ),
                focusedBorder = Border(
                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                    shape = RoundedCornerShape(10.dp)
                )
            ),
            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
            scale = CardDefaults.scale(focusedScale = 1f)
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = NuvioTheme.spacing.md)) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(inputFocusRequester)
                        .onKeyEvent { event ->
                            val native = event.nativeKeyEvent
                            when {
                                native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER &&
                                    native.action == KeyEvent.ACTION_DOWN -> true
                                (native.keyCode == KeyEvent.KEYCODE_ENTER ||
                                    native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) &&
                                    native.action == KeyEvent.ACTION_DOWN -> {
                                    submit()
                                    true
                                }
                                else -> false
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = NuvioTheme.colors.TextPrimary),
                    cursorBrush = SolidColor(
                        if (isInputFocused) NuvioTheme.colors.Primary else Color.Transparent
                    ),
                    decorationBox = { innerTextField ->
                        if (value.isBlank()) {
                            Text(
                                text = stringResource(R.string.tmdb_api_key_dialog_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = NuvioTheme.colors.TextTertiary
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        SettingsDialogActionRow {
            SettingsDialogActionButton(
                text = stringResource(R.string.action_clear),
                onClick = { value = "" }
            )
            SettingsDialogActionButton(
                text = stringResource(R.string.action_save),
                onClick = { submit() },
                primary = true
            )
        }
    }
}
