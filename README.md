# mobile-app
アプリ

## 作品名

## 対象者

- 落とし物をした人

## 作品の内容

## 作品の実現方法


### ① ウェアラブル端末

常時録画（ループ録画でも可）

動画をSDカードに保存
タイムスタンプ

### ② スマホアプリ（メイン処理）

システムの中核

役割：

動画取り込み

フレーム抽出

物体認識
Mobilenet SSD
Yolo

特徴量保存（DB）

検索UI

~~### ローカルDB(relativeTime)~~
~~- id~~
~~- movieAdress~~
~~- relativeTimeBase~~

### ローカルDB(keyFrame) sdCard > local
- id : uuid
- keyFramePath : Path
- movieParh : Path


### ローカルDB(cropImage)
- id : uuid
- crop_imagePath : バウンティボックス内の画像。(Path)
- class_name : 物体名 (string)
- score : 信頼値(浮動小数点)
- color : 物体の色(列挙子 : imageColor)
- realTime : timeStamp
- fileTime : timeStamp
- keyFramePath : Path
- movieParh : Path

※ Path = Text

### データの流れ
1. SDカードの認識がされているか  | 外部ストレージの接続状態が取得可能
2. 動画ファイル選択　| 
3. 準備完了したか (前処理)| フレームごとに認識した物体とその色を関連付けて、ローカルデータベースに保存する
4. 条件(色や物体の名前など)で検索　|
5. 合致する画像を表示
6. ユーザーが画像の選択
7. 詳細情報がわかる。

## 独創的な部分
事前の登録なしで忘れ物を探すことが可能

視界に映っている探し物を明確に画像や動画で提示することができる

身近な忘れ物や置き忘れしたものを探すことに適している

air tagなどと比較して忘れ物ごとのハードウェアが必要ないという利点がある

## 開発環境
android studio

android スマホ

ウェアラブル端末(録画データに直接アクセス可能な)
