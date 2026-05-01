package code.name.monkey.retromusic.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class YoutubeTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: Long,
    val url: String
)

object YoutubeSearchService {

    @Volatile
    private var initialized = false

    fun init() {
        if (initialized) return
        try {
            NewPipe.init(DownloaderImpl.getInstance())
            initialized = true
            Log.d("YoutubeSearchService", "NewPipe extractor initialized")
        } catch (e: Exception) {
            Log.e("YoutubeSearchService", "Init failed: ${e.message}")
        }
    }

    private fun ensureInitialized() {
        if (!initialized) {
            init()
        }
    }

    suspend fun search(query: String): List<YoutubeTrack> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val soundCloud = ServiceList.SoundCloud
            val searchExtractor = soundCloud.getSearchExtractor(query)
            searchExtractor.fetchPage()

            val results = mutableListOf<YoutubeTrack>()
            for (item in searchExtractor.initialPage.items) {
                if (item is StreamInfoItem) {
                    results.add(
                        YoutubeTrack(
                            videoId = item.url.substringAfterLast("/"),
                            title = item.name,
                            artist = item.uploaderName ?: "Bilinmiyor",
                            thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: "",
                            duration = item.duration,
                            url = item.url
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.e("YoutubeSearchService", "Search error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAudioStreamUrl(trackUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val streamInfo = StreamInfo.getInfo(ServiceList.SoundCloud, trackUrl)
            streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
        } catch (e: Exception) {
            Log.e("YoutubeSearchService", "Stream URL error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
