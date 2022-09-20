package uz.suhrob.musicplayerapp.exoplayer.callbacks

import com.google.android.exoplayer2.Player
import uz.suhrob.musicplayerapp.exoplayer.MusicService

class MusicPlayerEventListener(
    private val musicService: MusicService
): Player.Listener {
    override fun onPlaybackStateChanged(@Player.State state: Int) {
        if (state == Player.STATE_READY) {
            musicService.stopForeground(false)
        }
    }
}