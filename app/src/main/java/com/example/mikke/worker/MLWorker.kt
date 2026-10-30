package com.example.mikke.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mikke.R
import com.example.mikke.core.data.database.dao.CropImageDao
import com.example.mikke.core.data.database.dao.KeyFrameDao
import com.example.mikke.core.data.database.entity.CropImageEntity
import com.example.mikke.core.domain.model.ImageColor
import com.example.mikke.detector.ObjectDetector
import com.example.mikke.detector.detectDominantColor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.UUID

const val TAG = "MLWorker"

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MLWorkerEntryPoint {
    fun keyFrameDao(): KeyFrameDao
    fun cropImageDao(): CropImageDao
}

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
        // 通知のスモールアイコンはシルエット専用アイコンを使う (ランチャー用の
        // フルカラー画像はアルファ形状だけが描画されて潰れてしまうため)
        .setSmallIcon(R.drawable.ic_stat_search)
        .setOngoing(true)
        .setProgress(100, 0, false)

    private fun convertBbox(
        bitmap: Bitmap,
        bbox: FloatArray
    ): IntArray {
        val width: Int = bitmap.width
        val height: Int = bitmap.height

        return intArrayOf(
            (width * bbox[0]).toInt(),
            (height * bbox[1]).toInt(),
            (width * bbox[2]).toInt(),
            (height * bbox[3]).toInt()
        )
    }

    private fun detectColor(
        bitmap: Bitmap,
        bbox: IntArray
    ): ImageColor {
        val safeLeft = bbox[0].coerceIn(0, bitmap.width)
        val safeTop = bbox[1].coerceIn(0, bitmap.height)
        val safeRight = bbox[2].coerceIn(0, bitmap.width)
        val safeBottom = bbox[3].coerceIn(0, bitmap.height)

        require(safeRight > safeLeft && safeBottom > safeTop) {
            "検出範囲が画像の範囲外、または無効です"
        }

        val cropped = Bitmap.createBitmap(
            bitmap,
            safeLeft,
            safeTop,
            safeRight - safeLeft,
            safeBottom - safeTop
        )

        return try {
            cropped.detectDominantColor()
        } finally {
            // 解放するのは切り抜き画像だけにする。元の bitmap は参照で渡されており、
            // 呼び出し側 (doWork) が残りの検出でも使うため、ここで解放してはいけない
            // (本体の解放は doWork の finally で1回だけ行う)。
            // 切り抜き範囲が画像全体と一致した場合は createBitmap が元の bitmap 自身を
            // 返すため、その場合は解放しない。
            if (cropped !== bitmap && !cropped.isRecycled) {
                cropped.recycle()
            }
        }
    }


    override suspend fun doWork(): Result {
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            Log.w(TAG, "ForegroundService の起動はスキップされました（通常のバックグラウンドワーカーとして実行します）: ${e.message}")
        }

        // Hilt Singleton コンポーネントから共通の DB / DAO インスタンスを取得
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MLWorkerEntryPoint::class.java
        )
        val keyFrameDao = entryPoint.keyFrameDao()
        val cropImageDao = entryPoint.cropImageDao()

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
                            val bbox = convertBbox(bitmap, detection.bbox)

                            CropImageEntity(
                                id = UUID.randomUUID(),
                                className = detection.classname,
                                score = detection.score.toDouble(),
                                color = detectColor(bitmap, bbox).id,
                                bboxLeft = bbox[0],
                                bboxTop = bbox[1],
                                bboxRight = bbox[2],
                                bboxBottom = bbox[3],
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
