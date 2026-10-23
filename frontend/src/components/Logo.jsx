import React from "react";
import { BRAND } from "../brand";

/** Mark: a sprout growing from a bridge ("setu" means bridge). */
export function LogoMark({ size = 30 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <rect width="64" height="64" rx="15" fill="#12261B" />
      <path d="M8 46 Q32 20 56 46" fill="none" stroke="#F0A81C" strokeWidth="5" strokeLinecap="round" />
      <path d="M32 44 V26" stroke="#7FD6A0" strokeWidth="4" strokeLinecap="round" />
      <path d="M32 30 C22 30 19 22 18 18 C27 18 32 22 32 30Z" fill="#7FD6A0" />
      <path d="M32 26 C41 26 45 19 46 15 C37 15 32 19 32 26Z" fill="#7FD6A0" />
    </svg>
  );
}

export default function Logo({ light = false }) {
  return (
    <span className={`logo ${light ? "logo--light" : ""}`}>
      <LogoMark />
      <span className="logo__name">{BRAND.name}</span>
    </span>
  );
}
