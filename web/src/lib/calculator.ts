import { diffDays, eachDay, getPeriod, type DateKey, type Period } from './date';
import type { CarryoverMode, Expense } from './types';

export type Status = 'great' | 'healthy' | 'warning' | 'over';

export const STATUS_LABEL: Record<Status, string> = {
  great: '超順調！',
  healthy: '計画通り',
  warning: '使いすぎ注意',
  over: '予算オーバー',
};

/** 今日の予算に対する使用率で状態を決める（<50% / <80% / ≤100% / >100%） */
export function statusOf(spent: number, budget: number): Status {
  if (budget <= 0) return spent > 0 ? 'over' : 'warning';
  const r = spent / budget;
  if (r < 0.5) return 'great';
  if (r < 0.8) return 'healthy';
  if (r <= 1) return 'warning';
  return 'over';
}

export function totalsByDate(expenses: Expense[]): Map<DateKey, number> {
  const m = new Map<DateKey, number>();
  for (const e of expenses) m.set(e.date, (m.get(e.date) ?? 0) + e.amount);
  return m;
}

export interface BudgetInput {
  monthlyBudget: number;
  closingDay: number;
  mode: CarryoverMode;
  expenses: Expense[];
  today: DateKey;
  /** 使い始めた日。月度の途中から始めた場合、その月度の予算は残り日数ぶんに按分する */
  startDate?: DateKey | null;
}

export type DayState = 'under' | 'over' | 'today' | 'future' | 'inactive';

export interface ActiveRange {
  /** 集計を始める日（使い始めた日か月度の初日の遅いほう） */
  from: DateKey;
  days: number;
  /** この月度に使える予算（途中開始なら按分） */
  budget: number;
}

export function activeRange(period: Period, monthlyBudget: number, startDate?: DateKey | null): ActiveRange {
  if (!startDate || startDate <= period.start || startDate > period.end) {
    return { from: period.start, days: period.days, budget: monthlyBudget };
  }
  const days = diffDays(startDate, period.end) + 1;
  return { from: startDate, days, budget: Math.floor((monthlyBudget * days) / period.days) };
}

export interface DaySummary {
  date: DateKey;
  /** その日の割当予算（未来日は「今日以降使わなかった場合」の見込み） */
  budget: number;
  spent: number;
  state: DayState;
}

/**
 * 期間内の各日の割当予算と実績を求める。
 * 均等配分モードでは割当が日ごとに変わるため、期間の初日から順に計算する。
 */
export function simulatePeriod(
  period: Period,
  { monthlyBudget, mode, expenses, today, startDate }: Omit<BudgetInput, 'closingDay'>,
): DaySummary[] {
  const totals = totalsByDate(expenses);
  const range = activeRange(period, monthlyBudget, startDate);
  const fixed = Math.floor(monthlyBudget / period.days);
  let remaining = range.budget;
  let left = range.days;
  return eachDay(period.start, period.end).map((date) => {
    if (date < range.from) return { date, budget: 0, spent: 0, state: 'inactive' as const };
    const spent = date <= today ? (totals.get(date) ?? 0) : 0;
    const budget = mode === 'savings' ? fixed : Math.max(0, Math.floor(remaining / left));
    remaining -= spent;
    left -= 1;
    const state: DayState =
      date > today ? 'future' : date === today ? 'today' : spent <= budget ? 'under' : 'over';
    return { date, budget, spent, state };
  });
}

export interface TodaySnapshot {
  period: Period;
  /** この月度に使える予算（途中開始なら按分済み） */
  periodBudget: number;
  /** 今日を含む残り日数 */
  remainingDays: number;
  /** 使い始めてから今日が何日目か（1始まり） */
  dayNumber: number;
  dailyBudget: number;
  todaySpent: number;
  todayAvailable: number;
  progressRate: number;
  status: Status;
  /** 期間内の累計支出（今日を含む） */
  periodSpent: number;
  /** 期間の残予算（今日の支出を差し引いた後） */
  periodRemaining: number;
  /** 今日このあと使わなかった場合の明日の割当。最終日は null */
  tomorrowBudget: number | null;
  /** 貯金プールモードで貯まった額（昨日まで） */
  savingsAmount: number;
  /** 経過日数ぶんの予算ペースと実績の差（プラスなら節約できている） */
  pace: number;
}

export function computeToday(input: BudgetInput): TodaySnapshot {
  const { monthlyBudget, closingDay, mode, expenses, today, startDate } = input;
  const period = getPeriod(today, closingDay);
  const range = activeRange(period, monthlyBudget, startDate);
  const inPeriod = expenses.filter((e) => e.date >= range.from && e.date <= today);
  const periodSpent = inPeriod.reduce((a, e) => a + e.amount, 0);
  const todaySpent = inPeriod.filter((e) => e.date === today).reduce((a, e) => a + e.amount, 0);
  const remainingDays = diffDays(today, period.end) + 1;
  const dayNumber = diffDays(range.from, today) + 1;
  const remainingAtStart = range.budget - (periodSpent - todaySpent);

  let dailyBudget: number;
  let tomorrowBudget: number | null;
  let savingsAmount = 0;
  if (mode === 'savings') {
    dailyBudget = Math.floor(monthlyBudget / period.days);
    tomorrowBudget = remainingDays > 1 ? dailyBudget : null;
    for (const d of simulatePeriod(period, { monthlyBudget, mode, expenses, today, startDate })) {
      if (d.state === 'under' || d.state === 'over') savingsAmount += Math.max(0, d.budget - d.spent);
    }
  } else {
    dailyBudget = Math.max(0, Math.floor(remainingAtStart / remainingDays));
    tomorrowBudget =
      remainingDays > 1
        ? Math.max(0, Math.floor((remainingAtStart - todaySpent) / (remainingDays - 1)))
        : null;
  }

  return {
    period,
    periodBudget: range.budget,
    remainingDays,
    dayNumber,
    dailyBudget,
    todaySpent,
    todayAvailable: dailyBudget - todaySpent,
    progressRate: dailyBudget > 0 ? todaySpent / dailyBudget : todaySpent > 0 ? 1 : 0,
    status: statusOf(todaySpent, dailyBudget),
    periodSpent,
    periodRemaining: range.budget - periodSpent,
    tomorrowBudget,
    savingsAmount,
    pace: Math.floor((range.budget * dayNumber) / range.days) - periodSpent,
  };
}
