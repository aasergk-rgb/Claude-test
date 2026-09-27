# DayBudget（Web版）

「今日あといくら使えるか」だけを見るための家計アプリ。口座連携なし、記録はブラウザ（localStorage）の中だけに保存します。

## 使い方

```bash
npm install
npm run dev        # 開発サーバー（http://localhost:5173）
npm test           # 計算ロジック・ストアの単体テスト
npm run typecheck  # 型チェック
npm run build      # 本番ビルド（dist/、相対パスなのでどこに置いても動く）
```

開発サーバーでは、設定画面の一番下に「開発用: Pro版を有効にする」が出ます（本番ビルドには出ません）。

## 構成

| パス | 役割 |
|---|---|
| `src/lib/date.ts` | 月度（締め日）判定、日付ユーティリティ。日付は `YYYY-MM-DD` 文字列で扱う |
| `src/lib/calculator.ts` | 日割り計算（均等配分・貯金プール）、状態判定、途中開始時の予算按分 |
| `src/lib/csv.ts` | CSV 書き出し |
| `src/store/useBudgetStore.ts` | 設定と支出の状態（Zustand + localStorage 永続化） |
| `src/screens/` | オンボーディング・ダッシュボード・履歴・設定・Pro版 |
| `src/components/` | 支出入力シート、シート、確認ダイアログ、トースト、アイコン |
| `public/` | PWA（manifest・Service Worker・アイコン） |

## 未対応（次の段階）

- Pro版の購入（決済）。今は購入ボタンを押すと「準備中」と表示
- アクセス解析（PostHog）
- ホーム画面ウィジェット（iOS版で対応）
