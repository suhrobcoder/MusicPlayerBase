package uz.suhrob.musicplayerapp.di

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.suhrob.musicplayerapp.exoplayer.MusicServiceConnection
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {
    @Singleton
    @Provides
    fun provideMusicServiceConnection(
        @ApplicationContext context: Context,
        exoPlayer: ExoPlayer,
    ) = MusicServiceConnection(context, exoPlayer)
}