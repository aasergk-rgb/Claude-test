import { describe, expect, it } from 'vitest';
import { expensesToCsv } from './csv';

describe('expensesToCsv', () => {
  it('日付順・BOM付き・カンマや引用符をエスケープ', () => {
    const csv = expensesToCsv([
      { id: 'b', amount: 550, categoryId: 'cafe', memo: 'コーヒー, "大"', date: '2026-09-27', createdAt: '2026-09-27T08:00:00Z', updatedAt: '' },
      { id: 'a', amount: 1200, categoryId: 'food', memo: null, date: '2026-09-01', createdAt: '2026-09-01T08:00:00Z', updatedAt: '' },
    ]);
    expect(csv.startsWith('﻿日付,カテゴリ,金額,メモ\r\n')).toBe(true);
    expect(csv).toContain('2026-09-01,食費,1200,\r\n2026-09-27,カフェ・軽食,550,"コーヒー, ""大"""\r\n');
  });
});
