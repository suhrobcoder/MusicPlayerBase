package uz.suhrob.musicplayerapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.exoplayer2.Player
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.suhrob.musicplayerapp.R
import uz.suhrob.musicplayerapp.data.model.Song
import uz.suhrob.musicplayerapp.databinding.FragmentSongBinding
import uz.suhrob.musicplayerapp.exoplayer.isPlaying
import uz.suhrob.musicplayerapp.ui.BaseFragment
import uz.suhrob.musicplayerapp.ui.MainActivity
import uz.suhrob.musicplayerapp.ui.viewmodels.MainViewModel
import uz.suhrob.musicplayerapp.ui.viewmodels.SongViewModel

@AndroidEntryPoint
class SongFragment : BaseFragment<FragmentSongBinding>() {
    private val mainViewModel by activityViewModels<MainViewModel>()
    private val songViewModel by viewModels<SongViewModel>()

    private var pos = 0
    private lateinit var currentSong: Song
    private var isSeekbarTracking = false

    private lateinit var interstitialAd: InterstitialAd

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSongBinding = FragmentSongBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songPos.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                pos = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeekbarTracking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mainViewModel.seekTo(pos.toLong())
                isSeekbarTracking = false
            }

        })
        binding.prevBtn.setOnClickListener {
            mainViewModel.skipToPreviousSong()
        }
        binding.nextBtn.setOnClickListener {
            mainViewModel.skipToNextSong()
        }
        binding.playBtn.setOnClickListener {
            mainViewModel.playOrToggleSong(currentSong, true)
        }
        binding.repeatModeBtn.setOnClickListener {
            mainViewModel.nextRepeatMode()
        }
        binding.shuffleBtn.setOnClickListener {
            mainViewModel.toggleShuffle()
        }
        binding.playBtn1.setOnClickListener {
            mainViewModel.playOrToggleSong(currentSong, true)
        }
        (requireActivity() as MainActivity).onPanelSlide = { slide ->
            binding.bottomLayout.alpha = 1 - slide
            binding.playBtn1.isEnabled = slide != 1f
        }
        mainViewModel.currentPlayingSong.observe(viewLifecycleOwner) {
            if (it == null) return@observe

            val _song = if (it.description.mediaId != null) Song(
                mediaId = it.description.mediaId.toString(),
                title = it.description.title.toString(),
                artist = it.description.subtitle.toString(),
                album = it.description.description.toString(),
                path = it.description.mediaUri.toString(),
                image = it.description.iconBitmap
            ) else null
            _song?.let { song ->
                Timber.d(song.toString())
                binding.songTitle.text = song.title
                binding.songSubtitle.text = song.artist
                currentSong = song
                binding.songTitle1.text = song.title
                binding.songSubtitle1.text = song.artist
            }
        }
        songViewModel.currentPlayerPosition.observe(viewLifecycleOwner) {
            binding.songCurrentPos.text = formatTime(it)
            if (!isSeekbarTracking) {
                binding.songPos.progress = it.toInt()
            }
        }
        songViewModel.currentSongDuration.observe(viewLifecycleOwner) {
            binding.songPos.max = it.toInt()
            binding.songDuration.text = formatTime(it.coerceIn(0L, 600_000))
            Timber.d("song_duration $it")
        }
        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            it?.let {
                if (it.isPlaying) {
                    binding.playBtn.setImageResource(R.drawable.ic_pause)
                    binding.playBtn1.setImageResource(R.drawable.ic_pause)
                } else {
                    binding.playBtn.setImageResource(R.drawable.ic_play)
                    binding.playBtn1.setImageResource(R.drawable.ic_play)
                }
            }
        }
        mainViewModel.repeatMode.observe(viewLifecycleOwner) {
            when (it) {
                Player.REPEAT_MODE_OFF -> binding.repeatModeBtn.setImageResource(R.drawable.ic_repeat_off)
                Player.REPEAT_MODE_ALL -> binding.repeatModeBtn.setImageResource(R.drawable.ic_repeat)
                Player.REPEAT_MODE_ONE -> binding.repeatModeBtn.setImageResource(R.drawable.ic_repeat_one)
            }
        }
        mainViewModel.shuffleEnabled.observe(viewLifecycleOwner) {
            if (it) {
                binding.shuffleBtn.setImageResource(R.drawable.ic_shuffle)
            } else {
                binding.shuffleBtn.setImageResource(R.drawable.ic_shuffle_off)
            }
        }

        val adRequest = AdRequest.Builder().build()
        binding.banner.loadAd(adRequest)

//        val interstitialAdRequest = AdRequest.Builder().build()
//        InterstitialAd.load(
//            requireContext(),
//            "ca-app-pub-7532241080505290/8563530192", interstitialAdRequest,
//            object : InterstitialAdLoadCallback() {
//                override fun onAdLoaded(ad: InterstitialAd) {
//                    interstitialAd = ad
//                    interstitialAd.show(requireActivity())
//                }
//
//                override fun onAdFailedToLoad(error: LoadAdError) {
//                    Timber.d("AdError ${error.message}")
//                }
//            },
//        )
    }

    private fun formatTime(time: Long): String {
        val minute = time / 1000 / 60
        val second = (time / 1000) % 60
        return "$minute:${if (second > 9) "" else "0"}$second"
    }
}