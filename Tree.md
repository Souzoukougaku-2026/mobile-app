# プロジェクトファイル構成 (Tree.md)

## 1. 全体構造
プロジェクトは **Feature First**（機能単位）のパッケージ構成を採用しており、共通基盤を `core/`、各画面機能を `feature/` 配下に集約しています。

```text
com.example.a2026souzou/
├── RootActivity.kt          # アプリの単一エントリーポイント（NavHostによる画面遷移管理）
├── SouzouApplication.kt      # アプリケーションクラス（Hiltのセットアップ）
│
├── core/                     # アプリ共通の基盤機能
│   ├── data/
│   │   └── database/         # Roomデータベース関連
│   │       ├── dao/          # Data Access Objects (SQL定義)
│   │       ├── entity/       # DBテーブル定義
│   │       ├── AppDatabase.kt # データベース本体
│   │       └── Converters.kt  # 型変換 (UUID等)
│   ├── di/                   # Dagger Hiltによる依存注入設定
│   └── domain/
│       └── model/            # アプリ全体で使う純粋なデータモデル
│
└── feature/                  # 各画面・機能ごとの実装
    ├── home/                 # ホーム画面（ナビゲーションの起点）
    ├── movie/                # ムービー画面（グラフ・画像表示・ズーム）
    │   └── presentation/
    │       └── components/   # Movie画面を構成する部品群
    └── list/                 # 検出リスト画面（検索・絞り込み・ソート）
```

---

## 2. 主要ファイルの役割

### 共通基盤 (`core/`)
- **`AppDatabase.kt`**: データベースの構成定義。
- **`CropImageDao.kt`**: 物体検出データの検索SQL（複数条件フィルタ、ソート、LIMIT等）を記述。
- **`DatabaseModule.kt`**: DBやDAOをアプリ全体で使えるようにする注入設定。

### 画面機能 (`feature/`)
- **`MovieScreen.kt`**: ムービー表示画面のメインレイアウト。
- **`MovieViewModel.kt`**: グラフのシーク位置やズーム状態などのロジックを管理。
- **`ListScreen.kt`**: 検索・フィルタリングUIと検出結果リストの表示。
- **`ListViewModel.kt`**: 検索条件（クラス、色、ソート順）の保持とDBへの反映。

### ナビゲーション
- **`RootActivity.kt`**: `AppNavigation` を定義し、"home", "movie", "list" の画面遷移ルートを管理。

---

## 3. 動画部分の差し込み場所
動画（VideoPlayer等）の実装や差し込みを行うファイルは以下です。

- **ファイル名**: `feature/movie/presentation/components/VideoPlaceholder.kt`
- **現在の役割**: 16:9のアスペクト比を保った黒いボックス（プレースホルダ）です。
- **差し込み方法**: このファイル内の `Box` を、実際の `ExoPlayer` や `AndroidView` を使用した動画再生コンポーネントに置き換えることで、動画表示が組み込まれます。
