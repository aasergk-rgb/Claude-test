import { useMemo, useState } from 'react';
import { CategoryIcon } from '../components/CategoryIcon';
import type { SheetTarget } from '../components/ExpenseSheet';
import { Icon } from '../components/Icon';
import { navigate } from '../hooks/useRoute';
import { activeRange, simulatePeriod } from '../lib/calculator';
import { getCategory } from '../lib/categories';
import { addDays, diffDays, formatLong, formatMD, fromKey, getPeriod, parseKey, periodName, type DateKey } from '../lib/date';
import { formatSignedYen, formatYen } from '../lib/format';
import { useBudgetStore } from '../store/useBudgetStore';

const WEEKDAYS = ['日', '月', '火', '水', '木', '金', '土'];

export function History({ today, openSheet }: { today: DateKey; openSheet: (t: SheetTarget) => void }) {
  const settings = useBudgetStore((s) => s.settings);
  const expenses = useBudgetStore((s) => s.expenses);
  const { monthlyBudget, closingDay, carryoverMode: mode, startDate } = settings;

  const [anchor, setAnchor] = useState<DateKey>(today);
  const period = useMemo(() => getPeriod(anchor, closingDay), [anchor, closingDay]);
  const isCurrent = today >= period.start && today <= period.end;
  const [selected, setSelected] = useState<DateKey>(today);
  const range = activeRange(period, monthlyBudget, startDate);
  const selectedDay =
    selected >= range.from && selected <= period.end && selected <= today ? selected : isCurrent ? today : period.end;

  const days = useMemo(
    () => simulatePeriod(period, { monthlyBudget, mode, expenses, today, startDate }),
    [period, monthlyBudget, mode, expenses, today, startDate],
  );
  const spent = days.reduce((a, d) => a + d.spent, 0);
  const elapsed = isCurrent ? diffDays(range.from, today) + 1 : range.days;
  const third = isCurrent
    ? { label: 'ペースより', value: Math.floor((range.budget * elapsed) / range.days) - spent }
    : { label: '残った額', value: range.budget - spent };

  const dayInfo = days.find((d) => d.date === selectedDay);
  const dayItems = useMemo(
    () => expenses.filter((e) => e.date === selectedDay).sort((a, b) => a.createdAt.localeCompare(b.createdAt)),
    [expenses, selectedDay],
  );
  const leading = fromKey(period.start).getDay();
  const hasOlder = !!startDate && startDate < period.start;

  return (
    <section className="screen">
      <header className="topbar">
        <button className="icon-btn" onClick={() => navigate('dashboard')} aria-label="戻る">
          <Icon name="left" />
        </button>
        <div className="t">
          <b>履歴</b>
        </div>
        <span />
      </header>

      <div className="month-switch">
        <button className="icon-btn" onClick={() => setAnchor(addDays(period.start, -1))} disabled={!hasOlder} aria-label="前の月度">
          <Icon name="left" />
        </button>
        <div className="t">
          <b>{periodName(period)}</b>
          <small className="num">
            {formatMD(period.start)}〜{formatMD(period.end)}
          </small>
        </div>
        <button className="icon-btn" onClick={() => setAnchor(addDays(period.end, 1))} disabled={isCurrent} aria-label="次の月度">
          <Icon name="right" />
        </button>
      </div>

      <div className="summary">
        <div>
          <small>{range.budget < monthlyBudget ? '今月の予算（日割り）' : '今月の予算'}</small>
          <b>{formatYen(range.budget)}</b>
        </div>
        <div>
          <small>累計支出</small>
          <b>{formatYen(spent)}</b>
        </div>
        <div>
          <small>{third.label}</small>
          <b style={{ color: third.value >= 0 ? 'var(--great)' : 'var(--over)' }}>{formatSignedYen(third.value)}</b>
        </div>
      </div>

      <div className="cal">
        {WEEKDAYS.map((w, i) => (
          <div key={w} className={i === 0 ? 'wd sun' : i === 6 ? 'wd sat' : 'wd'}>
            {w}
          </div>
        ))}
        {Array.from({ length: leading }, (_, i) => (
          <div key={`b${i}`} className="day blank" />
        ))}
        {days.map((d, i) => {
          const [, m, dd] = parseKey(d.date);
          const future = d.state === 'future';
          const off = d.state === 'inactive';
          const cls = ['day', future ? 'future' : off ? 'off' : d.spent <= d.budget ? 'g' : 'r', d.date === today ? 'today' : ''].join(' ');
          return (
            <button key={d.date} className={cls} disabled={future || off} aria-pressed={d.date === selectedDay} onClick={() => setSelected(d.date)} aria-label={formatLong(d.date)}>
              <span className="d">{i === 0 || dd === 1 ? `${m}/${dd}` : dd}</span>
              <span className="a">{future || off ? '' : d.spent.toLocaleString('ja-JP')}</span>
              <span className="dot" />
            </button>
          );
        })}
      </div>
      <div className="cal-legend">
        <span>
          <i style={{ background: 'var(--great)' }} />
          予算内
        </span>
        <span>
          <i style={{ background: 'var(--over)' }} />
          超過
        </span>
      </div>

      <div className="day-head">
        <b>{formatLong(selectedDay)}</b>
        {dayInfo && (
          <small className="num">
            {formatYen(dayInfo.spent)} / 予算 {formatYen(dayInfo.budget)}
          </small>
        )}
      </div>
      <ul className="list">
        {dayItems.length === 0 && <li className="empty">この日の支出はありません</li>}
        {dayItems.map((e) => (
          <li key={e.id} className="row">
            <button className="row-main" onClick={() => openSheet({ mode: 'edit', expense: e })}>
              <CategoryIcon id={e.categoryId} />
              <span className="meta">
                <b>{getCategory(e.categoryId).name}</b>
                <small>{e.memo || '　'}</small>
              </span>
              <span className="amt">{formatYen(e.amount)}</span>
            </button>
            <button className="del" onClick={() => openSheet({ mode: 'edit', expense: e })} aria-label="編集">
              <Icon name="right" />
            </button>
          </li>
        ))}
      </ul>
      <button className="add-day" onClick={() => openSheet({ mode: 'add', date: selectedDay })}>
        <Icon name="plus" />
        この日に支出を追加
      </button>
    </section>
  );
}
