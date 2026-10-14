package com.example.keyframeplayer.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.keyframeplayer.R
import com.example.keyframeplayer.core.data.database.AppDatabase
import com.example.keyframeplayer.core.data.database.entity.CropImageEntity
import com.example.keyframeplayer.detector.ObjectDetector
import com.example.keyframeplayer.detector.detectDominantColor
import java.util.UUID

const val TAG = "MLWorker"

class MLWorker(
    private val context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java) as NotificationManager
    }

    private val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("落とし物解析を実行中")
        .setContentText(null)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setProgress(100, 0, false)

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())

        // データベースの取得
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "crop_image_db"
        ).fallbackToDestructiveMigration().build()

        val keyFrameDao = db.keyFrameDao()
        val cropImageDao = db.cropImageDao()

        val keyFrames = keyFrameDao.getAllKeyFrames()
        val total = keyFrames.size

        if (total == 0) {
            updateProgressNotification(100)
            return Result.success()
        }

        val detector = ObjectDetector(context)

        try {
            for ((index, keyFrame) in keyFrames.withIndex()) {
                val bitmap = BitmapFactory.decodeFile(keyFrame.keyFramePath)
                if (bitmap != null) {
                    try {
                        val dominantColor = bitmap.detectDominantColor()
                        val detections = detector.runDetect(bitmap)

                        val cropImageEntities = detections.map { detection ->
                            CropImageEntity(
                                id = UUID.randomUUID(),
                                className = detection.classname,
                                score = detection.score.toDouble(),
                                color = dominantColor.id,
                                bboxLeft = detection.bbox[0].toInt(),
                                bboxTop = detection.bbox[1].toInt(),
                                bboxRight = detection.bbox[2].toInt(),
                                bboxBottom = detection.bbox[3].toInt(),
                                realTime = keyFrame.realTime,
                                fileTime = keyFrame.fileTime,
                                idKeyFrame = keyFrame.id,
                                keyFramePath = keyFrame.keyFramePath,
                                isLiked = false
                            )
                        }

                        if (cropImageEntities.isNotEmpty()) {
                            cropImageDao.insertCropImages(cropImageEntities)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "キーフレーム解析エラー: ${keyFrame.id}", e)
                    } finally {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                }

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
        setProgress(workDataOf("progress" to progress))

        val notification = notificationBuilder
            .setProgress(100, progress, false)
            .setContentText("$progress% 完了")
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        createChannel(context)

        val notification = notificationBuilder.build()

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ml_worker"
        private const val NOTIFICATION_ID = 1001
    }
}
