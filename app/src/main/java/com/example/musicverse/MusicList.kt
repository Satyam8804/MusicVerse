package com.example.musicverse

import Music
import MusicAdapter
import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream

import java.util.*


class MusicList : AppCompatActivity(), MusicAdapter.OnItemClickListener {

    private lateinit var musicRecyclerView: RecyclerView
    private lateinit var musicAdapter: MusicAdapter
    private lateinit var musicList: ArrayList<Music>
    private var currentIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_list)

        musicList = ArrayList()
        musicRecyclerView = findViewById(R.id.music_list)
        musicRecyclerView.layoutManager = LinearLayoutManager(this)
        musicAdapter = MusicAdapter(this, musicList, this)
        musicRecyclerView.adapter = musicAdapter

        if (isPermissionGranted()) {
            loadMusicFiles()
        } else {
            requestPermission()
        }
    }



    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
            REQUEST_CODE_STORAGE_PERMISSION
        )
    }

    private fun loadMusicFiles() {
        val musicResolver = contentResolver
        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicCursor = musicResolver.query(
            musicUri,
            null,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.IS_ALARM} == 0 AND ${MediaStore.Audio.Media.IS_RINGTONE} == 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        if (musicCursor != null && musicCursor.moveToFirst()) {
            val idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

            do {
                val id = musicCursor.getString(idColumn)
                val title = musicCursor.getString(titleColumn)
                val artist = musicCursor.getString(artistColumn)
                val duration = musicCursor.getLong(durationColumn)

                val musicUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toLong()
                )

                val music = Music(id, title, artist, duration, musicUri)
                musicList.add(music)
            } while (musicCursor.moveToNext())

            musicCursor.close()
        }

        musicAdapter.notifyDataSetChanged()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicFiles()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission required to display music files.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        const val REQUEST_CODE_STORAGE_PERMISSION = 100
    }

    override fun onItemClick(music: Music) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("title", music.title)
        intent.putExtra("artist", music.artist)
        intent.putExtra("duration", music.duration)
        intent.putExtra("musicUri", music.musicUri.toString())

        val thumbnailBitmap = getMusicThumbnail(music.musicUri)
        if (thumbnailBitmap != null) {
            val thumbnailByteArray = bitmapToByteArray(thumbnailBitmap)
            intent.putExtra("thumbnail", thumbnailByteArray)
        }
        intent.putParcelableArrayListExtra("musicList", ArrayList(musicList))
        startActivity(intent)
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

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

}
