package com.example.a2026souzou.core.domain.model

/**
 * 取得された一連の検出結果（キーフレームと物体リスト）をまとめるデータモデル。
 * 外部（AIエンジン等）から取得したデータをそのまま受け渡すために使用します。
 */
data class DetectionResult(
    val keyFrame: KeyFrame,
    val cropImages: List<CropImage>
)
