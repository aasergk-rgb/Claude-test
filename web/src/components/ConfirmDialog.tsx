interface Props {
  open: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({ open, title, message, confirmLabel, onConfirm, onCancel }: Props) {
  return (
    <>
      <div className={open ? 'backdrop open' : 'backdrop'} onClick={onCancel} />
      <div className={open ? 'dialog open' : 'dialog'} role="alertdialog" aria-modal="true" aria-label={title} aria-hidden={!open} inert={!open}>
        <b>{title}</b>
        <p>{message}</p>
        <div className="btns">
          <button onClick={onCancel}>キャンセル</button>
          <button className="red" onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </>
  );
}
