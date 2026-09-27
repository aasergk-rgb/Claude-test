import { useEffect, type ReactNode } from 'react';
import { Icon } from './Icon';

interface Props {
  open: boolean;
  title: string;
  onClose: () => void;
  headerExtra?: ReactNode;
  children: ReactNode;
}

/** 画面下から出るシート */
export function Sheet({ open, title, onClose, headerExtra, children }: Props) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    document.body.classList.add('locked');
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.classList.remove('locked');
    };
  }, [open, onClose]);

  return (
    <>
      <div className={open ? 'backdrop open' : 'backdrop'} onClick={onClose} />
      <div className={open ? 'sheet open' : 'sheet'} role="dialog" aria-modal="true" aria-label={title} aria-hidden={!open} inert={!open}>
        <div className="grab" />
        <div className="sheet-head">
          <b>{title}</b>
          {headerExtra}
          <button className="icon-btn sm" onClick={onClose} aria-label="閉じる">
            <Icon name="x" />
          </button>
        </div>
        {open && children}
      </div>
    </>
  );
}
