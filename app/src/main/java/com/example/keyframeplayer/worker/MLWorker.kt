package com.example.wasuremono_prj.joint

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.keyframeplayer.R
import com.example.keyframeplayer.data.AppDatabase
import com.example.keyframeplayer.detector.ObjectDetector
import com.example.keyframeplayer.detector.detectDominantColor
import kotlinx.coroutines.delay
import com.example.keyframeplayer.data.ClopImageEntity
import com.example.keyframeplayer.data.BPoint

const val TAG = "MLWorker"

class MLWorker(
    private val context: Context, parameters: WorkerParameters
) :
    CoroutineWorker(context, parameters) {
    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java) as NotificationManager
    }

    private val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("落とし物解析を実行中")
        .setContentText(null)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setProgress(100, 0, false)

    val database = AppDatabase.getDatabase(context)
    val keyFrameDao = database.keyFrameDao()
    val clopImageDao = database.clopImageDao()

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())

        val ids = keyFrameDao.getKeyFrameIds()
        val total = ids.size

        if (total == 0) {
            updateProgressNotification(100)
            return Result.success()
        }

        val detector = ObjectDetector(context)

        try {
            for ((index, id) in ids.withIndex()) {
                val entity = keyFrameDao.getKeyFrameById(id)

                if (entity != null) {
                    val bitmap = BitmapFactory.decodeFile(entity.keyFramePath)
                        ?: error("画像を読み込めません: ${entity.keyFramePath}")

                    try {
                        val detections = detector.runDetect(bitmap)
                        val dominantColor = bitmap.detectDominantColor()

                        clopImageDao.insertClopImage(
                            clopImage = detections.map { detection ->
                                ClopImageEntity(
                                    classname = detection.classname,
                                    score = detection.score,
                                    color = dominantColor,
                                    bboxPoint = BPoint(
                                        ulX = detection.bbox[0],
                                        ulY = detection.bbox[1],
                                        lrX = detection.bbox[2],
                                        lrY = detection.bbox[3]
                                    ),
                                    idKeyFrame = id
                                )
                            }
                        )
                    } finally {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                }

                // このIDの処理が終わった時点で進捗を更新
                val progress = (index + 1) * 100 / total
                updateProgressNotification(progress)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "落とし物解析に失敗しました", e)
            return Result.failure()
        } finally {
            detector.close()
        }
    }

    private suspend fun updateProgressNotification(progress: Int) {
        // WorkManagerから進捗を監視するためのデータ
        setProgress(workDataOf("progress" to progress))

        // 通知に表示する進捗バーと本文
        val notification = notificationBuilder
            .setProgress(100, progress, false)
            .setContentText("$progress% 完了")
            .build()

        // 同じ通知IDを使うことで、表示中の通知を更新する
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        createChannel(context)

        val notification = notificationBuilder.build()

        val foregroundServiceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            foregroundServiceType
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "物体検出の完了通知",
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ml_worker"
        private const val NOTIFICATION_ID = 1001
    }

    // 動作確認用の関数
    private suspend fun sampleWork() {
        val minutes = 35;
        repeat(minutes) { min ->
            delay(60_000L)

            setProgress(
                workDataOf(
                    "progress" to ((min + 1) * 100 / minutes)
                )
            )

            val updatedNotification = notificationBuilder
                .setProgress(100, ((min + 1) * 100 / minutes), false)
                .build()

            notificationManager.notify(NOTIFICATION_ID, updatedNotification)
        }
    }
}
