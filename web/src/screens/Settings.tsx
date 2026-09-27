import { useState } from 'react';
import { BudgetInput, ClosingDayPicker, closingLabel } from '../components/BudgetFields';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Icon } from '../components/Icon';
import { Sheet } from '../components/Sheet';
import { toast } from '../components/toast';
import { navigate } from '../hooks/useRoute';
import { expensesToCsv } from '../lib/csv';
import { formatMD, getPeriod, todayKey, type DateKey } from '../lib/date';
import { formatYen } from '../lib/format';
import type { AppTheme, CarryoverMode } from '../lib/types';
import { useBudgetStore } from '../store/useBudgetStore';

const THEMES: [AppTheme, string][] = [
  ['system', '自動'],
  ['light', 'ライト'],
  ['dark', 'ダーク'],
];

const MODE_HELP: Record<CarryoverMode, string> = {
  distribute: '使わなかった分は、翌日以降の予算に均等に上乗せされます。使いすぎた分は翌日以降から均等に差し引かれます。',
  savings: '毎日の予算は固定です。使わなかった分は翌日に回さず、今月の貯金として貯まっていきます。',
};

function downloadCsv() {
  const csv = expensesToCsv(useBudgetStore.getState().expenses);
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = `daybudget-${todayKey().replace(/-/g, '')}.csv`;
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function Settings({ today }: { today: DateKey }) {
  const settings = useBudgetStore((s) => s.settings);
  const count = useBudgetStore((s) => s.expenses.length);
  const { updateSettings, resetAll } = useBudgetStore.getState();
  const [editing, setEditing] = useState<'budget' | 'closing' | null>(null);
  const [draft, setDraft] = useState(0);
  const [confirmReset, setConfirmReset] = useState(false);

  const openEditor = (kind: 'budget' | 'closing') => {
    setDraft(kind === 'budget' ? settings.monthlyBudget : settings.closingDay);
    setEditing(kind);
  };
  const saveEditor = () => {
    updateSettings(editing === 'budget' ? { monthlyBudget: draft } : { closingDay: draft });
    setEditing(null);
    toast('保存しました');
  };
  const setMode = (m: CarryoverMode) => {
    if (m === 'savings' && !settings.isPro) return navigate('paywall');
    updateSettings({ carryoverMode: m });
  };
  const exportCsv = () => {
    if (!settings.isPro) return navigate('paywall');
    if (!count) return toast('書き出す記録がまだありません');
    downloadCsv();
  };
  const preview = getPeriod(today, draft || 31);

  return (
    <section className="screen">
      <header className="topbar">
        <button className="icon-btn" onClick={() => navigate('dashboard')} aria-label="戻る">
          <Icon name="left" />
        </button>
        <div className="t">
          <b>設定</b>
        </div>
        <span />
      </header>

      {settings.isPro ? (
        <div className="pro-card">
          <span className="badge">
            <Icon name="check" />
          </span>
          <div>
            <b>Pro版 有効</b>
            <small>すべての機能が使えます</small>
          </div>
        </div>
      ) : (
        <button className="pro-card" onClick={() => navigate('paywall')}>
          <span className="badge">
            <Icon name="spark" />
          </span>
          <div>
            <b>DayBudget Pro</b>
            <small>貯金プール・CSV書き出し</small>
          </div>
          <span className="price">¥1,000</span>
        </button>
      )}

      <div className="group">
        <h3>予算</h3>
        <div className="panel">
          <button className="item" onClick={() => openEditor('budget')}>
            <span>月の予算</span>
            <span className="v">
              {formatYen(settings.monthlyBudget)}
              <Icon name="right" />
            </span>
          </button>
          <button className="item" onClick={() => openEditor('closing')}>
            <span>締め日</span>
            <span className="v">
              {closingLabel(settings.closingDay)}
              <Icon name="right" />
            </span>
          </button>
          <div className="sub">
            <div className="seg" role="radiogroup" aria-label="繰越モード">
              <button role="radio" aria-checked={settings.carryoverMode === 'distribute'} onClick={() => setMode('distribute')}>
                均等配分
              </button>
              <button role="radio" aria-checked={settings.carryoverMode === 'savings'} onClick={() => setMode('savings')}>
                貯金プール {!settings.isPro && <span className="pro-tag">PRO</span>}
              </button>
            </div>
            <p>{MODE_HELP[settings.carryoverMode]}</p>
          </div>
        </div>
      </div>

      <div className="group">
        <h3>表示</h3>
        <div className="panel">
          <div className="sub">
            <div className="seg" role="radiogroup" aria-label="テーマ">
              {THEMES.map(([id, label]) => (
                <button key={id} role="radio" aria-checked={settings.theme === id} onClick={() => updateSettings({ theme: id })}>
                  {label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="group">
        <h3>データ</h3>
        <div className="panel">
          <button className="item" onClick={exportCsv}>
            <span>CSVで書き出す</span>
            <span className="v">
              {!settings.isPro && <span className="pro-tag">PRO</span>}
              <Icon name="right" />
            </span>
          </button>
          <button className="item danger" onClick={() => setConfirmReset(true)}>
            <span>すべてのデータを消去</span>
          </button>
          {import.meta.env.DEV && (
            <button className="item" onClick={() => updateSettings({ isPro: !settings.isPro })}>
              <span>開発用: Pro版を{settings.isPro ? '無効' : '有効'}にする</span>
            </button>
          )}
        </div>
      </div>

      <p className="foot">
        DayBudget {__APP_VERSION__}
        <br />
        記録はこの端末のブラウザの中だけに保存され、外部には送信されません。
      </p>

      <Sheet open={editing !== null} title={editing === 'budget' ? '月の予算' : '締め日'} onClose={() => setEditing(null)}>
        {editing === 'budget' && <BudgetInput value={draft} onChange={setDraft} />}
        {editing === 'closing' && (
          <>
            <ClosingDayPicker value={draft} onChange={setDraft} showAll />
            <div className="preview">
              <div>
                <span>今の月度</span>
                <b>
                  {formatMD(preview.start)}〜{formatMD(preview.end)}（{preview.days}日間）
                </b>
              </div>
            </div>
          </>
        )}
        <button className="primary" onClick={saveEditor} disabled={!draft}>
          保存
        </button>
      </Sheet>

      <ConfirmDialog
        open={confirmReset}
        title="すべてのデータを消去しますか？"
        message="支出の記録と予算の設定がすべて消えます。元に戻すことはできません。"
        confirmLabel="消去する"
        onCancel={() => setConfirmReset(false)}
        onConfirm={() => {
          setConfirmReset(false);
          resetAll();
          navigate('dashboard');
        }}
      />
    </section>
  );
}
