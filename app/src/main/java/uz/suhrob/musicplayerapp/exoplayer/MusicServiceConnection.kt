package uz.suhrob.musicplayerapp.exoplayer

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import uz.suhrob.musicplayerapp.other.Event
import uz.suhrob.musicplayerapp.other.Resource

class MusicServiceConnection(
    context: Context,
    private val exoPlayer: ExoPlayer,
) {
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected

    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    private val _currentPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val currentPlayingSong: LiveData<MediaMetadataCompat?> = _currentPlayingSong

    private val _repeatMode = MutableLiveData<Int>(Player.REPEAT_MODE_ALL)
    val repeatMode: LiveData<Int> = _repeatMode

    private val _shuffleEnabled = MutableLiveData(false)
    val shuffleEnabled: LiveData<Boolean> = _shuffleEnabled

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    lateinit var mediaController: MediaControllerCompat

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MusicService::class.java),
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() }

    val transportControls
        get() = mediaController.transportControls

    init {
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
    }

    fun changeRepeatMode(@Player.RepeatMode mode: Int) {
        exoPlayer.repeatMode = mode
        _repeatMode.postValue(mode)
    }

    fun enableShuffle(enabled: Boolean) {
        exoPlayer.shuffleModeEnabled = enabled
        _shuffleEnabled.postValue(enabled)
    }

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(Event(Resource.Success(true)))
        }

        override fun onConnectionSuspended() {
            _isConnected.postValue(Event(Resource.Error("The Connection was suspended")))
        }

        override fun onConnectionFailed() {
            _isConnected.postValue(Event(Resource.Error("Couldn't connect to media browser")))
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _currentPlayingSong.postValue(metadata)
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}