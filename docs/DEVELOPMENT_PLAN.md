# DayBudget 開発プラン

設計書「DayBudget（デイバジェット）詳細設計書」をもとにした実装計画です。
設計書どおりの技術スタック（Expo / Expo Router / expo-sqlite / Zustand / NativeWind / WidgetKit / RevenueCat / PostHog）で進めます。

> **方針変更（2026-09-27）**
> - まず **Webアプリ** として開発します。iOS版（Expo・WidgetKit・RevenueCat）は、Web版で使い勝手を固めた後の後続フェーズとします。
> - 最初の成果物は **UI/UXだけのデモ**（`demo/index.html`）。機能は実装せず、画面デザインを決めることが目的です。
> - §1 の確認事項 11件は、すべて提案どおりで確定しました。
> - ホーム画面ウィジェットはWebでは作れないため、Web版では「ホーム画面に追加（PWA）」で代替し、ウィジェットはiOS版で提供します。

---

## 0. 前提と進め方

### 0.1 開発環境の分担

| 作業 | 実施場所 |
|---|---|
| TypeScript コード（画面・ロジック・DB・ストア）、Swift ウィジェットコードの作成 | Claude Code（このリポジトリ） |
| 計算ロジック・日付ロジックの単体テスト（Jest） | Claude Code 上で実行可能 |
| iOS ビルド・実機/シミュレータ確認 | **EAS Build（クラウド）** または **Mac + Xcode** |
| App Store Connect / RevenueCat / PostHog のアカウント設定 | オーナー（あなた） |

> ウィジェット・App Groups・課金はネイティブコードを含むため **Expo Go では動作しません**。`expo-dev-client` を使った Development Build で確認します。

### 0.2 オーナー側で事前に必要なもの

- [ ] Apple Developer Program 登録（年額 $99）
- [ ] Bundle ID の確定（例: `com.daybudget.app`）と App Group ID（設計書: `group.com.daybudget.app`）
- [ ] Expo アカウント（EAS Build 用）
- [ ] RevenueCat アカウント + App Store Connect で非消耗型 IAP `daybudget_pro_lifetime`（¥1,000）作成
- [ ] PostHog プロジェクト API キー
- [ ] 利用規約・プライバシーポリシーの公開 URL

---

## 1. 設計書の確認事項（実装前に決めたい点）

実装しながら仕様の穴に当たると手戻りが大きいため、以下は着手前に方針を確定させます。**太字**が本プランでの提案デフォルトです（2026-09-27 に全件この提案で確定）。

