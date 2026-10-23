import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { authApi, setToken, setUnauthorizedHandler } from "./api";

const KEY = "agrisetu.session";

// Browser storage can be blocked (private windows, strict settings) - every access is wrapped.
function readSession() {
  try { return JSON.parse(localStorage.getItem(KEY) || "null"); } catch { return null; }
}
function writeSession(s) {
  try { s ? localStorage.setItem(KEY, JSON.stringify(s)) : localStorage.removeItem(KEY); } catch { /* ignore */ }
}

/** Sign-in windows are separate per account type. slug = URL part, role = value the backend knows. */
export const ROLES = {
  FARMER: { slug: "learner", role: "FARMER", label: "Learner", noun: "learner farmer", home: "/learn", signUp: "learner" },
  SELLER: { slug: "seller", role: "SELLER", label: "Seller", noun: "seller", home: "/seller", signUp: "seller" },
  BUYER: { slug: "buyer", role: "BUYER", label: "Buyer", noun: "buyer", home: "/market", signUp: "buyer" },
  ADMIN: { slug: "admin", role: "ADMIN", label: "Admin", noun: "administrator", home: "/admin", signUp: null },
};
export const roleFromSlug = (slug) => Object.values(ROLES).find((r) => r.slug === slug) || null;
export const homeFor = (role) => ROLES[role]?.home || "/";

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => {
    const s = readSession();
    if (s?.token) setToken(s.token);
    return s;
  });
  const [ready, setReady] = useState(!session);

  const clear = useCallback(() => {
    setToken(null);
    writeSession(null);
    setSession(null);
  }, []);

  const store = useCallback((resp) => {
    setToken(resp.token);
    const s = { token: resp.token, user: resp.user };
    writeSession(s);
    setSession(s);
    return resp.user;
  }, []);

  // Token expired or rejected anywhere in the app -> back to signed-out state.
  useEffect(() => { setUnauthorizedHandler(clear); }, [clear]);

  // On first load, confirm the stored token is still valid (also picks up an account the admin disabled).
  useEffect(() => {
    if (!session) return;
    let alive = true;
    authApi.me()
      .then((user) => { if (alive) setSession((s) => { const n = { ...s, user }; writeSession(n); return n; }); })
      .catch(() => {})
      .finally(() => { if (alive) setReady(true); });
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const value = useMemo(() => ({
    user: session?.user || null,
    ready,
    signIn: async (role, email, password) => store(await authApi.login({ role, email, password })),
    register: async (role, form) => store(await authApi.register(ROLES[role].signUp, form)),
    refreshUser: async () => {
      const user = await authApi.me();
      setSession((s) => { const n = { ...s, user }; writeSession(n); return n; });
      return user;
    },
    updateProfile: async (body) => {
      const user = await authApi.updateMe(body);
      setSession((s) => { const n = { ...s, user }; writeSession(n); return n; });
      return user;
    },
    signOut: async () => {
      try { await authApi.logout(); } catch { /* token may already be invalid */ }
      clear();
    },
  }), [session, ready, store, clear]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
