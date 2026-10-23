import React, { createContext, useCallback, useContext, useRef, useState } from "react";

const ToastContext = createContext(() => {});
export const useToast = () => useContext(ToastContext);

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const nextId = useRef(1);

  const push = useCallback((message, type = "ok") => {
    const id = nextId.current++;
    setItems((cur) => [...cur.slice(-3), { id, message, type }]);
    setTimeout(() => setItems((cur) => cur.filter((t) => t.id !== id)), type === "error" ? 6000 : 3500);
  }, []);

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {items.map((t) => (
          <div key={t.id} className={`toast toast--${t.type}`}>{t.message}</div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
