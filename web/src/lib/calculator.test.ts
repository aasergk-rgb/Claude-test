import { describe, expect, it } from 'vitest';
import { computeToday, simulatePeriod, statusOf } from './calculator';
import { eachDay, getPeriod } from './date';
import type { Expense } from './types';

let n = 0;
const ex = (date: string, amount: number, categoryId = 'food'): Expense => ({
  id: String(++n), amount, categoryId, memo: null, date,
  createdAt: `${date}T12:00:00.000Z`, updatedAt: `${date}T12:00:00.000Z`,
});

// 9/1〜9/26 に計 38,000 円使い、今日 9/27 に 550 円使った状態（デモと同じ）
const PAST = [1200,1850,980,2400,3100,0,1520,1340,760,2100,1890,2620,650,1100,1480,1720,900,2650,1300,0,1150,1640,980,2210,1560,900];
const sample = [
  ...PAST.map((a, i) => ex(`2026-09-${String(i + 1).padStart(2, '0')}`, a)).filter((e) => e.amount > 0),
  ex('2026-09-27', 330, 'cafe'),
  ex('2026-09-27', 220, 'daily'),
];

describe('statusOf', () => {
  it('使用率で4段階', () => {
    expect(statusOf(0, 3000)).toBe('great');
    expect(statusOf(1499, 3000)).toBe('great');
    expect(statusOf(1500, 3000)).toBe('healthy');
    expect(statusOf(2400, 3000)).toBe('warning');
    expect(statusOf(3000, 3000)).toBe('warning');
    expect(statusOf(3001, 3000)).toBe('over');
  });
  it('割当0円の日', () => {
    expect(statusOf(0, 0)).toBe('warning');
    expect(statusOf(1, 0)).toBe('over');
  });
});

describe('均等配分モード', () => {
  const base = { monthlyBudget: 50000, closingDay: 31, mode: 'distribute' as const, today: '2026-09-27' };

  it('設計書の例: 割当 ¥3,000 / 支出 ¥550 → 残り ¥2,450', () => {
    const s = computeToday({ ...base, expenses: sample });
    expect(s.dailyBudget).toBe(3000);
    expect(s.todaySpent).toBe(550);
    expect(s.todayAvailable).toBe(2450);
    expect(s.progressRate).toBeCloseTo(0.183, 3);
    expect(s.status).toBe('great');
    expect(s.remainingDays).toBe(4);
    expect(s.periodRemaining).toBe(11450);
    expect(s.tomorrowBudget).toBe(3816);
    expect(s.pace).toBe(45000 - 38550);
  });

  it('初日は 予算 / 日数（切り捨て）', () => {
    const s = computeToday({ ...base, expenses: [], today: '2026-09-01' });
    expect(s.dailyBudget).toBe(1666);
    expect(s.status).toBe('great');
  });

  it('節約すると翌日以降の割当が増え、使いすぎると減る', () => {
    const save = computeToday({ ...base, today: '2026-09-02', expenses: [ex('2026-09-01', 0)] });
    const spend = computeToday({ ...base, today: '2026-09-02', expenses: [ex('2026-09-01', 5000)] });
    expect(save.dailyBudget).toBe(Math.floor(50000 / 29));
    expect(spend.dailyBudget).toBe(Math.floor(45000 / 29));
  });

  it('予算を使い切ったら割当は0円で、それ以上はマイナス表示', () => {
    const s = computeToday({ ...base, today: '2026-09-10', expenses: [ex('2026-09-01', 60000), ex('2026-09-10', 500)] });
    expect(s.dailyBudget).toBe(0);
    expect(s.todayAvailable).toBe(-500);
    expect(s.status).toBe('over');
  });

  it('最終日は明日の見込みなし', () => {
    expect(computeToday({ ...base, today: '2026-09-30', expenses: [] }).tomorrowBudget).toBeNull();
  });

  it('期間外の支出は数えない', () => {
    const s = computeToday({ ...base, expenses: [ex('2026-08-31', 9999), ex('2026-10-01', 9999)] });
    expect(s.periodSpent).toBe(0);
  });

  it('simulatePeriod は過去日の予算内/超過を判定する', () => {
    const days = simulatePeriod(getPeriod('2026-09-27', 31), { ...base, expenses: sample });
    expect(days).toHaveLength(30);
    expect(days[0]).toMatchObject({ date: '2026-09-01', budget: 1666, spent: 1200, state: 'under' });
    expect(days[1]).toMatchObject({ budget: 1682, spent: 1850, state: 'over' });
    expect(days[26]).toMatchObject({ date: '2026-09-27', budget: 3000, spent: 550, state: 'today' });
    expect(days[27].state).toBe('future');
    // 今日の割当は computeToday と一致する
    expect(days[26].budget).toBe(computeToday({ ...base, expenses: sample }).dailyBudget);
  });
});