| # | 論点 | 内容 | 提案 |
|---|---|---|---|
| 1 | **広告SDKが技術スタックに無い** | 機能マトリクスに「無料版は広告あり」とあるが、SDK未定義。広告を入れると ATT（トラッキング許可）ダイアログ・プライバシーラベル対応が増え、「外部通信なし・安心」というコンセプトとも衝突する | **MVPは広告なしでリリース**、Pro の価値は「ウィジェット全サイズ・着せ替え／貯金プール／CSV」。広告は v1.1 で AdMob（`react-native-google-mobile-ads`）を検討 |
| 2 | 締め日のデフォルトと「月末」 | スキーマのデフォルト `closing_day = 1` だと期間が「2日〜翌1日」になり、ヘッダー例「9/1〜9/30」と矛盾。また 29〜31 日指定時の短い月の扱いが未定義 | **デフォルトは「月末」（=31 として保存）**。月の日数を超える締め日は**その月の末日に丸める** |
| 3 | 「給料日」と「締め日」の混同 | 設定画面は「給料日・締め日」と併記、ロジックは締め日（25日締め → 26日開始）。給料日25日の人は「25日開始」を期待しがち | **ロジックは設計書どおり締め日基準**。オンボーディングでは「毎月何日に区切りますか？（その翌日から新しい月度）」と明示し、プレビューで期間を表示 |
| 4 | ウィジェットの日付またぎ | 共有JSONは「今日」の値だけなので、深夜0時を過ぎてもアプリを開くまで前日の値が表示され続ける | **共有JSONに期間情報・残予算・残日数も含め**、ウィジェット側で 0:00 のタイムラインエントリを生成して翌日分を再計算 |
| 5 | Medium ウィジェットのデータ不足 | 「直近2件の支出」「今月の節約ペース」が共有JSONスキーマに無い | JSON に `recentExpenses`（最大2件）と `savingsPace` を追加（下記 §4.3） |
| 6 | 状態ラベルの閾値が未定義 | 「超順調！」「計画通り」「使いすぎ注意」の判定基準が無い | `progressRate = todaySpent / dailyBudget` で **< 0.5 超順調 / < 0.8 計画通り / ≤ 1.0 使いすぎ注意 / > 1.0 予算オーバー** |
| 7 | テーマ設定が2種類 | `user_settings.theme`（アプリ: system/dark/light）とウィジェットテーマ（ダーク/ホワイト/ネオン）が別物 | `user_settings` に **`widget_theme` カラムを追加** |
| 8 | Pro 判定の正 | `user_settings.is_pro` と RevenueCat の Entitlement が二重管理 | **RevenueCat を正**とし、`is_pro` はオフライン起動用キャッシュ。起動時・復元時に同期 |
| 9 | 履歴カレンダーの緑/赤判定 | 均等配分モードでは日ごとの割当額が変動するため、過去日の「日割り予算」を別途算出する必要がある | 期間開始日から1日ずつシミュレーションして各日の `DailyBudget` を算出（計算ロジックに `simulatePeriod()` を用意） |
| 10 | 貯金プールモードの赤字日 | `SavingsAmount = Σ max(0, …)` は超過日を無視するため、月合計で予算を超えても貯金額がプラス表示になりうる | 設計書どおり実装しつつ、履歴サマリーの「節約額」は **月予算 − 累計支出（実額）** で表示し整合を取る |
| 11 | 予算変更・過去日の支出編集 | 期間途中の予算変更や過去日の支出追加時の挙動 | 常に「現在の設定 + 全支出」から再計算（状態を保存しない）ことで自然に整合させる |

---

## 2. アーキテクチャ補足（設計書 §2 への実装方針）

- **Expo SDK**: 着手時点の最新安定版（設計書の 51+ 要件を満たす）。New Architecture 有効。
- **ウィジェットターゲット**: `@bacons/apple-targets` を使い、設計書どおり `targets/DayBudgetWidget/` に Swift を置く。`expo prebuild` で Xcode ターゲットと App Group Entitlement を自動生成（ios/ ディレクトリはコミットしない CNG 運用）。
  - `expo-widgets` はその時点の安定度を確認し、安定していれば置き換えを検討。
- **widgetBridge**: Expo Modules API のローカルモジュール（`modules/widget-bridge/`, Swift 数十行）を作成。
  - `setWidgetData(json: string)` → `UserDefaults(suiteName:)` へ書き込み
  - `reloadWidgets()` → `WidgetCenter.shared.reloadAllTimelines()`
  - `getInstalledWidgets()` → `WidgetCenter.shared.getCurrentConfigurations`（`widget_installed` イベント検知用）
- **計算ロジックは純粋関数**: `calculator.ts` / `dateUtils.ts` は DB・React に依存させず、日付を引数で受け取る → Jest で網羅テスト。
- **日付**: すべて端末ローカルタイムの `YYYY-MM-DD` 文字列で扱う（UTC変換による日付ズレを防止）。
- **Pro 機能ゲート**: `useIsPro()` フック 1 箇所に集約。ウィジェット側にも `isPro` を共有JSONで渡し、Free では Medium サイズ・着せ替えをロック表示（タップで `paywall?source=widget_locked` へディープリンク）。

---

## 3. フェーズ別タスク

設計書のマイルストーン（Day 1〜5）を踏襲しつつ、確認作業を含めて各フェーズに完了条件を設けます。

### Phase 1: 基盤構築（設計書 Day 1）

| タスク | 成果物 |
|---|---|
| Expo プロジェクト作成（TypeScript, Expo Router） | `app/`, `package.json`, `app.json`/`app.config.ts` |
| NativeWind 導入・デザイントークン（カラー、状態色、フォント） | `tailwind.config.js`, `global.css` |
| ESLint / Prettier / Jest / TypeScript strict 設定 | 設定ファイル一式 |
| SQLite スキーマ・マイグレーション（`PRAGMA user_version` 方式）、カテゴリ初期データ投入 | `src/db/client.ts`, `schema.ts` |
| Repository（settings / expenses / categories の CRUD） | `src/db/repository.ts` |
| `dateUtils.ts`（月度判定、残り日数、期間内日付列挙） | + 単体テスト |
| `calculator.ts`（均等配分・貯金プール・状態判定・期間シミュレーション） | + 単体テスト |
| Zustand ストア（設定・当期支出・派生値） | `src/stores/useBudgetStore.ts` |
| ルートレイアウトと起動時ルーティング（未設定→onboarding） | `app/_layout.tsx` |

