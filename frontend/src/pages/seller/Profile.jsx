import React, { useState } from "react";
import { useAuth } from "../../auth";
import { useAction } from "../../hooks";
import { ErrorNote, Field } from "../../components/Ui";
import { useToast } from "../../components/Toast";

export default function Profile() {
  const { user, updateProfile } = useAuth();
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const [f, setF] = useState({
    fullName: user.fullName || "", phone: user.phone || "", city: user.city || "", businessName: user.businessName || "",
    upiId: user.upiId || "", bankAccountNumber: user.bankAccountNumber || "", bankIfsc: user.bankIfsc || "",
  });
  const [err, setErr] = useState("");
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const save = async (e) => {
    e.preventDefault(); setErr("");
    if (!f.fullName.trim() || !f.businessName.trim()) return setErr("Name and shop name are required.");
    if (!f.upiId.trim() && !(f.bankAccountNumber.trim() && f.bankIfsc.trim())) return setErr("Keep a UPI ID, or a bank account with its IFSC code, for payouts.");
    await run(() => updateProfile(f), "Profile saved");
  };

  return (
    <form className="panel" style={{ maxWidth: 640 }} onSubmit={save}>
      <div className="privacy-note">Only your shop name is shown to buyers. Phone, email and payout details stay private.</div>
      <ErrorNote>{err}</ErrorNote>
      <div className="form-row">
        <Field label="Your name"><input value={f.fullName} onChange={set("fullName")} /></Field>
        <Field label="Shop or farm name"><input value={f.businessName} onChange={set("businessName")} /></Field>
        <Field label="Phone"><input value={f.phone} onChange={set("phone")} /></Field>
        <Field label="City or village"><input value={f.city} onChange={set("city")} /></Field>
      </div>
      <h3>Where payments go</h3>
      <Field label="UPI ID"><input value={f.upiId} onChange={set("upiId")} autoCapitalize="off" /></Field>
      <div className="form-row">
        <Field label="Bank account number"><input value={f.bankAccountNumber} onChange={set("bankAccountNumber")} /></Field>
        <Field label="IFSC code"><input value={f.bankIfsc} onChange={set("bankIfsc")} /></Field>
      </div>
      <p className="muted small">Email: {user.email}. Payouts are settled to these details by the site owner.</p>
      <button className="btn btn--primary" disabled={busy}>{busy ? "Saving" : "Save profile"}</button>
    </form>
  );
}
