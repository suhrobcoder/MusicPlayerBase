package uz.suhrob.musicplayerapp.exoplayer

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import timber.log.Timber
import uz.suhrob.musicplayerapp.data.model.Song
import kotlin.random.Random

class MusicDataSource(
    private val context: Context
) {

    var songs = emptyList<MediaMetadataCompat>()

    fun fetchMediaMetadata() {
        state = State.STATE_INITIALIZING
        val allSongs = getAllSongs()
        songs = allSongs.map { song ->
            Builder()
                .putString(METADATA_KEY_ARTIST, song.artist)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_MEDIA_URI, song.path)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.artist)
                .putString(METADATA_KEY_ALBUM, song.album)
                .putBitmap(METADATA_KEY_ALBUM_ART, song.image)
                .putBitmap(METADATA_KEY_DISPLAY_ICON, song.image)
                .build()
        }
        state = State.STATE_INITIALIZED
    }

    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(song.getString(METADATA_KEY_MEDIA_URI).toUri()))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asMediaItems() = songs.map { song ->
        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconBitmap(song.description.iconBitmap)
            .setDescription(song.getString(METADATA_KEY_ALBUM))
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }.toMutableList()

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: State = State.STATE_CREATED
        set(value) {
            if (value == State.STATE_INITIALIZED || value == State.STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == State.STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    fun whenReady(action: (Boolean) -> Unit): Boolean {
        if (state == State.STATE_CREATED || state == State.STATE_INITIALIZING) {
            onReadyListeners += action
            return false
        } else {
            action(state == State.STATE_INITIALIZED)
            return true
        }
    }

    private fun getAllSongs(): List<Song> {
        val list = mutableListOf<Song>()
        val metaRetriever = MediaMetadataRetriever()
        try {
            val filePaths = context.assets.list("music")
            filePaths?.forEach { path ->
                val afd = context.assets.openFd("music/$path")
                metaRetriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                val data = metaRetriever.embeddedPicture
                val bitmap = data?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
                list.add(
                    Song(
                        mediaId = Random.nextInt().toString(),
                        title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "",
                        path = "file:///android_asset/music/$path",
                        album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "",
                        artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "",
                        image = bitmap,
                    )
                )
            }
        } catch (e: Exception) {
            Timber.d("AppDebug ${e.message}")
        }
        return list
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}