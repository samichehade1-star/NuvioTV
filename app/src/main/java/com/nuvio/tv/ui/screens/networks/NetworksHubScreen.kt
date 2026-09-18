@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.networks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.CatalogRowSection
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.PosterCardDefaults
import com.nuvio.tv.ui.theme.NuvioTheme

@Composable
fun NetworksHubScreen(
    viewModel: NetworksHubViewModel = hiltViewModel(),
    showBuiltInHeader: Boolean = true,
    onNavigateToDetail: (String, String, String) -> Unit,
    onNavigateToFolderDetail: (String, String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val posterCardStyle = remember(uiState.posterCardWidthDp, uiState.posterCardHeightDp, uiState.posterCardCornerRadiusDp) {
        PosterCardDefaults.Style.copy(
            width = uiState.posterCardWidthDp.dp,
            height = uiState.posterCardHeightDp.dp,
            cornerRadius = uiState.posterCardCornerRadiusDp.dp,
        )
    }
    val listState = rememberLazyListState()
    val rowStates = remember { mutableMapOf<String, LazyListState>() }
    val rowFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val visibleRows = remember(uiState.rows) {
        uiState.rows.filter { row -> (row.catalogRow?.items?.isNotEmpty()) == true }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background),
    ) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
            visibleRows.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyScreenState(
                        title = stringResource(R.string.networks_hub_empty_title),
                        subtitle = stringResource(R.string.networks_hub_empty_message),
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = NuvioTheme.spacing.xl, bottom = NuvioTheme.spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xxl),
                ) {
                    if (showBuiltInHeader) {
                        item(key = "networks_hub_header") {
                            Text(
                                text = stringResource(R.string.networks_hub_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = NuvioTheme.colors.TextPrimary,
                                modifier = Modifier.padding(horizontal = NuvioTheme.spacing.xxxl),
                            )
                        }
                    }

                    itemsIndexed(
                        items = visibleRows,
                        key = { _, row -> row.key },
                    ) { _, row ->
                        val catalogRow = row.catalogRow ?: return@itemsIndexed
                        val rowListState = rowStates.getOrPut(row.key) { LazyListState() }
                        val rowFocusRequester = rowFocusRequesters.getOrPut(row.key) { FocusRequester() }

                        CatalogRowSection(
                            catalogRow = catalogRow,
                            posterCardStyle = posterCardStyle,
                            showPosterLabels = uiState.posterLabelsEnabled,
                            showAddonName = false,
                            showCatalogTypeSuffix = true,
                            onItemClick = { id, type, addonBaseUrl ->
                                onNavigateToDetail(id, type, addonBaseUrl)
                            },
                            onSeeAll = {
                                onNavigateToFolderDetail(row.collectionId, row.folderId)
                            },
                            listState = rowListState,
                            rowFocusRequester = rowFocusRequester,
                        )
                    }
                }
            }
        }
    }
}
