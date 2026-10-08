# KeyframePlayer - 動画時系列連動再生システム 機能仕様書

## 1. 概要 (Overview)
本仕様書は、複数動画ファイルを時系列順に統合管理し、リスト表示画面（`ListUpScreen`）から動画詳細・再生画面（`MovieScreen`）への適切な遷移および現実時間（Real Time）軸に基づいた動画のストリーミング連続再生を実現するための設計・実装仕様です。

---

## 2. システム構成・ライブラリ (Architecture & Libraries)

| ライブラリ / テクノロジー | バージョン / パッケージ | 用途・役割 |
| :--- | :--- | :--- |
| **Jetpack Compose UI** | `2024.02.01` (BOM) | 宣言的UI描画・動画再生レイアウト・グラフ表示 |
| **Compose Navigation** | `androidx.navigation:navigation-compose:2.8.5` | 画面間遷移・パラメータ（`cropImageId`, `realTime`）の受け渡し |
| **Hilt & Navigation Compose** | `androidx.hilt:hilt-navigation-compose:1.2.0` | ViewModelへの依存注入と `SavedStateHandle` 連携 |
| **Room Database** | `androidx.room:room-runtime:2.6.1` | 動画メタデータ（`VideoEntity`）、キーフレーム（`KeyFrameEntity`）、クロップ画像（`CropImageEntity`）の永続化 |
| **Media3 ExoPlayer** | `androidx.media3:media3-exoplayer:1.5.1` | 複数動画ファイルのストリーミング連続再生（プレイリスト管理）およびシーク制御 |
| **Media3 UI** | `androidx.media3:media3-ui:1.5.1` | AndroidView 連携による動画描画（`PlayerView`） |
| **Coil Compose** | `io.coil-kt:coil-compose:2.6.0` | 抽出画像の非同期読み込み・表示 |

---

## 3. 実装機能仕様 (Detailed Functional Specifications)

### 【Step 1】 動画の時系列順管理用DB（VideoEntity & VideoDao）の作成
- **目的**: 選択された動画フォルダ内の動画ファイルを現実時間順（時系列）に一元管理する。
- **データ構造 (`VideoEntity`)**:
  - `uri: String` (主キー): 動画ファイルのURI文字列
  - `name: String`: ファイル名
  - `realStartTime: Long`: 動画の現実開始時刻（Epoch ms）
  - `realEndTime: Long`: 動画の現実終了時刻（Epoch ms）
  - `durationMs: Long`: 動画の再生時間（ミリ秒）
- **DAO & Repository**:
  - `VideoDao`: `insertVideos(videos: List<VideoEntity>)`, `getAllVideosOrdered(): Flow<List<VideoEntity>>`（`realStartTime ASC` でソート取得）
  - `ImageRecognitionViewModel`: 動画スキャン時に `VideoEntity` を生成してDBへ保存。
  - `MovieRepository`: 保存済み動画リストの読み出しロジックを提供。

---

### 【Step 2】 画面遷移およびデータ連携 (ListUpScreen → MovieScreen)
- **目的**: リスト画面で特定のクロップ画像（`CropImage`）を選択した際、対象動画・時間位置を保持して動画詳細画面へスムーズに遷移する。
- **Navigation ルート**:
  - ルート名: `"movie?cropImageId={cropImageId}&realTime={realTime}"`
  - パラメータ: `cropImageId` (Optional String), `realTime` (Optional Long)
- **MovieViewModel での処理**:
  - `SavedStateHandle` 経由で `cropImageId` および `realTime` を取得。
  - 選択された `CropImage` / `KeyFrame` の現実時刻およびファイル時間から、全体の時間軸における相対時間（秒）を計算。
  - `MovieUiState.currentTime` および初期再生動画インデックス・ミリ秒位置（`initialMediaItemIndex`, `initialPositionMs`）へ反映。

---

### 【Step 3】 複数動画の1本化ストリーミング連続再生と現実時間軸シークバーの実現
- **目的**: 複数の動画群をユーザーに対してあたかも1本の動画であるかのように連続ストリーミング再生し、現実時間軸を優先したシーク・再生体験を提供する。
- **時系列統合再生アーキテクチャ**:
  - **ExoPlayer の活用**: 単一の `ExoPlayer` インスタンスに対し、時系列順に並べた `MediaItem` リストを `setMediaItems` で一元登録。動画のつなぎ目でプレイヤーの再生成を行わず、シームレスに遷移。
  - **累積タイムライン計算**:
    各動画の累積開始オフセット $R_k = \sum_{i=0}^{k-1} D_i$ および 全体時間 $D_{total} = \sum_{i=0}^{N-1} D_i$ を算出。
  - **相互座標変換**:
    - **[全体時間 (秒) → 動画ファイル位置]**:
      全体の相対時間 $t_{ms}$ から $R_k \le t_{ms} < R_{k+1}$ となる動画インデックス $k$ を特定。
      ファイル内位置 $t_{file\_ms} = t_{ms} - R_k$、現実時間 $T_{real} = V_k.\text{realStartTime} + t_{file\_ms}$。
    - **[ExoPlayer 再生位置 → 全体時間]**:
      現在の動画インデックス $k$ とファイル内位置 $t_{pos\_ms}$ から、全体相対時間 $t_{ms} = R_k + t_{pos\_ms}$。
- **UI表示・操作連動**:
  - シークバー / グラフ: 全体時間軸（$0 \sim T_{total\_sec}$）を表示し、動画ファイル境界を跨いでスムーズにドラッグ・タップシークが可能。
  - 時間ラベル (`TimeLabel` & `BottomInfoArea`): ファイル内経過時間に加え、フォーマットされた現実絶対日時（例: `2025/02/16 10:15:30`）を優先表示。

---

## 4. 進捗・コミット管理方針 (Commit Strategy)
1. **Commit 1 (Step 1)**: `VideoEntity`・`VideoDao` の定義とDBバージョン更新、動画読み込み時の時系列DB保存・Repository拡張
2. **Commit 2 (Step 2)**: Navigation の引数受渡し（`cropImageId`, `realTime`）追加、リストタップからMovie画面へのデータ連携実装
3. **Commit 3 (Step 3)**: 単一ExoPlayerによる複数動画1本化ストリーミング連続再生と現実時間軸シークバー連動の実装
