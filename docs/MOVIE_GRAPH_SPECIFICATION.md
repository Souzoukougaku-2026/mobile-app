# MovieDetail（MovieScreen）グラフ統合およびフィルター機能 仕様書

## 1. 概要 (Overview)
本仕様書は、MovieDetail（`MovieScreen`）画面内に存在していた2つのグラフ（全体グラフ・詳細グラフ）を**単一の統合グラフ（Unified Graph Area）**に一元化し、画面横幅の表示時間スケール（1日 / 6時間 / 1.5時間）および `CropImageDB` データに対する色（Color）とクラス（Class）のフィルタリング機能を実装するための詳細設計および開発手順を定義します。

---

## 2. 要件仕様 (Requirements)

### 2.1 グラフの一元化
- 従来の「全体グラフ (Overall Graph)」と「詳細グラフ (Detailed Graph)」の2画面表示を廃止し、**単一の統合グラフ領域**を配置する。
- 統合グラフはシーク操作（タップ/ドラッグ）および現在再生位置インジケーター表示に対応する。

### 2.2 表示時間スケール (Time Scale) と集計バケット (Binning Interval)
画面横幅に表示する時間のスパンを以下の3種類から切り替え可能とする。

| 表示時間スケール | 横幅時間スパン | バーの集計間隔 (1本あたり) | 1画面あたりのバーの本数 |
| :--- | :--- | :--- | :--- |
| **1日 Mode** | 24時間 (1,440分 = 86,400秒) | **16分** | 90本 |
| **6時間 Mode** | 6時間 (360分 = 21,600秒) | **4分** | 90本 |
| **1.5時間 Mode** | 1.5時間 (90分 = 5,400秒) | **1分** | 90本 |

- 各バーの高さは、該当時間バケット内に存在する `CropImageDB` (`CropImageEntity`) の件数（データ数）を表す。

### 2.3 フィルタリング機能 (Filter Options)
`CropImageDB` 内のデータを以下の2つの条件で動的に絞り込み、グラフのバー件数を自動更新する。
1. **クラス (Class / className)**:
   - 「すべて (All)」または DB内に存在する全クラス名（例: `wallet`, `headphone`, `key`, `smart phone`, `umbrella` 等）
2. **色 (Color)**:
   - 「すべて (All)」または `ImageColor`（例: RED, BLUE, GREEN, YELLOW, BLACK, WHITE 等）

---

## 3. アーキテクチャとデータ構造 (Data Architecture)

### 3.1 TimeScale Enum
```kotlin
enum class TimeScale(
    val label: String,
    val totalSeconds: Float,
    val intervalMinutes: Int
) {
    ONE_DAY("1日", 86400f, 16),
    SIX_HOURS("6時間", 21600f, 4),
    ONE_HALF_HOURS("1.5時間", 5400f, 1);

    val barCount: Int
        get() = ((totalSeconds / 60f) / intervalMinutes).toInt()
}
```

### 3.2 UI State (`MovieUiState`)
- `selectedTimeScale`: `TimeScale` (デフォルト: `TimeScale.SIX_HOURS` または `ONE_DAY`)
- `selectedClassFilter`: `String?` (null は「すべて」)
- `selectedColorFilter`: `ImageColor?` (null は「すべて」)
- `availableClasses`: `List<String>`
- `availableColors`: `List<ImageColor>`
- `graphBarCounts`: `List<Int>` (各バーの件数データ)
- `maxBarCount`: `Int` (高さ正規化用)

### 3.3 データ集計ロジック (ViewModel)
1. `CropImage` の全件リストを取得。
2. `selectedClassFilter` および `selectedColorFilter` に基づいてリストをフィルタリング。
3. 基準時刻（例: 全動画の最小 `realTime` または表示時間軸）から各 `CropImage.realTime` の経過時間を算出。
4. `TimeScale` の集計間隔（16分 / 4分 / 1分）でバケットインデックスを算出し、バー件数配列 (`graphBarCounts`) に格納。

---

## 4. 段階的実装・Gitコミット計画 (Step-by-step Execution Plan)

- **[Commit 1] ドキュメント追加**:
  - 本仕様書 (`docs/MOVIE_GRAPH_SPECIFICATION.md`) の作成と初期コミット。
- **[Commit 2] データ・ドメイン層の拡張**:
  - `TimeScale` enum の定義。
  - `MovieUiState` への時間スケール・フィルター状態の追加。
- **[Commit 3] ViewModel の集計・フィルタリングロジック実装**:
  - フィルター条件適用および時間バケット集計関数の実装。
  - 時間スケール・フィルター変更用イベントハンドラーの実装。
- **[Commit 4] UI層の実装 (統合グラフとフィルターコントロール)**:
  - 2つの旧グラフを撤去。
  - 単一統合グラフ (`UnifiedGraphArea`) の新規実装。
  - 表示時間スケール切替タブおよびクラス・色フィルターUIの配置。
- **[Commit 5] ビルド・動作検証**:
  - Gradle ビルドを実行し、全機能のコンパイルと動作を確認。
