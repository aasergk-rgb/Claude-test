export function formatNumber(n: number): string {
  return (n < 0 ? '−' : '') + Math.abs(n).toLocaleString('ja-JP');
}

export function formatYen(n: number): string {
  return (n < 0 ? '−¥' : '¥') + Math.abs(n).toLocaleString('ja-JP');
}

export function formatSignedYen(n: number): string {
  return (n >= 0 ? '+' : '') + formatYen(n);
}

export function formatTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
}
