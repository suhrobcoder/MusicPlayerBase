package uz.suhrob.musicplayerapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.suhrob.musicplayerapp.BuildConfig
import uz.suhrob.musicplayerapp.R
import uz.suhrob.musicplayerapp.data.model.Song
import uz.suhrob.musicplayerapp.data.pref.AppPref
import uz.suhrob.musicplayerapp.databinding.FragmentHomeBinding
import uz.suhrob.musicplayerapp.other.Resource
import uz.suhrob.musicplayerapp.ui.BaseFragment
import uz.suhrob.musicplayerapp.ui.MainActivity
import uz.suhrob.musicplayerapp.ui.adapters.SongAdapter
import uz.suhrob.musicplayerapp.ui.viewmodels.MainViewModel
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val viewModel by activityViewModels<MainViewModel>()

    @Inject
    lateinit var songAdapter: SongAdapter

    @Inject
    lateinit var appPref: AppPref

    private var currentPlayingSong: Song? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        songAdapter.clickListener = { song ->
            viewModel.playOrToggleSong(song)
        }
        binding.songsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }
        (requireActivity() as MainActivity).setSupportActionBar(binding.toolbar)
        viewModel.mediaItems.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    songAdapter.submitList(it.data)
                    Timber.d("Songs ${it.data.size}")
                }
                is Resource.Error -> Unit
                Resource.Loading -> Unit
            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.share_menu -> {
                        Timber.d("AppDebug Share Menu")
                        shareApp()
                        true
                    }
                    R.id.rate_menu -> {
                        Timber.d("AppDebug Rate Menu")
                        rateApp()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val adRequest = AdRequest.Builder().build()
        binding.banner.loadAd(adRequest)
    }

    private fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            val shareMessage =
                """
                ${getString(R.string.app_name).substringBefore(" 20")}ning barcha qo'shiqlari
                https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}
                """.trimIndent()
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_app)))
        } catch (e: Exception) {
            Timber.d("Share App error %s", e.message)
        }
    }

    private fun rateApp() {
        val reviewManager = ReviewManagerFactory.create(requireContext())
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(requireActivity(), reviewInfo)
                flow.addOnCompleteListener {
                    appPref.setReviewed()
                }
            } else {
                Timber.d("Review Error ${task.exception?.message}")
            }
        }
    }
}