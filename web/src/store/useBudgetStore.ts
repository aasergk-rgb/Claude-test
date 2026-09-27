import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { CLOSING_END_OF_MONTH, type DateKey } from '../lib/date';
import type { Expense, Settings } from '../lib/types';

export interface NewExpense {
  amount: number;
  categoryId: string;
  memo: string | null;
  date: DateKey;
}

interface BudgetState {
  settings: Settings;
  expenses: Expense[];
  completeOnboarding: (monthlyBudget: number, closingDay: number, today: DateKey) => void;
  updateSettings: (patch: Partial<Omit<Settings, 'createdAt' | 'updatedAt'>>) => void;
  addExpense: (input: NewExpense) => Expense;
  updateExpense: (id: string, patch: Partial<NewExpense>) => void;
  deleteExpense: (id: string) => Expense | undefined;
  restoreExpense: (expense: Expense) => void;
  resetAll: () => void;
}

const now = () => new Date().toISOString();

const uuid = () =>
  typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;

export function defaultSettings(): Settings {
  const t = now();
  return {
    monthlyBudget: 50000,
    closingDay: CLOSING_END_OF_MONTH,
    carryoverMode: 'distribute',
    theme: 'system',
    isPro: false,
    onboarded: false,
    lastCategoryId: 'food',
    startDate: null,
    createdAt: t,
    updatedAt: t,
  };
}

/** 記録はこの端末のブラウザ（localStorage）にだけ保存し、外部には送らない */
export const useBudgetStore = create<BudgetState>()(
  persist(
    (set, get) => ({
      settings: defaultSettings(),
      expenses: [],

      completeOnboarding: (monthlyBudget, closingDay, today) =>
        set((s) => ({
          settings: { ...s.settings, monthlyBudget, closingDay, startDate: today, onboarded: true, updatedAt: now() },
        })),

      updateSettings: (patch) => set((s) => ({ settings: { ...s.settings, ...patch, updatedAt: now() } })),

      addExpense: (input) => {
        const t = now();
        const expense: Expense = { id: uuid(), ...input, amount: Math.round(input.amount), createdAt: t, updatedAt: t };
        set((s) => ({
          expenses: [...s.expenses, expense],
          settings: { ...s.settings, lastCategoryId: input.categoryId },
        }));
        return expense;
      },

      updateExpense: (id, patch) =>
        set((s) => ({
          expenses: s.expenses.map((e) => (e.id === id ? { ...e, ...patch, updatedAt: now() } : e)),
        })),

      deleteExpense: (id) => {
        const target = get().expenses.find((e) => e.id === id);
        set((s) => ({ expenses: s.expenses.filter((e) => e.id !== id) }));
        return target;
      },

      restoreExpense: (expense) =>
        set((s) => (s.expenses.some((e) => e.id === expense.id) ? s : { expenses: [...s.expenses, expense] })),

      resetAll: () => set({ settings: defaultSettings(), expenses: [] }),
    }),
    {
      name: 'daybudget:v1',
      version: 1,
      storage: createJSONStorage(() => localStorage),
      partialize: (s) => ({ settings: s.settings, expenses: s.expenses }),
      merge: (persisted, current) => {
        const p = (persisted ?? {}) as Partial<BudgetState>;
        return {
          ...current,
          settings: { ...current.settings, ...(p.settings ?? {}) },
          expenses: Array.isArray(p.expenses) ? p.expenses : current.expenses,
        };
      },
    },
  ),
);
