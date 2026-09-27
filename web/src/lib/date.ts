/** 日付は端末ローカルの 'YYYY-MM-DD' 文字列で扱う（UTC 変換による日付ズレを防ぐ） */
export type DateKey = string;

export interface Period {
  start: DateKey;
  end: DateKey;
  /** 期間の日数（開始日・終了日を含む） */
  days: number;
}

/** 締め日の「月末」を表す値。短い月では末日に丸められる */
export const CLOSING_END_OF_MONTH = 31;

const pad = (n: number) => String(n).padStart(2, '0');

export function makeKey(year: number, month: number, day: number): DateKey {
  return `${year}-${pad(month)}-${pad(day)}`;
}

export function parseKey(key: DateKey): [number, number, number] {
  const [y, m, d] = key.split('-').map(Number);
  return [y, m, d];
}

export function toKey(date: Date): DateKey {
  return makeKey(date.getFullYear(), date.getMonth() + 1, date.getDate());
}

export function fromKey(key: DateKey): Date {
  const [y, m, d] = parseKey(key);
  return new Date(y, m - 1, d);
}

export function todayKey(now: Date = new Date()): DateKey {
  return toKey(now);
}

export function addDays(key: DateKey, n: number): DateKey {
  const [y, m, d] = parseKey(key);
  return toKey(new Date(y, m - 1, d + n));
}

const dayNumber = (key: DateKey) => {
  const [y, m, d] = parseKey(key);
  return Date.UTC(y, m - 1, d) / 86_400_000;
};

/** b - a の日数 */
export function diffDays(a: DateKey, b: DateKey): number {
  return dayNumber(b) - dayNumber(a);
}

export function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

function closeKey(year: number, month: number, closingDay: number): DateKey {
  return makeKey(year, month, Math.min(closingDay, daysInMonth(year, month)));
}

function shiftMonth(year: number, month: number, delta: number): [number, number] {
  const idx = year * 12 + (month - 1) + delta;
  return [Math.floor(idx / 12), (idx % 12) + 1];
}

/**
 * 締め日から、指定日が含まれる月度（開始日〜終了日）を求める。
 * 例: 締め日 25 → 9/27 は 9/26〜10/25、9/10 は 8/26〜9/25。
 * 締め日がその月の日数を超える場合は末日に丸める。
 */
export function getPeriod(today: DateKey, closingDay: number): Period {
  const [y, m, d] = parseKey(today);
  const thisClose = Math.min(closingDay, daysInMonth(y, m));
  let start: DateKey;
  let end: DateKey;
  if (d <= thisClose) {
    const [py, pm] = shiftMonth(y, m, -1);
    start = addDays(closeKey(py, pm, closingDay), 1);
    end = makeKey(y, m, thisClose);
  } else {
    const [ny, nm] = shiftMonth(y, m, 1);
    start = addDays(makeKey(y, m, thisClose), 1);
    end = closeKey(ny, nm, closingDay);
  }
  return { start, end, days: diffDays(start, end) + 1 };
}

export function eachDay(start: DateKey, end: DateKey): DateKey[] {
  const out: DateKey[] = [];
  for (let k = start; k <= end; k = addDays(k, 1)) out.push(k);
  return out;
}

const WEEKDAYS = ['日', '月', '火', '水', '木', '金', '土'];

export function weekday(key: DateKey): string {
  return WEEKDAYS[fromKey(key).getDay()];
}

/** '9/27' */
export function formatMD(key: DateKey): string {
  const [, m, d] = parseKey(key);
  return `${m}/${d}`;
}

/** '9月27日(日)' */
export function formatLong(key: DateKey): string {
  const [, m, d] = parseKey(key);
  return `${m}月${d}日(${weekday(key)})`;
}

/** 月度の名前。終了日の月で呼ぶ（9/26〜10/25 → 10月度） */
export function periodName(period: Period): string {
  return `${parseKey(period.end)[1]}月度`;
}
