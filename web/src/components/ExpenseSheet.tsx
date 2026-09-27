import { useCallback, useEffect, useState, type CSSProperties } from 'react';
import { CATEGORIES, getCategory } from '../lib/categories';
import { formatLong, type DateKey } from '../lib/date';
import { formatNumber, formatYen } from '../lib/format';
import type { Expense } from '../lib/types';
import { useBudgetStore } from '../store/useBudgetStore';
import { CategoryIcon } from './CategoryIcon';
import { Icon } from './Icon';
import { Sheet } from './Sheet';
import { toast } from './toast';

export type SheetTarget = { mode: 'add'; date: DateKey } | { mode: 'edit'; expense: Expense } | null;

const MAX_DIGITS = 7;
const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '00', '0', 'del'] as const;

const haptic = () => {
  try {
    navigator.vibrate?.(12);
  } catch {
    /* 対応していない端末では何もしない */
  }
};

export function ExpenseSheet({ target, today, onClose }: { target: SheetTarget; today: DateKey; onClose: () => void }) {
  const title = target?.mode === 'edit' ? '支出を編集' : '支出を記録';
  const key = target ? (target.mode === 'edit' ? target.expense.id : `add-${target.date}`) : 'none';
  return (
    <Sheet open={target !== null} title={title} onClose={onClose}>
      {target && <ExpenseForm key={key} target={target} today={today} onClose={onClose} />}
    </Sheet>
  );
}

function ExpenseForm({ target, today, onClose }: { target: NonNullable<SheetTarget>; today: DateKey; onClose: () => void }) {
  const lastCategoryId = useBudgetStore((s) => s.settings.lastCategoryId);
  const startDate = useBudgetStore((s) => s.settings.startDate);
  const { addExpense, updateExpense, deleteExpense, restoreExpense } = useBudgetStore.getState();
  const editing = target.mode === 'edit' ? target.expense : null;

  const [digits, setDigits] = useState(editing ? String(editing.amount) : '');
  const [categoryId, setCategoryId] = useState(editing?.categoryId ?? lastCategoryId);
  const [memo, setMemo] = useState(editing?.memo ?? '');
  const [date, setDate] = useState<DateKey>(editing?.date ?? (target.mode === 'add' ? target.date : today));
  const amount = Number(digits) || 0;

  const press = useCallback((k: string) => {
    setDigits((d) => (k === 'del' ? d.slice(0, -1) : (d + k).replace(/^0+/, '').slice(0, MAX_DIGITS)));
  }, []);

  const submit = useCallback(() => {
    if (!amount) return;
    const data = { amount, categoryId, memo: memo.trim() || null, date };
    if (editing) {
      updateExpense(editing.id, data);
      toast('変更しました');
    } else {
      addExpense(data);
      toast(`${getCategory(categoryId).name} ${formatYen(amount)} を記録しました`);
    }
    haptic();
    onClose();
  }, [amount, categoryId, memo, date, editing, addExpense, updateExpense, onClose]);

  const remove = () => {
    if (!editing) return;
    const removed = deleteExpense(editing.id);
    onClose();
    if (removed) toast(`${getCategory(removed.categoryId).name} ${formatYen(removed.amount)} を削除しました`, { label: '元に戻す', onAction: () => restoreExpense(removed) });
  };

  // パソコンではキーボードでも入力できる
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const inField = e.target instanceof HTMLInputElement;
      if (e.key === 'Enter') {
        e.preventDefault();
        submit();
      } else if (!inField && /^[0-9]$/.test(e.key)) press(e.key);
      else if (!inField && e.key === 'Backspace') press('del');
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [press, submit]);

  return (
    <>
      <label className="date-chip">
        <span>{date === today ? `今日 ${formatLong(date)}` : formatLong(date)}</span>
        <input type="date" value={date} min={startDate ?? undefined} max={today} onChange={(e) => e.target.value && setDate(e.target.value)} aria-label="日付" />
      </label>
      <div className={amount ? 'amount' : 'amount zero'} aria-live="polite">
        <span className="yen">¥</span>
        {formatNumber(amount)}
        <span className="caret" />
      </div>
      <div className="cats" role="radiogroup" aria-label="カテゴリ">
        {CATEGORIES.map((c) => (
          <button
            key={c.id}
            className="cat"
            role="radio"
            aria-checked={c.id === categoryId}
            style={{ '--c': c.color } as CSSProperties}
            onClick={() => setCategoryId(c.id)}
          >
            <CategoryIcon id={c.id} small />
            {c.name}
          </button>
        ))}
      </div>
      <input
        className="memo"
        id="memo"
        type="text"
        placeholder="メモ（任意）例：コンビニ"
        maxLength={40}
        autoComplete="off"
        value={memo}
        onChange={(e) => setMemo(e.target.value)}
      />
      <div className="keys">
        {KEYS.map((k) => (
          <button key={k} className="key" onClick={() => press(k)} aria-label={k === 'del' ? '1文字消す' : k}>
            {k === 'del' ? <Icon name="backspace" size={24} /> : k}
          </button>
        ))}
      </div>
      <div className={editing ? 'sheet-actions two' : 'sheet-actions'}>
        {editing && (
          <button className="danger-btn" onClick={remove} aria-label="この支出を削除">
            <Icon name="trash" />
          </button>
        )}
        <button className="primary" onClick={submit} disabled={!amount}>
          {editing ? '変更を保存' : '決定'}
        </button>
      </div>
    </>
  );
}
