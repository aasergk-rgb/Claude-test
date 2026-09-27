# DayBudget（Android版）

「今日あといくら使えるか」だけを見るための家計アプリの Android ネイティブ版です。
Kotlin + Jetpack Compose でゼロから作っています（Web版を包んだものではありません）。

## 機能

- 初回設定（月の予算・締め日。月度の途中から始めた場合は最初の月度だけ予算を按分）
- ダッシュボード（今日使えるお金、状態の色、明日の見込み、月度の歩み、今日の支出）
- 支出の記録・編集・削除（テンキー、6カテゴリ、メモ、日付。削除は「元に戻す」可能）
- 履歴カレンダー（日ごとの予算内/超過、過去の月度、日付を指定して追加）
- 設定（予算、締め日、繰越モード、テーマ、ウィジェットの着せ替え、CSV書き出し、全消去）
- ホーム画面ウィジェット（小・中サイズ、ダーク/ホワイト/ネオン、0時に自動で翌日に切り替え）
- Pro版の画面（購入処理は未実装。デバッグビルドでは設定画面から Pro を切り替えられます）

## 技術構成

| 項目 | 内容 |
|---|---|
| 言語・UI | Kotlin 2.2 / Jetpack Compose（Material 3） |
| 保存 | Room（SQLite）。ネットワーク権限なし・端末の外に送信しない |
| ウィジェット | Jetpack Glance |
| 対応OS | Android 8.0（API 26）以上、targetSdk 36 |

```
app/src/main/java/com/daybudget/app/
├── domain/   月度判定・日割り計算・CSV（Android に依存しない純粋な Kotlin）
├── data/     Room（テーブル: user_settings / expenses）とリポジトリ
├── ui/       画面（screens/）、部品（components/）、テーマ、アイコン、ViewModel
└── widget/   ホーム画面ウィジェットと0時の更新
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
