import { describe, expect, it } from 'vitest';
import { addDays, diffDays, eachDay, getPeriod, periodName, weekday } from './date';

describe('getPeriod', () => {
  it('締め日25日: 9/27 は 9/26〜10/25（設計書の例）', () => {
    expect(getPeriod('2026-09-27', 25)).toEqual({ start: '2026-09-26', end: '2026-10-25', days: 30 });
  });
  it('締め日25日: 9/10 は 8/26〜9/25（設計書の例）', () => {
    expect(getPeriod('2026-09-10', 25)).toEqual({ start: '2026-08-26', end: '2026-09-25', days: 31 });
  });
  it('締め日当日はその期間の最終日', () => {
    expect(getPeriod('2026-09-25', 25).end).toBe('2026-09-25');
    expect(getPeriod('2026-09-26', 25).start).toBe('2026-09-26');
  });
  it('月末締めはカレンダーどおり', () => {
    expect(getPeriod('2026-09-27', 31)).toEqual({ start: '2026-09-01', end: '2026-09-30', days: 30 });
    expect(getPeriod('2026-02-10', 31)).toEqual({ start: '2026-02-01', end: '2026-02-28', days: 28 });
    expect(getPeriod('2028-02-29', 31)).toEqual({ start: '2028-02-01', end: '2028-02-29', days: 29 });
  });
  it('締め日30日: 2月は末日に丸める', () => {
    expect(getPeriod('2026-03-01', 30)).toEqual({ start: '2026-03-01', end: '2026-03-30', days: 30 });
    expect(getPeriod('2026-02-15', 30)).toEqual({ start: '2026-01-31', end: '2026-02-28', days: 29 });
    expect(getPeriod('2026-03-31', 30)).toEqual({ start: '2026-03-31', end: '2026-04-30', days: 31 });
  });
  it('年をまたぐ', () => {
    expect(getPeriod('2026-12-28', 25)).toEqual({ start: '2026-12-26', end: '2027-01-25', days: 31 });
    expect(getPeriod('2027-01-05', 25)).toEqual({ start: '2026-12-26', end: '2027-01-25', days: 31 });
  });
  it('締め日1日', () => {
    expect(getPeriod('2026-09-01', 1)).toEqual({ start: '2026-08-02', end: '2026-09-01', days: 31 });
    expect(getPeriod('2026-09-02', 1)).toEqual({ start: '2026-09-02', end: '2026-10-01', days: 30 });
  });
});

describe('日付ユーティリティ', () => {
  it('addDays / diffDays は月・年をまたげる', () => {
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
    expect(diffDays('2026-09-27', '2026-09-30')).toBe(3);
    expect(diffDays('2026-03-28', '2026-03-30')).toBe(2);
  });
  it('eachDay', () => {
    expect(eachDay('2026-09-29', '2026-10-02')).toEqual(['2026-09-29', '2026-09-30', '2026-10-01', '2026-10-02']);
  });
  it('weekday / periodName', () => {
    expect(weekday('2026-09-27')).toBe('日');
    expect(periodName(getPeriod('2026-09-27', 25))).toBe('10月度');
  });
});
