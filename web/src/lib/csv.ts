import { getCategory } from './categories';
import type { Expense } from './types';

const cell = (v: string | number) => {
  const s = String(v);
  return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

/** Excel で文字化けしないよう BOM 付きで出力する */
export function expensesToCsv(expenses: Expense[]): string {
  const rows = [...expenses]
    .sort((a, b) => a.date.localeCompare(b.date) || a.createdAt.localeCompare(b.createdAt))
    .map((e) => [e.date, getCategory(e.categoryId).name, e.amount, e.memo ?? ''].map(cell).join(','));
  return '﻿' + ['日付,カテゴリ,金額,メモ', ...rows].join('\r\n') + '\r\n';
}
