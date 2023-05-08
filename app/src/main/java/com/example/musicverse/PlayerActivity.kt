package com.example.musicverse

import Music
import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import java.io.IOException
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var playPause: Button
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var currentDurationTextView: TextView
    private lateinit var totalDurationTextView: TextView
    private lateinit var songName :TextView
    private var isPlaying = false
    private var currentSongIndex: Int = 0
    private var currentDuration = 0
    private  var musicList: List<Music>? = null
    private val notificationId = 1
    private lateinit var musicThumbnailImageView:ImageView
    private lateinit var linearLayout: LinearLayout
    private var rotationAnimator: ObjectAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {

        override fun run() {

            updateSeekBar()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        val musicUri = intent.getStringExtra("musicUri")
        val songTitle = intent.getStringExtra("title")


        prevButton = findViewById(R.id.btn_prev)
        nextButton = findViewById(R.id.btn_next)
        seekBar = findViewById(R.id.seekbar)
        playPause = findViewById(R.id.play_pause)
        currentDurationTextView = findViewById(R.id.txtstart)
        totalDurationTextView = findViewById(R.id.txtEnd)
        songName = findViewById(R.id.txtsn)
        musicThumbnailImageView = findViewById(R.id.imageView)
        linearLayout = findViewById(R.id.linear_layout)
        songName.text = songTitle
        songName.isSelected = true


        val parcelableList = intent.getParcelableArrayListExtra<Parcelable>("musicList")
        musicList = parcelableList?.mapNotNull { it as? Music }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            try {
                setDataSource(this@PlayerActivity, Uri.parse(musicUri))
                prepareAsync()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        prevButton.setOnClickListener {
            prevSong()
        }

        nextButton.setOnClickListener {
            nextSong()
        }

        val thumbnailByteArray = intent.getByteArrayExtra("thumbnail")
        if (thumbnailByteArray != null) {
            val thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnailByteArray, 0, thumbnailByteArray.size)
            musicThumbnailImageView.setImageBitmap(thumbnailBitmap)
        } else {
            // Set a default thumbnail image if no thumbnail is available
            musicThumbnailImageView.setImageResource(R.drawable.music)
        }

        if (thumbnailByteArray != null) {
            val thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnailByteArray, 0, thumbnailByteArray.size)
            musicThumbnailImageView.setImageBitmap(thumbnailBitmap)

            // Extract the dominant color from the thumbnail
            val palette = Palette.from(thumbnailBitmap).generate()
            val dominantColor = palette.getDominantColor(ContextCompat.getColor(this, R.color.black))

            // Create a gradient drawable with the dominant color
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(dominantColor, ContextCompat.getColor(this, R.color.black))
            )

            // Set the background gradient to the linear layout
            linearLayout.background = gradientDrawable
        } else {
            // Set a default thumbnail image if no thumbnail is available
            linearLayout.setBackgroundResource(R.drawable.background_img)
        }


        mediaPlayer.setOnPreparedListener {
            seekBar.max = mediaPlayer.duration
            updateTotalDuration(mediaPlayer.duration.toLong())
            mediaPlayer.start()
            playPause.setBackgroundResource(R.drawable.pause)
            isPlaying = true
            updateSeekBar()
            handler.postDelayed(updateSeekBarRunnable, 1000)
        }



        playPause.setOnClickListener {
            if (isPlaying) {
                pauseMusic()
            } else {
                playMusic()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentDuration = progress
                    mediaPlayer.seekTo(progress)
                    updateCurrentDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // No action needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isPlaying) {
                    handler.postDelayed(updateSeekBarRunnable, 1000)
                }
            }
        })
    }


    private fun playMusic() {
        mediaPlayer.start()
        playPause.setBackgroundResource(R.drawable.pause)
        isPlaying = true
        updateSeekBar()
        handler.postDelayed(updateSeekBarRunnable, 1000)
        startRotationAnimation()
    }

    private fun pauseMusic() {
        mediaPlayer.pause()
        playPause.setBackgroundResource(R.drawable.play)
        isPlaying = false
        handler.removeCallbacks(updateSeekBarRunnable)
        stopRotationAnimation()

    }

    private fun updateSeekBar() {
        val currentPosition =mediaPlayer.currentPosition
        seekBar.progress = currentPosition
        updateCurrentDuration(currentPosition.toLong())
    }

    private fun updateCurrentDuration(duration: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes)
        currentDurationTextView.text = String.format("%02d:%02d", minutes, seconds)
    }
    private fun updateTotalDuration(duration: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes)
        totalDurationTextView.text = String.format("%02d:%02d", minutes, seconds)
    }
    private fun startRotationAnimation() {
        rotationAnimator = ObjectAnimator.ofFloat(musicThumbnailImageView, "rotation", 0f, 360f).apply {
            duration = 10000 // Set the duration of one complete rotation (in milliseconds)
            repeatCount = ObjectAnimator.INFINITE // Repeat the animation indefinitely
            interpolator = LinearInterpolator() // Use a linear interpolator for smooth rotation
            start()
        }
    }

    private fun stopRotationAnimation() {
        rotationAnimator?.cancel()
    }


    private fun playMusic(songUri: String, thumbnail: Bitmap?) {
        mediaPlayer.apply {
            reset()
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            try {
                setDataSource(this@PlayerActivity, Uri.parse(songUri))
                prepareAsync()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // Update the thumbnail image view
        if (thumbnail != null) {
            musicThumbnailImageView.setImageBitmap(thumbnail)
        } else {
            // Set a default thumbnail image if no thumbnail is available
            musicThumbnailImageView.setImageResource(R.drawable.music)
        }
    }

    private fun prevSong() {

        // Decrement the index to move to the previous song
        currentSongIndex--

        // Check if the index is within the bounds of the musicList
        if (currentSongIndex < 0) {
            // Handle reaching the beginning of the playlist, for example, you can loop to the last song
            currentSongIndex = musicList!!.size - 1
        }
        if (musicList.isNullOrEmpty()) {
            // Handle the case when musicList is null or empty
            return
        }
        // Get the URI of the previous song
        val prevSongUri = musicList!![currentSongIndex].musicUri
        val prevSongTitle = musicList!![currentSongIndex].title

// Update the song name TextView
        songName.text = prevSongTitle
        songName.isSelected = true

        // Get the thumbnail of the previous song
        val prevSongThumbnail = getMusicThumbnail(prevSongUri)

        // Play the previous song
        playMusic(prevSongUri.toString(), prevSongThumbnail)
    }

    private fun nextSong() {
        // Increment the index to move to the next song
        currentSongIndex++

        // Check if the index is within the bounds of the musicList
        if (currentSongIndex >= musicList!!.size) {
            // Handle reaching the end of the playlist, for example, you can loop back to the first song
            currentSongIndex = 0
        }
        if (musicList.isNullOrEmpty()) {
            // Handle the case when musicList is null or empty
            return
        }

        // Get the URI of the next song
        val nextSongUri = musicList!![currentSongIndex].musicUri


        val nextSongTitle = musicList!![currentSongIndex].title

// Update the song name TextView
        songName.text = nextSongTitle
        songName.isSelected = true
        // Get the thumbnail of the next song
        val nextSongThumbnail = getMusicThumbnail(nextSongUri)

        // Play the next song
        playMusic(nextSongUri.toString(), nextSongThumbnail)
    }

    private fun getMusicThumbnail(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        val rawArt = retriever.embeddedPicture
        retriever.release()

        return if (rawArt != null) {
            BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size)
        } else {
            null
        }
    }

}

