import { useEffect, useRef, useState } from 'react';

const reduceMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

/** 数値が変わったときに、前の値から新しい値までカウントアニメーションする */
export function useCountUp(value: number, duration = 520): number {
  const [shown, setShown] = useState(value);
  const from = useRef(value);
  useEffect(() => {
    const start = from.current;
    from.current = value;
    if (start === value || reduceMotion()) {
      setShown(value);
      return;
    }
    const t0 = performance.now();
    let raf = 0;
    const step = (t: number) => {
      const p = Math.min(1, (t - t0) / duration);
      const k = 1 - Math.pow(1 - p, 3);
      setShown(Math.round(start + (value - start) * k));
      if (p < 1) raf = requestAnimationFrame(step);
    };
    raf = requestAnimationFrame(step);
    return () => cancelAnimationFrame(raf);
  }, [value, duration]);
  return shown;
}
