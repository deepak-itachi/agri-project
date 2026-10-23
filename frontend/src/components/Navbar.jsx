import React, { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import Logo from "./Logo";
import { homeFor, ROLES, useAuth } from "../auth";
import { Avatar } from "./Ui";

export default function Navbar() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

  const logout = async () => { close(); await signOut(); navigate("/"); };

  return (
    <header className="topbar">
      <div className="topbar__inner">
        <Link to="/" className="topbar__brand" onClick={close} aria-label="AgriSetu home"><Logo /></Link>

        <button type="button" className="topbar__toggle" aria-expanded={open} aria-controls="site-menu" onClick={() => setOpen(!open)}>
          <span className="sr-only">Menu</span>
          <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>
        </button>

        <nav id="site-menu" className={`topbar__nav ${open ? "is-open" : ""}`}>
          <NavLink to="/market" onClick={close}>Marketplace</NavLink>
          <NavLink to="/learn-more" onClick={close}>Learn to farm</NavLink>
          {user ? (
            <>
              {user.role === "BUYER" && <NavLink to="/buyer/orders" onClick={close}>My orders</NavLink>}
              <Link to={homeFor(user.role)} className="topbar__user" onClick={close}>
                <Avatar name={user.fullName} size={28} />
                <span>{ROLES[user.role].label} workspace</span>
              </Link>
              <button type="button" className="btn btn--quiet btn--sm" onClick={logout}>Sign out</button>
            </>
          ) : (
            <Link to="/signin" className="btn btn--primary btn--sm" onClick={close}>Sign in</Link>
          )}
        </nav>
      </div>
    </header>
  );
}
