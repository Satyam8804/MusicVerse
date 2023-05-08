import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicverse.PlayerActivity
import com.example.musicverse.R
import java.util.*
import java.util.concurrent.TimeUnit

class MusicAdapter(
    private val context: Context,
    private val musicList: List<Music>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<MusicAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.single_row_music, parent, false)
        return ViewHolder(view)
    }

    // Inside the onBindViewHolder method, update the click listener

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val musicFile = musicList[position]
        holder.bind(musicFile)
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(musicFile)
        }
    }


    override fun getItemCount(): Int {
        return musicList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val musicNameTextView: TextView = itemView.findViewById(R.id.music_name)
        private val musicDurationTextView: TextView = itemView.findViewById(R.id.music_duration)
        private val musicThumbnailImageView: ImageView = itemView.findViewById(R.id.music_thumbnail)

        fun bind(musicFile: Music) {
            musicNameTextView.text = musicFile.title
            musicDurationTextView.text = getDurationString(musicFile.duration)
            musicThumbnailImageView.setImageBitmap(getMusicThumbnail(musicFile.musicUri))
        }
    }

    private fun getDurationString(duration: Long?): String {
        if (duration == null) return ""
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun getMusicThumbnail(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val rawArt = retriever.embeddedPicture
        retriever.release()

        return if (rawArt != null) {
            BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size)
        } else {
            null
        }
    }

    interface OnItemClickListener {
        fun onItemClick(music: Music)
    }
}