**完了条件**: `npm test` で計算・日付ロジックのテストが全て通る（設計書 §5.1 の例「25日締め／9/27 → 9/26〜10/25」「9/10 → 8/26〜9/25」を含む）。

### Phase 2: コア機能（設計書 Day 2）

| タスク | 備考 |
|---|---|
| オンボーディング画面（月予算入力、締め日選択、期間プレビュー） | 完了で `onboarding_completed` |
| メインダッシュボード（今日使えるお金の巨大表示、状態ラベル、サブテキスト、月度ヘッダー） | 数値のカウントアップアニメーション（Reanimated） |
| 支出入力ボトムシート（テンキー、カテゴリ6種ワンタップ、メモ、決定） | `expo-haptics`、2タップ登録を満たす UI |
| 本日の支出タイムライン（削除はスワイプ or ボタン＋確認） | |
| 日付変更時の再計算（アプリ復帰時 `AppState` で今日を再評価） | |

**完了条件**: Development Build 上で「予算設定 → 支出登録 → 残額が即時更新 → 削除で戻る」が動作。

### Phase 3: ウィジェット連携（設計書 Day 3）

| タスク | 備考 |
|---|---|
| App Group Entitlement 設定（本体・ウィジェット両方） | `app.config.ts` + apple-targets |
| widget-bridge ローカルモジュール（Swift） | `modules/widget-bridge/` |
| `widgetBridge.ts`：ストア更新時に共有JSONを生成・書き込み・リロード | 支出追加/削除/設定変更/日付変更時 |
| `DayBudgetWidget.swift`：TimelineProvider（0:00 エントリで翌日再計算）、Small / Medium ビュー、テーマ3種 | Free は Small 基本テーマのみ |
| ウィジェットタップでアプリ起動（ディープリンク `daybudget://`） | |

**完了条件**: 実機で支出登録後、ホーム画面ウィジェットが数秒以内に更新される。日付をまたいだ際に翌日分の値に切り替わる。

### Phase 4: 履歴・設定・課金（設計書 Day 4）

| タスク | 備考 |
|---|---|
| 履歴画面：月間サマリーカード、カレンダー（日別合計＋緑/赤ドット）、日別詳細（編集・削除） | 月度単位で前後移動 |
| 設定画面：予算・締め日・繰越モード・ウィジェットテーマ・全データ初期化・CSVエクスポート・Pro状態 | CSV は `expo-file-system` + `expo-sharing` |
| RevenueCat 導入（`purchaseService.ts`）：購入・復元・Entitlement `pro` 監視 | Sandbox で検証 |
| ペイウォール画面：キャッチコピー、比較表、購入ボタン、復元・規約・プライバシー | 表示で `paywall_viewed` |
| Pro ゲート適用：貯金プールモード・CSV・ウィジェット着せ替え/Medium | |

**完了条件**: Sandbox アカウントで購入 → Pro 機能解放 → アプリ再インストール後に「購入を復元」で Pro に戻る。

### Phase 5: 仕上げ・申請（設計書 Day 5）

| タスク | 備考 |
|---|---|
| PostHog 導入（`analyticsService.ts`）、§8 の6イベント実装 | 金額は送信しない。`budget_range` はバケット化 |
| アプリアイコン・スプラッシュ | |
| ダークモード・Dynamic Type・VoiceOver ラベルの確認 | |
| App Store 用メタデータ、スクリーンショット、プライバシー栄養ラベル（PostHog 分を申告） | |
| EAS Build（production）→ TestFlight → 審査提出 | `eas.json` |

**完了条件**: TestFlight で一通りのシナリオが通り、審査提出が完了。

---

## 4. 詳細仕様（実装で使う確定版）

### 4.1 スキーマ差分（設計書 §4 からの追加・変更）

