import type { DateKey } from './date';

export type CarryoverMode = 'distribute' | 'savings';
export type AppTheme = 'system' | 'light' | 'dark';

export interface Expense {
  id: string;
  /** 支出金額（正の整数、円） */
  amount: number;
  categoryId: string;
  memo: string | null;
  date: DateKey;
  createdAt: string;
  updatedAt: string;
}

export interface Settings {
  monthlyBudget: number;
  /** 1〜31。31 は「月末」 */
  closingDay: number;
  carryoverMode: CarryoverMode;
  theme: AppTheme;
  isPro: boolean;
  onboarded: boolean;
  lastCategoryId: string;
  /** 使い始めた日（最初の月度の予算按分に使う） */
  startDate: DateKey | null;
  createdAt: string;
  updatedAt: string;
}
