# DayBudget（Android版）

「今日あといくら使えるか」だけを見るための家計アプリの Android ネイティブ版です。
Kotlin + Jetpack Compose でゼロから作っています（Web版を包んだものではありません）。

## 機能

- 初回設定（月の予算・締め日。月度の途中から始めた場合は最初の月度だけ予算を按分）
- ダッシュボード（今日使えるお金、状態の色、明日の見込み、月度の歩み、今日の支出）
- 支出の記録・編集・削除（テンキー、6カテゴリ、メモ、日付。削除は「元に戻す」可能）
- 履歴カレンダー（日ごとの予算内/超過、過去の月度、日付を指定して追加）
- 設定（予算、締め日、繰越モード、テーマ、ウィジェットの着せ替え、CSV書き出し、全消去）
- ホーム画面ウィジェット（小・中・大、0時に自動で翌日に切り替え）
  - 小: 今日使える額と「＋」、中: 「よく使う金額」を1タップで記録（アプリを開かない）、大: 詳細表示（Pro）
  - 着せ替え5種（ダーク無料、ホワイト/ネオン/サクラ/ミントは Pro）
- よく使う金額（最大6個。ホームのチップとウィジェットから1タップで記録）
- 朝のお知らせ（今日使える額と昨日との差）・夜のリマインド（記録忘れ・明日の見込み）。時刻は設定で変更
- 大きな出費の予約（その日まで毎日の予算から取り分け、当日の割当に上乗せ）
- 予算内の連続日数
- 月末の振り返りカード（節約額・予算内の日数・最長連続・よく使ったカテゴリ。金額を隠してSNSにシェア）
- バックアップの書き出し・復元（JSON。保存先はユーザーが選ぶ）
- 演出: 節約ボーナスのコインが数字へ飛び込む、記録した金額が数字へ飛ぶ、桁ごとに入れ替わる数字、連続日数の節目（3/7/14/30/60/100日）で紙吹雪、状態の色を上部に敷く（端末の「アニメーションを削除」がオンなら省略）
- 一覧のスワイプ（左で削除、右でもう一度記録）
- アイコン長押しのショートカット（支出を記録・履歴）とクイック設定のタイル
- Pro版の画面（購入処理は未実装。デバッグビルドでは設定画面から Pro を切り替えられます）

## 技術構成

| 項目 | 内容 |
|---|---|
| 言語・UI | Kotlin 2.2 / Jetpack Compose（Material 3） |
| 保存 | Room（SQLite、v2: quick_presets・planned_expenses を追加）。ネットワーク権限なし・端末の外に送信しない |
| ウィジェット | Jetpack Glance |
| 対応OS | Android 8.0（API 26）以上、targetSdk 36 |

```
app/src/main/java/com/daybudget/app/
├── domain/   月度判定・日割り計算・CSV（Android に依存しない純粋な Kotlin）
├── data/     Room（テーブル: user_settings / expenses）とリポジトリ
├── ui/       画面（screens/）、部品（components/）、テーマ、アイコン、ViewModel
├── notify/   朝・夜のお知らせと、再起動後の予約し直し
└── widget/   ホーム画面ウィジェット（1タップ記録を含む）と0時の更新
```

## ビルド

JDK 17 以上と Android SDK（`local.properties` の `sdk.dir` か `ANDROID_HOME`）が必要です。

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 計算ロジック・ウィジェット・画面操作のテスト
./gradlew recordRoborazziDebug   # 画面のスクリーンショット（app/build/outputs/roborazzi/）
./gradlew lintDebug
```

リリース版に署名するときは、`keystore.properties`（リポジトリには含めない）を置いてから `./gradlew assembleRelease` を実行します。

```properties
storeFile=release.jks
storePassword=...
keyAlias=daybudget
keyPassword=...
```

Maven Central が混雑していて Robolectric の Android ランタイムを取得できない場合は、jar を手元のフォルダに置いて `-ProbolectricDepsDir=/path/to/dir` を付けて実行できます。
