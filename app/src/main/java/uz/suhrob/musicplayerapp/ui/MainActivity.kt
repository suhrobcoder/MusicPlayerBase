package uz.suhrob.musicplayerapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.ads.MobileAds
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import dagger.hilt.android.AndroidEntryPoint
import hotchemi.android.rate.AppRate
import timber.log.Timber
import uz.suhrob.musicplayerapp.databinding.ActivityMainBinding
import uz.suhrob.musicplayerapp.ui.viewmodels.MainViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    var onPanelSlide: ((Float) -> Unit)? = null

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !viewModel.loaded }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this)

        binding.root.addPanelSlideListener(object: SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                Timber.d("onPanelSlide $slideOffset")
                onPanelSlide?.invoke(slideOffset)
            }

            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {
                Timber.d("onPanelStateChanged $newState")
            }
        })

        AppRate.with(this)
            .setInstallDays(3)
            .setLaunchTimes(3)
            .setRemindInterval(1)
            .setShowLaterButton(true)
            .setDebug(false)
            .monitor()

        AppRate.showRateDialogIfMeetsConditions(this)
    }

    override fun onBackPressed() {
        if (binding.root.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            binding.root.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        } else {
            super.onBackPressed()
        }
    }
}