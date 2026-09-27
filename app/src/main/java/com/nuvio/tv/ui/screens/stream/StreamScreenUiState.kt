package com.nuvio.tv.ui.screens.stream

import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.ui.components.SourceChipItem
import com.nuvio.tv.ui.components.SourceChipStatus

data class StreamScreenUiState(
    val isLoading: Boolean = true,
    val isDirectAutoPlayFlow: Boolean = false,
    val showDirectAutoPlayOverlay: Boolean = false,
    val autoPlayDecided: Boolean = false,
    val externalPlayerOverlayVisible: Boolean = false,
    val directAutoPlayMessage: String? = null,
    val directAutoPlayProgress: Float? = null,
    val videoId: String = "",
    val contentType: String = "",
    val title: String = "",
    val poster: String? = null,
    val backdrop: String? = null,
    val logo: String? = null,
    // Episode-specific fields
    val season: Int? = null,
    val episode: Int? = null,
    val episodeName: String? = null,
    val runtime: Int? = null,
    // Movie-specific fields
    val genres: String? = null,
    val year: String? = null,
    val addonStreams: List<AddonStreams> = emptyList(),
    val allStreams: List<Stream> = emptyList(),
    val selectedAddonFilter: String? = null, // null means "All"
    val selectedQualityFilter: String? = null, // null means "All"
    val filteredStreams: List<Stream> = emptyList(),
    val availableAddons: List<String> = emptyList(),
    val sourceChips: List<SourceChipItem> = emptyList(),
    val autoPlayStream: Stream? = null,
    val autoPlayPlaybackInfo: StreamPlaybackInfo? = null,
    val error: String? = null,
    val playbackErrorMessage: String? = null
) {
    val isEpisode: Boolean get() = season != null && episode != null

    /** Best pick across every stream found (independent of the active filters), if any stands out. */
    val recommendedStream: Stream?
        get() = allStreams.maxByOrNull { it.recommendationScore }
            ?.takeIf { it.recommendationScore >= 30 }

    /** Quality buckets present across the addon-filtered selection, for building the filter row. */
    val availableQualityFilters: List<String>
        get() {
            val byAddon = if (selectedAddonFilter == null) allStreams
                else allStreams.filter { it.addonName == selectedAddonFilter }
            val present = byAddon.mapNotNull { it.qualityBucket }.toSet()
            return listOf("4K", "1080p", "720p", "SD").filter { it in present }
        }
}

sealed class StreamScreenEvent {
    data class OnAddonFilterSelected(val addonName: String?) : StreamScreenEvent()
    data class OnQualityFilterSelected(val quality: String?) : StreamScreenEvent()
    data class OnStreamSelected(val stream: Stream) : StreamScreenEvent()
    data object OnAutoPlayConsumed : StreamScreenEvent()
    data object OnRefresh : StreamScreenEvent()
    data object OnRetry : StreamScreenEvent()
    data object OnBackPress : StreamScreenEvent()
    data object OnResume : StreamScreenEvent()
}
