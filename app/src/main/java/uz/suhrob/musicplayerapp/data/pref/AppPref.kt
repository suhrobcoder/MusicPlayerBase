package uz.suhrob.musicplayerapp.data.pref

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import uz.suhrob.musicplayerapp.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPref @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

    val shouldShowReviewDialog: Boolean
            get() {
                val time = sharedPreferences.getLong("installed_time", 0L)
                if (time == 0L) {
                    sharedPreferences.edit { putLong("installed_time", System.currentTimeMillis()) }
                    return false
                }
                val reviewed = sharedPreferences.getBoolean("reviewed", false)
                if (System.currentTimeMillis() > time + 3 * 86400 * 1000 && !reviewed) {
                    return true
                }
                return false
            }

    fun setReviewed() {
        sharedPreferences.edit { putBoolean("reviewed", true) }
    }
}