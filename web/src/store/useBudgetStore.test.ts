import { beforeEach, describe, expect, it } from 'vitest';

const mem = new Map<string, string>();
globalThis.localStorage = {
  getItem: (k: string) => mem.get(k) ?? null,
  setItem: (k: string, v: string) => void mem.set(k, v),
  removeItem: (k: string) => void mem.delete(k),
  clear: () => mem.clear(),
  key: () => null,
  length: 0,
} as Storage;

const { useBudgetStore } = await import('./useBudgetStore');

describe('useBudgetStore', () => {
  beforeEach(() => useBudgetStore.getState().resetAll());

  it('初期状態は未設定・月末締め', () => {
    const { settings } = useBudgetStore.getState();
    expect(settings.onboarded).toBe(false);
    expect(settings.closingDay).toBe(31);
  });

  it('支出の追加・更新・削除・復元', () => {
    const s = useBudgetStore.getState();
    const e = s.addExpense({ amount: 550, categoryId: 'cafe', memo: null, date: '2026-09-27' });
    expect(useBudgetStore.getState().settings.lastCategoryId).toBe('cafe');
    s.updateExpense(e.id, { amount: 600 });
    expect(useBudgetStore.getState().expenses[0].amount).toBe(600);
    const removed = s.deleteExpense(e.id)!;
    expect(useBudgetStore.getState().expenses).toHaveLength(0);
    s.restoreExpense(removed);
    s.restoreExpense(removed);
    expect(useBudgetStore.getState().expenses).toHaveLength(1);
  });

  it('localStorage に保存される', () => {
    useBudgetStore.getState().completeOnboarding(80000, 25, '2026-09-27');
    const saved = JSON.parse(mem.get('daybudget:v1')!);
    expect(saved.state.settings).toMatchObject({ monthlyBudget: 80000, closingDay: 25, onboarded: true, startDate: '2026-09-27' });
  });
});
