package com.example.a2026souzou.core.util

import com.example.a2026souzou.core.domain.model.*
import java.util.UUID

/**
 * 実際の取得（Acquisition）をシミュレートするユーティリティ。
 * 特定のシナリオに基づいたデータを「取得」し、ViewModelへ渡すための準備をします。
 */
object DetectionSimulator {
    fun acquireWalletDetection(): DetectionResult {
        val keyFrameId = UUID.randomUUID()
        val now = System.currentTimeMillis()
        
        val keyFrame = KeyFrame(
            id = keyFrameId,
            realTime = now,
            fileTime = 1500, // 動画内1.5秒地点
            keyFramePath = "/data/user/0/com.example.a2026souzou/files/frames/wallet_$keyFrameId.jpg",
            moviePath = "/data/user/0/com.example.a2026souzou/files/movies/scenario_01.mp4"
        )
        
        val cropImage = CropImage(
            id = UUID.randomUUID(),
            className = CropImage.CLASS_WALLET,
            score = 0.98,
            color = ImageColor.Brown,
            bboxLeft = 15,
            bboxTop = 20,
            bboxRight = 45,
            bboxBottom = 55,
            realTime = now,
            idKeyFrame = keyFrameId
        )
        
        return DetectionResult(keyFrame, listOf(cropImage))
    }

    fun acquireSmartphoneDetection(): DetectionResult {
        val keyFrameId = UUID.randomUUID()
        val now = System.currentTimeMillis()
        
        val keyFrame = KeyFrame(
            id = keyFrameId,
            realTime = now,
            fileTime = 3000,
            keyFramePath = "/data/user/0/com.example.a2026souzou/files/frames/phone_$keyFrameId.jpg",
            moviePath = "/data/user/0/com.example.a2026souzou/files/movies/scenario_02.mp4"
        )
        
        val cropImage = CropImage(
            id = UUID.randomUUID(),
            className = CropImage.CLASS_SMART_PHONE,
            score = 0.92,
            color = ImageColor.Black,
            bboxLeft = 60,
            bboxTop = 10,
            bboxRight = 90,
            bboxBottom = 40,
            realTime = now,
            idKeyFrame = keyFrameId
        )
        
        return DetectionResult(keyFrame, listOf(cropImage))
    }
}
