package uz.suhrob.musicplayerapp.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.Player.*
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import uz.suhrob.musicplayerapp.data.model.Song
import uz.suhrob.musicplayerapp.exoplayer.MusicServiceConnection
import uz.suhrob.musicplayerapp.exoplayer.isPlayEnabled
import uz.suhrob.musicplayerapp.exoplayer.isPlaying
import uz.suhrob.musicplayerapp.exoplayer.isPrepared
import uz.suhrob.musicplayerapp.other.Constants.MEDIA_ROOT_ID
import uz.suhrob.musicplayerapp.other.Resource
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val currentPlayingSong = musicServiceConnection.currentPlayingSong
    val playbackState = musicServiceConnection.playbackState
    val repeatMode = musicServiceConnection.repeatMode
    val shuffleEnabled = musicServiceConnection.shuffleEnabled
    var loaded = false

    init {
        Timber.d("Mainviewmodel initialized")
        _mediaItems.postValue(Resource.Loading)
        musicServiceConnection.subscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                    val items = children.map {
                        Song(
                            mediaId = it.mediaId.toString(),
                            title = it.description.title.toString(),
                            artist = it.description.subtitle.toString(),
                            album = it.description.description.toString(),
                            path = it.description.mediaUri.toString(),
                            image = it.description.iconBitmap
                        )
                    }
                    _mediaItems.postValue(Resource.Success(items))
                    loaded = true
                }
            })
    }

    fun skipToNextSong() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong() {
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos: Long) {
        musicServiceConnection.transportControls.seekTo(pos)
    }

    fun playOrToggleSong(mediaItem: Song, toggle: Boolean = false) {
        val isPrepared = playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaItem.mediaId ==
            currentPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)
        ) {
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if (toggle) musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
        } else {
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId, null)
        }
    }

    fun nextRepeatMode() {
        val newRepeatMode = when (repeatMode.value) {
            REPEAT_MODE_OFF -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
            REPEAT_MODE_ONE -> REPEAT_MODE_OFF
            else -> REPEAT_MODE_ALL
        }
        musicServiceConnection.changeRepeatMode(newRepeatMode)
    }

    fun toggleShuffle() {
        musicServiceConnection.enableShuffle(!(shuffleEnabled.value ?: false))
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {})
    }
}