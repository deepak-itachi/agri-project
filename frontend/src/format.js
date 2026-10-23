const inr0 = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
const inr2 = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", minimumFractionDigits: 0, maximumFractionDigits: 2 });
const num = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 1 });

export const inr = (n) => inr0.format(Number(n) || 0);
export const inrPrecise = (n) => inr2.format(Number(n) || 0);
export const kg = (n) => `${num.format(Number(n) || 0)} kg`;

export function dateTime(value) {
  if (!value) return "-";
  const d = new Date(value);
  if (isNaN(d)) return "-";
  return d.toLocaleString("en-IN", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit" });
}

export function dateOnly(value) {
  if (!value) return "-";
  const d = new Date(value);
  if (isNaN(d)) return "-";
  return d.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

export function timeAgo(value) {
  if (!value) return "never";
  const secs = Math.round((Date.now() - new Date(value).getTime()) / 1000);
  if (isNaN(secs)) return "-";
  if (secs < 60) return "just now";
  if (secs < 3600) return `${Math.floor(secs / 60)} min ago`;
  if (secs < 86400) return `${Math.floor(secs / 3600)} h ago`;
  return `${Math.floor(secs / 86400)} d ago`;
}

export const initials = (name = "") =>
  name.trim().split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() || "").join("") || "?";
