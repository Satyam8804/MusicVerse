package com.example.musicverse

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder

class MusicPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null


    override fun onCreate() {
        super.onCreate()
        // Initialize the MediaPlayer
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setOnPreparedListener { startPlayback() }
        mediaPlayer?.setOnErrorListener { mp, what, extra -> false }
        mediaPlayer?.setOnCompletionListener { stopSelf() }
    }



    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            when (action) {
                ACTION_PLAY -> {
                    val musicUriString = intent.getStringExtra(EXTRA_MUSIC_URI)
                    if (!musicUriString.isNullOrEmpty()) {
                        val musicUri = Uri.parse(musicUriString)
                        playMusic(musicUri)
                    }
                }
                ACTION_STOP -> stopPlayback()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playMusic(musicUri: Uri) {
        stopPlayback()
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(applicationContext, musicUri)
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPlayback() {
        this.mediaPlayer?.start()
    }

    private fun stopPlayback() {
        this.mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
        }
    }


    companion object {
        private var mediaPlayer: MediaPlayer? = null

        const val ACTION_STOP = "com.example.musicverse.action.STOP"
        const val EXTRA_MUSIC_URI = "com.example.musicverse.extra.MUSIC_URI"
        const val ACTION_PLAY = "com.example.musicverse.ACTION_PLAY"
        fun isPlaying(): Boolean {
            return mediaPlayer?.isPlaying ?: false
        }
    }

}
