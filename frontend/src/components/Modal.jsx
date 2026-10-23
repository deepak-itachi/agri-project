import React, { useEffect, useRef } from "react";

export default function Modal({ title, onClose, children, wide = false, footer }) {
  const box = useRef(null);

  useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const first = box.current?.querySelector("input, select, textarea, button:not(.modal__close)");
    first?.focus();
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={`modal ${wide ? "modal--wide" : ""}`} role="dialog" aria-modal="true" aria-label={title} ref={box}>
        <header className="modal__head">
          <h2>{title}</h2>
          <button type="button" className="modal__close" onClick={onClose} aria-label="Close">×</button>
        </header>
        <div className="modal__body">{children}</div>
        {footer && <footer className="modal__foot">{footer}</footer>}
      </div>
    </div>
  );
}
