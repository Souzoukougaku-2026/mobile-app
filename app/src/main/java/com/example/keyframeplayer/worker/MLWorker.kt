package com.example.keyframeplayer.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
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
import com.example.keyframeplayer.core.domain.model.ImageColor
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

    fun detectColor(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int): ImageColor{
        val cropped = Bitmap.createBitmap(
            bitmap,
            left,
            top,
            right - left,
            bottom - top
        )
        return cropped.detectDominantColor()
    }

    override suspend fun doWork(): Result {
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            Log.w(TAG, "ForegroundService の起動はスキップされました（通常のバックグラウンドワーカーとして実行します）: ${e.message}")
        }

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
            updateProgressNotification(0, 0, 100)
            return Result.success()
        }

        val detector = ObjectDetector(context)

        try {
            for ((index, keyFrame) in keyFrames.withIndex()) {
                val bitmap = BitmapFactory.decodeFile(keyFrame.keyFramePath)
                if (bitmap != null) {
                    try {
                        val detections = detector.runDetect(bitmap)

                        val cropImageEntities = detections.map { detection ->
                            CropImageEntity(
                                id = UUID.randomUUID(),
                                className = detection.classname,
                                score = detection.score.toDouble(),
                                color = detectColor(
                                    bitmap,
                                    detection.bbox[0].toInt(),
                                    detection.bbox[1].toInt(),
                                    detection.bbox[2].toInt(),
                                    detection.bbox[3].toInt()
                                ).id,
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

                val current = index + 1
                val progress = current * 100 / total
                Log.i(TAG, "[画像解析・物体検出] キーフレーム処理中: $current/$total 枚 ($progress%)")
                updateProgressNotification(current, total, progress)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "落とし物解析に失敗しました", e)
            return Result.failure()
        } finally {
            detector.close()
        }
    }

    private suspend fun updateProgressNotification(current: Int, total: Int, progress: Int) {
        setProgress(
            workDataOf(
                "progress" to progress,
                "current" to current,
                "total" to total
            )
        )

        try {
            val notification = notificationBuilder
                .setProgress(100, progress, false)
                .setContentText(if (total > 0) "$current/$total 枚 完了 ($progress%)" else "$progress% 完了")
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "通知の更新に失敗しました: ${e.message}")
        }
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
