package com.example.mikke.ui.theme

import androidx.compose.ui.graphics.Color

// アプリのアイコンの色味に合わせた、青ベースのトーンパレット。
// Material 3 の考え方に沿って、色相ごとに明度 (トーン) の段階を用意し、
// Theme.kt のライト/ダーク各スキームがここから色を選ぶ。
// - Blue: アイコンの虫眼鏡などの青 (#1665DA 付近) を基準にしたメインの色相
// - BlueGrey: Blue を落ち着かせた補助色 (チップやコンテナ向け)
// - Yellow: アイコンの鍵の黄色 (#FED61D 付近) を基準にしたアクセント
// - Neutral / NeutralVariant: Blue をわずかに含ませたグレー (背景や枠線向け)

// プライマリ (青)
val Blue20 = Color(0xFF082D5E)
val Blue30 = Color(0xFF0C448D)
val Blue40 = Color(0xFF105BBC)
val Blue80 = Color(0xFFA1C6F7)
val Blue90 = Color(0xFFD0E3FB)
val Blue100 = Color(0xFFFFFFFF)

// セカンダリ (青グレー)
val BlueGrey20 = Color(0xFF28323E)
val BlueGrey30 = Color(0xFF3C4A5D)
val BlueGrey40 = Color(0xFF50637C)
val BlueGrey80 = Color(0xFFC1CBD7)
val BlueGrey90 = Color(0xFFE0E5EB)
val BlueGrey100 = Color(0xFFFFFFFF)

// ターシャリ (黄色)
val Yellow20 = Color(0xFF5C4B0A)
val Yellow30 = Color(0xFF8A710F)
val Yellow40 = Color(0xFFB89714)
val Yellow80 = Color(0xFFF5E4A3)
val Yellow90 = Color(0xFFFAF2D1)
val Yellow100 = Color(0xFFFFFFFF)

// ニュートラル (背景・サーフェス)
val Neutral4 = Color(0xFF0A0A0B)
val Neutral6 = Color(0xFF0E0F10)
val Neutral10 = Color(0xFF18191B)
val Neutral12 = Color(0xFF1D1E20)
val Neutral17 = Color(0xFF292B2E)
val Neutral20 = Color(0xFF303336)
val Neutral22 = Color(0xFF35383B)
val Neutral24 = Color(0xFF3A3D41)
val Neutral87 = Color(0xFFDCDEE0)
val Neutral90 = Color(0xFFE4E5E7)
val Neutral92 = Color(0xFFE9EAEC)
val Neutral94 = Color(0xFFEFF0F1)
val Neutral95 = Color(0xFFF1F2F3)
val Neutral96 = Color(0xFFF4F5F5)
val Neutral98 = Color(0xFFFAFAFA)
val Neutral100 = Color(0xFFFFFFFF)

// ニュートラルバリアント (サーフェスバリアント・枠線)
val NeutralVariant30 = Color(0xFF454B54)
val NeutralVariant50 = Color(0xFF737E8C)
val NeutralVariant60 = Color(0xFF8F98A3)
val NeutralVariant80 = Color(0xFFC7CBD1)
val NeutralVariant90 = Color(0xFFE3E5E8)

// エラー (Material 3 の標準色をそのまま使う)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF93000A)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