```sql
-- user_settings
closing_day   INTEGER NOT NULL DEFAULT 31,              -- 31 = 月末
widget_theme  TEXT    NOT NULL DEFAULT 'dark',          -- 'dark' | 'white' | 'neon'
onboarded     INTEGER NOT NULL DEFAULT 0                -- 起動時ルーティング判定用
```

`expenses` / `categories` は設計書どおり。カテゴリ初期データ：
`food 食費` / `cafe カフェ・軽食` / `daily 日用品` / `transport 交通費` / `fun 娯楽` / `other その他`

### 4.2 月度判定（`dateUtils.getPeriod(today, closingDay)`）

```
effectiveClose(y, m) = min(closingDay, daysInMonth(y, m))
if today.day <= effectiveClose(today.year, today.month):
    end   = (today.year, today.month, effectiveClose(当月))
    start = effectiveClose(前月) の翌日
else:
    start = effectiveClose(当月) の翌日
    end   = (翌月, effectiveClose(翌月))
```

テストケース例：締め日25（9/27 → 9/26〜10/25、9/10 → 8/26〜9/25）、締め日31（2月 → 2/1〜2/28 or 29）、締め日30（3/1 → 3/1〜3/30、2/28 の翌日開始）、年跨ぎ（12/26〜1/25）。

### 4.3 共有 JSON（設計書 §6.2 からの拡張）

```json
{
  "version": 1,
  "todayAvailable": 2450,
  "dailyBudget": 3000,
  "todaySpent": 550,
  "progressRate": 0.183,
  "status": "healthy",
  "currencySymbol": "¥",
  "today": "2026-09-27",
  "periodStart": "2026-09-26",
  "periodEnd": "2026-10-25",
  "remainingBudgetAtStartOfToday": 87000,
  "carryoverMode": "distribute",
  "monthlyBudget": 90000,
  "recentExpenses": [
    { "name": "カフェ・軽食", "icon": "cafe", "amount": 550 }
  ],
  "savingsPace": 4200,
  "widgetTheme": "dark",
  "isPro": false,
  "updatedAt": "2026-09-27T17:15:00Z"
}
```

ウィジェットは `today` が現在日付と異なる場合、`remainingBudgetAtStartOfToday - todaySpent` と残日数から翌日の `dailyBudget` を再計算して表示する（支出0として扱う）。

### 4.4 状態（`status`）

| status | 条件（progressRate） | ラベル | 色 |
|---|---|---|---|
| `great` | < 0.5 | 超順調！ | 緑 |
| `healthy` | < 0.8 | 計画通り | 青 |
| `warning` | ≤ 1.0 | 使いすぎ注意 | 橙 |
| `over` | > 1.0 | 予算オーバー | 赤 |

`dailyBudget = 0` のときは支出0なら `warning`、それ以外は `over`。

---

## 5. テスト方針

- **単体テスト（Jest）**: `dateUtils` / `calculator` / CSV 生成 / 共有JSON 生成。境界値（月末・閏年・年跨ぎ・予算超過・残り1日）を重点的に。
- **Repository テスト**: SQLite をモック or インメモリで CRUD とマイグレーションを検証。
- **手動 E2E チェックリスト**（Phase ごとに実機で確認）：オンボーディング、登録/削除、日付変更、ウィジェット更新、購入/復元、CSV、データ初期化。

## 6. リスクと対策

| リスク | 対策 |
|---|---|
| ウィジェットのネイティブ設定でビルドが通らない | Phase 3 を独立させ、最小ウィジェット（固定文字表示）で先に疎通確認してから本実装 |
| App Store 審査（買い切りIAP・復元ボタン・規約リンク必須） | ペイウォールに復元・規約・プライバシーを必ず配置、Sandbox で全フロー確認 |
| 日付・タイムゾーン起因のバグ | 日付は文字列で扱う・ロジックに「今日」を注入してテスト |
| 4〜5日のスケジュール | 設計書どおりの日程を目標としつつ、アカウント準備や審査待ちはスケジュール外とする |

---

## 7. 次のアクション

1. §1 の確認事項に回答（特に #1 広告、#2 締め日デフォルト）
2. §0.2 のアカウント類を準備（Phase 3 までに App Group、Phase 4 までに RevenueCat が必要）
3. 回答後、**Phase 1（基盤構築）から実装を開始**
