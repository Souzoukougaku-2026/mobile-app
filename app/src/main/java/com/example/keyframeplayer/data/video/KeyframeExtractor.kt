package com.example.keyframeplayer.data.video


import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.keyframeplayer.KeyframeItem

class KeyframeExtractor {

    fun getKeyframeTimes(context: Context, uri: Uri): List<Long> {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        var videoTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                videoTrackIndex = i
                break
            }
        }
        if (videoTrackIndex == -1) return emptyList()

        extractor.selectTrack(videoTrackIndex)

        val times = mutableListOf<Long>()
        while (true) {
            val t = extractor.sampleTime
            if (t < 0) break
            if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                times.add(t)
            }
            extractor.advance()
        }

        extractor.release()
        return times
    }

    fun getKeyframeItems(
        context: Context,
        uri: Uri,
        timesUs: List<Long>,
        limit: Int = 10
    ): List<KeyframeItem> {

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)

        val items = mutableListOf<KeyframeItem>()

        for (timeUs in timesUs.take(limit)) {
            retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )?.let {
                items.add(KeyframeItem(timeUs, it))
            }
        }

        retriever.release()
        return items
    }
}