describe('貯金プールモード', () => {
  const base = { monthlyBudget: 30000, closingDay: 31, mode: 'savings' as const };

  it('毎日の割当は固定で、余った分が貯金になる（超過日は0として数える）', () => {
    const expenses = [ex('2026-09-01', 400), ex('2026-09-02', 1500), ex('2026-09-03', 0), ex('2026-09-04', 200)];
    const s = computeToday({ ...base, expenses, today: '2026-09-04' });
    expect(s.dailyBudget).toBe(1000);
    expect(s.todayAvailable).toBe(800);
    expect(s.savingsAmount).toBe(600 + 0 + 1000);
    expect(s.tomorrowBudget).toBe(1000);
  });

  it('日ごとの割当は期間中ずっと同じ', () => {
    const p = getPeriod('2026-09-10', 31);
    const days = simulatePeriod(p, { ...base, expenses: [ex('2026-09-01', 9000)], today: '2026-09-10' });
    expect(new Set(days.map((d) => d.budget))).toEqual(new Set([1000]));
    expect(days.map((d) => d.date)).toEqual(eachDay(p.start, p.end));
  });
});

describe('月度の途中から使い始めた場合', () => {
  const base = { monthlyBudget: 50000, closingDay: 31, mode: 'distribute' as const };

  it('最初の月度の予算は残り日数ぶんに按分する', () => {
    const s = computeToday({ ...base, expenses: [], today: '2026-09-27', startDate: '2026-09-27' });
    expect(s.periodBudget).toBe(Math.floor((50000 * 4) / 30));
    expect(s.dailyBudget).toBe(Math.floor(6666 / 4));
    expect(s.dayNumber).toBe(1);
    expect(s.pace).toBe(Math.floor(6666 / 4));
  });

  it('使い始める前の日は集計しない', () => {
    const expenses = [ex('2026-09-10', 9999), ex('2026-09-28', 500)];
    const s = computeToday({ ...base, expenses, today: '2026-09-28', startDate: '2026-09-27' });
    expect(s.periodSpent).toBe(500);
    const days = simulatePeriod(getPeriod('2026-09-28', 31), { ...base, expenses, today: '2026-09-28', startDate: '2026-09-27' });
    expect(days[9]).toMatchObject({ date: '2026-09-10', state: 'inactive', budget: 0 });
    expect(days[26]).toMatchObject({ date: '2026-09-27', state: 'under', budget: 1666 });
  });

  it('次の月度からは満額', () => {
    const s = computeToday({ ...base, expenses: [], today: '2026-10-01', startDate: '2026-09-27' });
    expect(s.periodBudget).toBe(50000);
    expect(s.dailyBudget).toBe(Math.floor(50000 / 31));
  });

  it('月度の初日に始めた場合は按分しない', () => {
    expect(computeToday({ ...base, expenses: [], today: '2026-09-01', startDate: '2026-09-01' }).periodBudget).toBe(50000);
  });
});
