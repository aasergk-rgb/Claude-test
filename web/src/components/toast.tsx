import { create } from 'zustand';

interface ToastState {
  id: number;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
  show: (message: string, action?: { label: string; onAction: () => void }) => void;
  hide: () => void;
}

let timer: number | undefined;

export const useToast = create<ToastState>()((set) => ({
  id: 0,
  message: '',
  show: (message, action) => {
    window.clearTimeout(timer);
    set((s) => ({ id: s.id + 1, message, actionLabel: action?.label, onAction: action?.onAction }));
    timer = window.setTimeout(() => set({ message: '' }), action ? 5000 : 3000);
  },
  hide: () => set({ message: '' }),
}));

export const toast = (message: string, action?: { label: string; onAction: () => void }) =>
  useToast.getState().show(message, action);

export function Toast() {
  const { message, actionLabel, onAction, hide } = useToast();
  return (
    <div className={message ? 'toast show' : 'toast'} role="status" aria-live="polite">
      <span>{message}</span>
      {actionLabel && (
        <button
          onClick={() => {
            onAction?.();
            hide();
          }}
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}
