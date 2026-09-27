const PATHS: Record<string, string> = {
  sliders: '<path d="M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4"/>',
  calendar: '<rect x="3" y="4" width="18" height="18" rx="3"/><path d="M16 2v4M8 2v4M3 10h18"/>',
  left: '<path d="m15 18-6-6 6-6"/>',
  right: '<path d="m9 18 6-6-6-6"/>',
  x: '<path d="M18 6 6 18M6 6l12 12"/>',
  trash: '<path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v5M14 11v5"/>',
  lock: '<rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  minus: '<path d="M6 12h12"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  backspace: '<path d="M21 5H8l-6 7 6 7h13a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1z"/><path d="m16 9-5 6M11 9l5 6"/>',
  spark: '<path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z"/><path d="M19 17l.7 1.8 1.8.7-1.8.7L19 22l-.7-1.8-1.8-.7 1.8-.7z"/>',
  shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  tap: '<path d="M9 11V5a2 2 0 0 1 4 0v5l4.5.9a2 2 0 0 1 1.6 2.3L18 19a3 3 0 0 1-3 2.5h-3.3a3 3 0 0 1-2.4-1.2L5.6 15a1.8 1.8 0 0 1 2.7-2.4L9 13.4"/>',
  roll: '<path d="M3 12a9 9 0 0 1 15.5-6.2L21 8M21 3v5h-5M21 12a9 9 0 0 1-15.5 6.2L3 16M3 21v-5h5"/>',
  piggy: '<path d="M19 9c1.5 0 2 1 2 2v2h-2l-1.5 2.5V19h-3v-2h-4v2h-3v-3.5C5.5 14.5 4 12.9 4 11c0-3.3 3.6-6 8-6 2.7 0 5 1 6.5 2.5Z"/><path d="M15 10h.01"/>',
  'c-food': '<path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2M7 2v20M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7"/>',
  'c-cafe': '<path d="M17 8h1a4 4 0 1 1 0 8h-1"/><path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z"/><path d="M6 2v2M10 2v2M14 2v2"/>',
  'c-daily': '<path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"/><path d="M3 6h18M16 10a4 4 0 0 1-8 0"/>',
  'c-transport': '<rect x="4" y="3" width="16" height="16" rx="3"/><path d="M4 11h16M12 3v8M8 19l-2 3M18 22l-2-3M8 15h.01M16 15h.01"/>',
  'c-fun': '<rect x="2" y="6" width="20" height="12" rx="3"/><path d="M6 12h4M8 10v4M15 13h.01M18 11h.01"/>',
  'c-other': '<circle cx="5" cy="12" r="1.2"/><circle cx="12" cy="12" r="1.2"/><circle cx="19" cy="12" r="1.2"/>',
};

export type IconName = keyof typeof PATHS;

/** アイコン定義を1回だけ描画する */
export function IconSprite() {
  const symbols = Object.entries(PATHS)
    .map(([id, d]) => `<symbol id="i-${id}" viewBox="0 0 24 24">${d}</symbol>`)
    .join('');
  return <svg width="0" height="0" style={{ position: 'absolute' }} aria-hidden="true" dangerouslySetInnerHTML={{ __html: symbols }} />;
}

export function Icon({ name, size }: { name: IconName; size?: number }) {
  return (
    <svg className="i" style={size ? { width: size, height: size } : undefined} aria-hidden="true">
      <use href={`#i-${name}`} />
    </svg>
  );
}
