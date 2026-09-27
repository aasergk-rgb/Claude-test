import { useEffect, useMemo, useRef, type CSSProperties } from 'react';
import { CategoryIcon } from '../components/CategoryIcon';
import type { SheetTarget } from '../components/ExpenseSheet';
import { Icon } from '../components/Icon';
import { toast } from '../components/toast';
import { useCountUp } from '../hooks/useCountUp';
import { navigate } from '../hooks/useRoute';
import { computeToday, simulatePeriod, STATUS_LABEL } from '../lib/calculator';
import { getCategory } from '../lib/categories';
import { formatMD, periodName, type DateKey } from '../lib/date';
import { formatNumber, formatTime, formatYen } from '../lib/format';
import { useBudgetStore } from '../store/useBudgetStore';

export function Dashboard({ today, openSheet }: { today: DateKey; openSheet: (t: SheetTarget) => void }) {
  const settings = useBudgetStore((s) => s.settings);
  const expenses = useBudgetStore((s) => s.expenses);
  const { deleteExpense, restoreExpense } = useBudgetStore.getState();

  const { monthlyBudget, closingDay, carryoverMode: mode, startDate } = settings;
  const snap = useMemo(
    () => computeToday({ monthlyBudget, closingDay, mode, expenses, today, startDate }),
    [monthlyBudget, closingDay, mode, expenses, today, startDate],
  );
  const days = useMemo(
    () => simulatePeriod(snap.period, { monthlyBudget, mode, expenses, today, startDate }),
    [snap.period, monthlyBudget, mode, expenses, today, startDate],
  );
  const todays = useMemo(
    () => expenses.filter((e) => e.date === today).sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    [expenses, today],
  );

  const shown = useCountUp(snap.todayAvailable);
  const bigRef = useRef<HTMLDivElement>(null);
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    const el = bigRef.current;
    if (!el) return;
    el.classList.remove('pulse');
    void el.offsetWidth;
    el.classList.add('pulse');
  }, [snap.todayAvailable]);

  const st = `var(--${snap.status})`;
  const alarming = snap.status === 'warning' || snap.status === 'over';
  const savings = mode === 'savings';

  const remove = (id: string) => {
    const removed = deleteExpense(id);
    if (removed)
      toast(`${getCategory(removed.categoryId).name} ${formatYen(removed.amount)} を削除しました`, {
        label: '元に戻す',
        onAction: () => restoreExpense(removed),
      });
  };

  return (
    <section className="screen has-dock">
      <header className="topbar">
        <button className="icon-btn" onClick={() => navigate('settings')} aria-label="設定">
          <Icon name="sliders" />
        </button>
        <div className="t">
          <b>{periodName(snap.period)}</b>
          <small className="num">
            {formatMD(snap.period.start)}〜{formatMD(snap.period.end)} · 残り{snap.remainingDays}日
          </small>
        </div>
        <button className="icon-btn" onClick={() => navigate('history')} aria-label="履歴">
          <Icon name="calendar" />
        </button>
      </header>

      <div className="hero">
        <div className="hero-label">今日使えるお金</div>
        <div
          className="big"
          ref={bigRef}
          style={{ '--hero-c': alarming ? st : 'var(--ink)', fontSize: `min(20vw, 84px, ${Math.round(540 / (formatNumber(shown).length + 1))}px)` } as CSSProperties}
        >
          {shown < 0 && '−'}
          <span className="yen">¥</span>
          {Math.abs(shown).toLocaleString('ja-JP')}
        </div>
        <span className="chip" style={{ '--st': st } as CSSProperties}>
          {STATUS_LABEL[snap.status]}
        </span>
      </div>

      <div className="meter">
        <div className="meter-track">
          <div className="meter-fill" style={{ '--st': st, width: `${Math.min(100, snap.progressRate * 100)}%` } as CSSProperties} />
        </div>
        <div className="meter-legend">
          <span>
            本日の支出 <b className="mono">{formatYen(snap.todaySpent)}</b>
          </span>
          <span>
            本日の割当 <b className="mono">{formatYen(snap.dailyBudget)}</b>
          </span>
        </div>
      </div>

      {savings ? (
        <div className="hint">
          <span>今月の貯金（昨日まで）</span>
          <b>{formatYen(snap.savingsAmount)}</b>
        </div>
      ) : snap.tomorrowBudget !== null ? (
        <div className="hint">
          <span>このあと使わなければ、明日は</span>
          <b>{formatYen(snap.tomorrowBudget)}</b>
        </div>
      ) : (
        <div className="hint">
          <span>今日で{periodName(snap.period)}はおしまい。残りは</span>
          <b>{formatYen(snap.periodRemaining)}</b>
        </div>
      )}

      <div className="period">
        <div className="section-h">
          {periodName(snap.period)}の歩み <span className="mono">残り {formatYen(snap.periodRemaining)}</span>
        </div>
        <div className="ticks" style={{ gridTemplateColumns: `repeat(${days.length}, minmax(0, 1fr))` }}>
          {days.map((d) => (
            <i
              key={d.date}
              className={d.state === 'today' ? 'today' : d.state === 'under' ? 'g' : d.state === 'over' ? 'r' : d.state === 'inactive' ? 'off' : ''}
              title={`${formatMD(d.date)} ${formatYen(d.spent)} / ${formatYen(d.budget)}`}
            />
          ))}
        </div>
        <div className="ticks-legend">
          <span>{formatMD(snap.period.start)}</span>
          <span>{formatMD(snap.period.end)}</span>
        </div>
      </div>

      <div className="today-list">
        <div className="section-h">
          今日の支出 <span>{todays.length}件</span>
        </div>
        <ul className="list">
          {todays.length === 0 && <li className="empty">今日はまだ記録がありません</li>}
          {todays.map((e) => (
            <li key={e.id} className="row">
              <button className="row-main" onClick={() => openSheet({ mode: 'edit', expense: e })} aria-label={`${getCategory(e.categoryId).name} ${formatYen(e.amount)} を編集`}>
                <CategoryIcon id={e.categoryId} />
                <span className="meta">
                  <b>{getCategory(e.categoryId).name}</b>
                  <small>
                    {formatTime(e.createdAt)}
                    {e.memo ? ` · ${e.memo}` : ''}
                  </small>
                </span>
                <span className="amt">{formatYen(e.amount)}</span>
              </button>
              <button className="del" onClick={() => remove(e.id)} aria-label="削除">
                <Icon name="trash" />
              </button>
            </li>
          ))}
        </ul>
      </div>

      <div className="dock">
        <button className="primary" onClick={() => openSheet({ mode: 'add', date: today })}>
          <Icon name="plus" />
          支出を記録
        </button>
      </div>
    </section>
  );
}
