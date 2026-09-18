package com.nuvio.tv.ui.screens.networks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.tmdb.TmdbCollectionSourceResolver
import com.nuvio.tv.core.util.hasNoReleaseInfo
import com.nuvio.tv.core.util.isUnreleased
import com.nuvio.tv.data.local.CollectionsDataStore
import com.nuvio.tv.data.local.LayoutPreferenceDataStore
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.TmdbCollectionSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One browsable row: a single network's Movies or TV Shows source. */
data class NetworkRow(
    val key: String,
    val collectionId: String,
    val folderId: String,
    val networkTitle: String,
    val catalogRow: CatalogRow? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class NetworksHubUiState(
    val rows: List<NetworkRow> = emptyList(),
    val isLoading: Boolean = true,
    val posterCardWidthDp: Int = 126,
    val posterCardHeightDp: Int = 189,
    val posterCardCornerRadiusDp: Int = 12,
    val posterLabelsEnabled: Boolean = true,
)

@HiltViewModel
class NetworksHubViewModel @Inject constructor(
    private val collectionsDataStore: CollectionsDataStore,
    private val tmdbCollectionSourceResolver: TmdbCollectionSourceResolver,
    private val layoutPreferenceDataStore: LayoutPreferenceDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworksHubUiState())
    val uiState: StateFlow<NetworksHubUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val collections = collectionsDataStore.collections.first()
            val networkCollections = collections.filter { it.id.startsWith("network_") }
            val hideUnreleasedContent = layoutPreferenceDataStore.hideUnreleasedContent.first()
            val posterCardWidthDp = layoutPreferenceDataStore.posterCardWidthDp.first()
            val posterCardHeightDp = layoutPreferenceDataStore.posterCardHeightDp.first()
            val posterCardCornerRadiusDp = layoutPreferenceDataStore.posterCardCornerRadiusDp.first()
            val posterLabelsEnabled = layoutPreferenceDataStore.posterLabelsEnabled.first()

            data class PendingSource(
                val key: String,
                val folderId: String,
                val networkTitle: String,
                val source: TmdbCollectionSource,
            )

            val pendingByCollection = networkCollections.map { collection ->
                collection to collection.folders.flatMap { folder ->
                    folder.sources.mapIndexedNotNull { sourceIndex, source ->
                        (source as? TmdbCollectionSource)?.let {
                            PendingSource(
                                key = "${collection.id}_${folder.id}_$sourceIndex",
                                folderId = folder.id,
                                networkTitle = collection.title,
                                source = it,
                            )
                        }
                    }
                }
            }

            val initialRows = pendingByCollection.flatMap { (collection, sources) ->
                sources.map { pending ->
                    NetworkRow(
                        key = pending.key,
                        collectionId = collection.id,
                        folderId = pending.folderId,
                        networkTitle = pending.networkTitle,
                        isLoading = true,
                    )
                }
            }

            _uiState.update {
                it.copy(
                    rows = initialRows,
                    isLoading = false,
                    posterCardWidthDp = posterCardWidthDp,
                    posterCardHeightDp = posterCardHeightDp,
                    posterCardCornerRadiusDp = posterCardCornerRadiusDp,
                    posterLabelsEnabled = posterLabelsEnabled,
                )
            }

            pendingByCollection.forEach { (_, sources) ->
                sources.forEach { pending ->
                    launch {
                        tmdbCollectionSourceResolver.resolve(pending.source, page = 1).collect { result ->
                            when (result) {
                                is NetworkResult.Success -> {
                                    val filtered = result.data
                                        .copy(catalogName = pending.networkTitle)
                                        .filteredForRelease(hideUnreleasedContent)
                                    updateRow(pending.key) { row ->
                                        row.copy(catalogRow = filtered, isLoading = false)
                                    }
                                }
                                is NetworkResult.Error -> {
                                    updateRow(pending.key) { row ->
                                        row.copy(isLoading = false, error = result.message)
                                    }
                                }
                                NetworkResult.Loading -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateRow(key: String, transform: (NetworkRow) -> NetworkRow) {
        _uiState.update { state ->
            state.copy(rows = state.rows.map { row -> if (row.key == key) transform(row) else row })
        }
    }

    private fun CatalogRow.filteredForRelease(hideUnreleased: Boolean): CatalogRow {
        if (!hideUnreleased) return this
        val today = java.time.LocalDate.now()
        val filtered = items.filterNot { item -> item.isUnreleased(today) || item.hasNoReleaseInfo() }
        return if (filtered.size == items.size) this else copy(items = filtered)
    }
}
