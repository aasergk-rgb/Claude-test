import { CLOSING_END_OF_MONTH } from '../lib/date';

export const BUDGET_PRESETS = [30000, 50000, 80000, 100000];
export const MAX_BUDGET = 9_999_999;

export function BudgetInput({ value, onChange }: { value: number; onChange: (n: number) => void }) {
  return (
    <>
      <label className="budget-field" htmlFor="budget">
        <span>¥</span>
        <input
          id="budget"
          inputMode="numeric"
          autoComplete="off"
          value={value ? value.toLocaleString('ja-JP') : ''}
          placeholder="0"
          onChange={(e) => onChange(Math.min(MAX_BUDGET, Number(e.target.value.replace(/\D/g, '')) || 0))}
          aria-label="月の予算"
        />
      </label>
      <div className="presets">
        {BUDGET_PRESETS.map((v) => (
          <button key={v} aria-pressed={v === value} onClick={() => onChange(v)}>
            {v / 10000}万
          </button>
        ))}
      </div>
    </>
  );
}

export const closingLabel = (d: number) => (d >= CLOSING_END_OF_MONTH ? '月末' : `${d}日`);

const QUICK = [5, 10, 15, 20, 25, CLOSING_END_OF_MONTH];

export function ClosingDayPicker({ value, onChange, showAll }: { value: number; onChange: (d: number) => void; showAll?: boolean }) {
  const options = showAll ? Array.from({ length: CLOSING_END_OF_MONTH }, (_, i) => i + 1) : QUICK;
  return (
    <div className={showAll ? 'days all' : 'days'}>
      {options.map((d) => (
        <button key={d} aria-pressed={d === value} onClick={() => onChange(d)}>
          {closingLabel(d)}
        </button>
      ))}
    </div>
  );
}
