package uz.suhrob.musicplayerapp.ui.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.suhrob.musicplayerapp.exoplayer.MusicService
import uz.suhrob.musicplayerapp.exoplayer.MusicServiceConnection
import uz.suhrob.musicplayerapp.exoplayer.currentPlaybackPosition
import uz.suhrob.musicplayerapp.other.Constants.UPDATE_PLAYER_POSITION_INTERVAL
import javax.inject.Inject

@HiltViewModel
class SongViewModel @Inject constructor(
    musicServiceConnection: MusicServiceConnection
): ViewModel() {
    private val playbackState = musicServiceConnection.playbackState

    private val _currentSongDuration = MutableLiveData<Long>()
    val currentSongDuration: LiveData<Long> = _currentSongDuration

    private val _currentPlayerPosition = MutableLiveData<Long>()
    val currentPlayerPosition: LiveData<Long> = _currentPlayerPosition

    init {
        updateCurrentPlayerPosition()
    }

    private fun updateCurrentPlayerPosition() {
        viewModelScope.launch {
            while (true) {
                val pos = playbackState.value?.currentPlaybackPosition
                if (currentPlayerPosition.value != pos) {
                    _currentPlayerPosition.postValue(pos ?: 0)
                    _currentSongDuration.postValue(MusicService.currentSongDuration)
                }
                delay(UPDATE_PLAYER_POSITION_INTERVAL)
            }
        }
    }
}