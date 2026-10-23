import React, { useState } from "react";
import { Link, Navigate, useLocation, useNavigate, useParams } from "react-router-dom";
import { homeFor, roleFromSlug, useAuth } from "../auth";
import { errorMessage } from "../api";
import { ErrorNote, Field } from "../components/Ui";

const SIDE = {
  FARMER: { photo: "leaves", title: "Learn to farm, one stage at a time", points: ["Crop suggestions for your land, with photos", "Upload your soil report, water details and land photo", "Stage-wise guides based on TNAU"] },
  SELLER: { photo: "oxen", title: "Sell straight to buyers", points: ["Add your own photo to every product", "See who bought what, and your total business", "Buyers never see your phone, email or payout details"] },
  BUYER: { photo: "market", title: "Fresh produce from the farm", points: ["Real photos uploaded by the farmer", "Pay with UPI, GPay, PhonePe or card", "Track every order in one place"] },
  ADMIN: { photo: "rice-sunset", title: "Administrator sign in", points: ["See everyone who has signed in", "Edit users, products and crop images", "Review orders across the site"] },
};
const DEMO = {
  FARMER: "learner@demo.in", SELLER: "seller@demo.in", BUYER: "buyer@demo.in",
};
const EMPTY = { fullName: "", email: "", password: "", phone: "", city: "", businessName: "", upiId: "", bankAccountNumber: "", bankIfsc: "" };

export default function Auth() {
  const { role: slug } = useParams();
  const meta = roleFromSlug(slug);
  const { user, signIn, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (!meta) return <Navigate to="/signin" replace />;
  if (user) {
    const from = location.state?.from;
    return <Navigate to={user.role === meta.role && from ? from : homeFor(user.role)} replace />;
  }

  const side = SIDE[meta.role];
  const canRegister = !!meta.signUp;
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.email.trim() || !form.password) { setError("Enter your email and password."); return; }
    if (mode === "register") {
      if (!form.fullName.trim()) { setError("Enter your name."); return; }
      if (form.password.length < 8) { setError("Choose a password with at least 8 characters."); return; }
      if (meta.role === "SELLER") {
        if (!form.businessName.trim()) { setError("Enter the name buyers will see for your shop or farm."); return; }
        if (!form.upiId.trim() && !(form.bankAccountNumber.trim() && form.bankIfsc.trim())) {
          setError("Add a UPI ID, or a bank account number with its IFSC code, so we know where your payments go."); return;
        }
      }
    }
    setBusy(true);
    try {
      const u = mode === "login"
        ? await signIn(meta.role, form.email.trim(), form.password)
        : await register(meta.role, { ...form, email: form.email.trim() });
      const from = location.state?.from;
      navigate(from && u.role === meta.role ? from : homeFor(u.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err, "Could not sign you in."));
    } finally { setBusy(false); }
  };

  return (
    <div className="auth" data-role={meta.slug}>
      <aside className="auth__side" style={{ backgroundImage: `url(/images/backgrounds/${side.photo}.jpg)` }}>
        <h1>{side.title}</h1>
        <ul>{side.points.map((p) => <li key={p}>{p}</li>)}</ul>
      </aside>
      <section className="auth__main">
        <form className="auth__card" onSubmit={submit} noValidate>
          <h2>{meta.label} {mode === "login" ? "sign in" : "registration"}</h2>
          <p className="muted">{mode === "login" ? `Welcome back. Sign in to your ${meta.noun} account.` : `Create your ${meta.noun} account.`}</p>

          {canRegister && (
            <div className="auth__swap" role="group" aria-label="Sign in or register">
              <button type="button" aria-pressed={mode === "login"} onClick={() => { setMode("login"); setError(""); }}>Sign in</button>
              <button type="button" aria-pressed={mode === "register"} onClick={() => { setMode("register"); setError(""); }}>Create account</button>
            </div>
          )}

          <ErrorNote>{error}</ErrorNote>

          {mode === "register" && (
            <>
              <Field label="Full name"><input value={form.fullName} onChange={set("fullName")} autoComplete="name" /></Field>
              <div className="form-row">
                <Field label="Phone"><input value={form.phone} onChange={set("phone")} inputMode="tel" autoComplete="tel" /></Field>
                <Field label="City or village"><input value={form.city} onChange={set("city")} /></Field>
              </div>
              {meta.role === "SELLER" && (
                <>
                  <Field label="Shop or farm name" hint="Buyers see this name, not your personal details."><input value={form.businessName} onChange={set("businessName")} /></Field>
                  <Field label="UPI ID for payouts" hint="For example name@okaxis. Or fill in bank details below."><input value={form.upiId} onChange={set("upiId")} autoCapitalize="off" /></Field>
                  <div className="form-row">
                    <Field label="Bank account number"><input value={form.bankAccountNumber} onChange={set("bankAccountNumber")} inputMode="numeric" /></Field>
                    <Field label="IFSC code"><input value={form.bankIfsc} onChange={set("bankIfsc")} autoCapitalize="characters" /></Field>
                  </div>
                </>
              )}
            </>
          )}

          <Field label="Email"><input type="email" value={form.email} onChange={set("email")} autoComplete="email" /></Field>
          <Field label="Password" hint={mode === "register" ? "At least 8 characters." : undefined}>
            <input type="password" value={form.password} onChange={set("password")} autoComplete={mode === "login" ? "current-password" : "new-password"} />
          </Field>

          <button className="btn btn--primary btn--lg btn--block" disabled={busy}>
            {busy ? "Please wait" : mode === "login" ? "Sign in" : "Create account"}
          </button>

          {DEMO[meta.role] && mode === "login" && (
            <div className="demo-box">
              Trying the project? Use the demo account if the server has demo data switched on.
              <div><button type="button" className="btn btn--quiet btn--sm" onClick={() => setForm({ ...form, email: DEMO[meta.role], password: "Demo@1234" })}>Fill demo login</button></div>
            </div>
          )}

          <p className="auth__other">
            Not a {meta.noun}? <Link to="/signin">Choose another sign in</Link>
          </p>
        </form>
      </section>
    </div>
  );
}
