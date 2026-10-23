import React, { useState } from "react";
import { assetUrl } from "../api";
import { initials } from "../format";

export function Spinner({ label = "Loading" }) {
  return <div className="spinner-wrap" role="status"><span className="spinner" aria-hidden="true" /><span>{label}</span></div>;
}

export function Empty({ title, children, action }) {
  return (
    <div className="empty">
      <h3>{title}</h3>
      {children && <p>{children}</p>}
      {action}
    </div>
  );
}

export function ErrorNote({ children, onRetry }) {
  if (!children) return null;
  return (
    <div className="note note--error" role="alert">
      <span>{children}</span>
      {onRetry && <button type="button" className="btn btn--quiet btn--sm" onClick={onRetry}>Try again</button>}
    </div>
  );
}

const STATUS_TONE = {
  AVAILABLE: "ok", PAID: "ok", DELIVERED: "ok", LOGIN: "ok", REGISTER: "info",
  PENDING_PAYMENT: "warn", SHIPPED: "info", SOLD_OUT: "muted", HIDDEN: "muted",
  CANCELLED: "bad", LOGIN_FAILED: "bad", LOGOUT: "muted",
};
const STATUS_LABEL = {
  PENDING_PAYMENT: "Awaiting payment", SOLD_OUT: "Sold out", LOGIN_FAILED: "Failed sign-in",
  LOGIN: "Signed in", LOGOUT: "Signed out", REGISTER: "Registered",
};
export function Pill({ value, tone }) {
  const label = STATUS_LABEL[value] || String(value || "").toLowerCase().replace(/_/g, " ").replace(/^./, (c) => c.toUpperCase());
  return <span className={`pill pill--${tone || STATUS_TONE[value] || "muted"}`}>{label}</span>;
}

export function RolePill({ role }) {
  const label = role === "FARMER" ? "Learner" : role ? role.charAt(0) + role.slice(1).toLowerCase() : "-";
  return <span className={`pill pill--role role-${(role || "").toLowerCase()}`}>{label}</span>;
}

export function Avatar({ name, size = 36 }) {
  return <span className="avatar" style={{ width: size, height: size, fontSize: size * 0.38 }} aria-hidden="true">{initials(name)}</span>;
}

/** Product / crop photo with a graceful fallback if the file is missing. */
export function Photo({ src, alt = "", className = "", ratio }) {
  const [broken, setBroken] = useState(false);
  const url = broken || !src ? "/images/crops/default.jpg" : assetUrl(src);
  return (
    <img className={`photo ${className}`} src={url} alt={alt} loading="lazy" style={ratio ? { aspectRatio: ratio } : undefined}
      onError={() => setBroken(true)} />
  );
}

export function Field({ label, hint, children, error }) {
  return (
    <label className="field">
      <span className="field__label">{label}</span>
      {children}
      {hint && !error && <span className="field__hint">{hint}</span>}
      {error && <span className="field__error">{error}</span>}
    </label>
  );
}

export function Stat({ label, value, note, tone }) {
  return (
    <div className={`stat ${tone ? `stat--${tone}` : ""}`}>
      <div className="stat__label">{label}</div>
      <div className="stat__value">{value}</div>
      {note && <div className="stat__note">{note}</div>}
    </div>
  );
}

export function Chips({ options, value, onChange, label }) {
  return (
    <div className="chips" role="group" aria-label={label}>
      {options.map((o) => (
        <button key={o.value} type="button" className={`chip ${value === o.value ? "chip--on" : ""}`}
          aria-pressed={value === o.value} onClick={() => onChange(o.value)}>
          {o.label}{o.count != null && <span className="chip__count">{o.count}</span>}
        </button>
      ))}
    </div>
  );
}
