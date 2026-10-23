import React from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { ROLES, useAuth } from "../auth";
import { Avatar } from "./Ui";

/** Layout used by the seller and admin workspaces: left rail on desktop, scrolling tab strip on phones. */
export default function PortalShell({ role, title, subtitle, items }) {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const meta = ROLES[role];

  return (
    <div className="portal" data-role={role.toLowerCase()}>
      <aside className="portal__rail">
        <div className="portal__who">
          <Avatar name={user?.fullName} size={44} />
          <div>
            <div className="portal__name">{role === "SELLER" ? (user?.businessName || user?.fullName) : user?.fullName}</div>
            <div className="portal__role">{meta.label} workspace</div>
          </div>
        </div>
        <nav className="portal__nav" aria-label={`${meta.label} sections`}>
          {items.map((it) => (
            <NavLink key={it.to} to={it.to} end={it.end} className={({ isActive }) => (isActive ? "is-active" : "")}>
              {it.label}
            </NavLink>
          ))}
        </nav>
        <button type="button" className="btn btn--quiet btn--sm portal__out"
          onClick={async () => { await signOut(); navigate("/"); }}>Sign out</button>
      </aside>
      <main className="portal__main">
        <div className="portal__head">
          <h1>{title}</h1>
          {subtitle && <p>{subtitle}</p>}
        </div>
        <Outlet />
      </main>
    </div>
  );
}
