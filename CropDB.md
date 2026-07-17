# CropImage データベース設計・実装仕様 (CropDB.md)

## 1. 概要
本システムは、大量の物体検出データ（数十万〜数百万件）に対して高速な検索とソートを実現するために設計された SQLite (Room) データベースを使用します。

---

## 2. テーブル構成

### `CropImage` (検出物体情報)
検出された物体ごとの情報を保持します。検索性能を最大化するため、時間情報を非正規化して保持しています。

| カラム名 | 型 | 説明 |
| :--- | :--- | :--- |
| `id` | UUID (PK) | 主キー |
| `class_name` | TEXT | 物体クラス名 (wallet, headphone, key, smart phone, umbrella) |
| `score` | REAL | AIによる信頼値 |
| `color` | INTEGER | 物体色（0: White 〜 11: Brown） |
| `bbox_left` | INTEGER | バウンディングボックス左上X座標 |
| `bbox_top` | INTEGER | バウンディングボックス左上Y座標 |
| `bbox_right` | INTEGER | バウンディングボックス右下X座標 |
| `bbox_bottom` | INTEGER | バウンディングボックス右下Y座標 |
| `realTime` | INTEGER | 現実世界の撮影時刻（Unixタイムスタンプ・ミリ秒） |
| `id_keyFrame` | UUID (FK) | `KeyFrame` テーブルへの外部キー |

### `KeyFrame` (動画・キーフレーム情報)
動画ファイルや抽出された静止画（キーフレーム）のメタデータを保持します。

| カラム名 | 型 | 説明 |
| :--- | :--- | :--- |
| `id` | UUID (PK) | 主キー |
| `realTime` | INTEGER | 現実世界の撮影時刻 |
| `fileTime` | INTEGER | 動画ファイル内での相対時刻 |
| `keyFramePath` | TEXT | キーフレーム画像の保存パス |
| `moviePath` | TEXT | 元動画ファイルの保存パス |

---

## 3. インデックス設計 (検索高速化)
大量データの中から必要な情報を瞬時に抽出するため、以下の複合インデックスを設定しています。

| インデックス定義 | 主な用途 |
| :--- | :--- |
| `(class_name, color, realTime)` | クラス・色の指定検索 + 時間順ソート |
| `(color, realTime)` | 色のみの指定検索 + 時間順ソート |
| `(realTime)` | 時間範囲のみの検索 + 時間順ソート |

---

## 4. 検索・操作機能 (DAO)

### 絞り込み検索 (`CropImageDao`)
- **動的フィルタ**: クラス名、色、時間範囲（開始・終了）を任意に組み合わせて検索可能。
- **柔軟なソート**: `realTime` カラムに対して、昇順 (ASC) と 降順 (DESC) を切り替え可能。
- **ページング制御**: `LIMIT` 句を使用して1回あたりの取得件数を制限（デフォルト20件）。

### データ操作
- **一括挿入**: `insertCropData(KeyFrame, List<CropImage>)` メソッドを通じて、親となる `KeyFrame` とそれに紐づく複数の `CropImage` を一括で登録します。
- **データ登録の順序**: 内部的に `ForeignKey` 制約を考慮し、まず `KeyFrame` を登録した後に `CropImage` 群を登録します。
- **整合性維持**: `ForeignKey` の `CASCADE` 削除により、親（KeyFrame）が消えると、それに紐づく子（CropImage）も自動でSQLiteによって削除されます。

---

## 5. UI操作・連携仕様 (`ListViewModel`)
別の開発者がリストUIを実装したり、データの登録・更新を行う際に使用する ViewModel 公開インターフェースの概要です。

### 取得可能な状態 (`ListUiState`)
UI側で `viewModel.uiState.collectAsStateWithLifecycle()` を用いて監視します。

| プロパティ名 | 型 | 説明 |
| :--- | :--- | :--- |
| `items` | `List<CropImage>` | 検索・絞り込み結果の物体リスト。 |
| `selectedClasses` | `Set<String>` | 現在選択中の物体名（チップの点灯管理用）。 |
| `selectedColors` | `Set<Int>` | 現在選択中の色ID（チップの点灯管理用）。 |
| `isAscending` | `Boolean` | ソート順（true: 昇順, false: 降順）。 |
| `isLoading` | `Boolean` | データベース検索中のローディング状態。 |

### 公開操作関数
UIイベント（クリック等）から呼び出します。呼び出し後、自動的に状態が更新されます。

#### 検索・フィルタリング
- **`toggleClass(className: String)`**: 物体名フィルタの有効/無効を切り替えます（単一選択）。
- **`toggleColor(color: Int)`**: 色フィルタの有効/無効を切り替えます（単一選択）。
- **`setSortOrder(isAsc: Boolean)`**: `realTime` による並び順を変更します。

#### データ登録・取得のシミュレーション
- **`addDetectionResult(DetectionResult)`**: 
    1. 外部ソース（AIエンジンやシミュレーター等）から `DetectionResult`（`KeyFrame` と `CropImage` のペア）を受け取ります。
    2. 受け取ったデータを `ListRepository.insertCropData` へ渡し、データベースへ保存します。
    3. UI（`ListScreen`）では `DetectionSimulator` を使用して、特定の物体が検出されたシナリオを「取得」して登録する動作をデバッグできます。

---

## 6. 設計方針の要点
1. **検索優先**: 検索で頻繁に使用される `realTime` を `CropImage` テーブルに持たせることで、テーブル結合 (JOIN) なしで高速なフィルタリングを可能にしました。
2. **リアクティブ**: 検索結果を `Flow` で返却することで、データベースの内容が更新されると自動的にUIへ反映される仕組みを構築しています。
3. **拡張性**: Room のバージョン管理 (`version = 2`) と `fallbackToDestructiveMigration` により、開発中のスキーマ変更に柔軟に対応しています。
