import { useEffect, useState } from 'react';
import { todayKey, type DateKey } from '../lib/date';

/** 日付が変わったら（深夜0時・アプリ復帰時）再描画する */
export function useToday(): DateKey {
  const [today, setToday] = useState(todayKey);
  useEffect(() => {
    const check = () => setToday(todayKey());
    const timer = window.setInterval(check, 30_000);
    document.addEventListener('visibilitychange', check);
    window.addEventListener('focus', check);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener('visibilitychange', check);
      window.removeEventListener('focus', check);
    };
  }, []);
  return today;
}
