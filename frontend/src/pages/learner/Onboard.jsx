import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { learnerApi } from "../../api";
import { useAuth } from "../../auth";
import { useAction } from "../../hooks";
import { ErrorNote, Field } from "../../components/Ui";
import { useToast } from "../../components/Toast";

export default function Onboard() {
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const [f, setF] = useState({ location: user.city || "", landSizeSqft: "", landType: "OPEN_FIELD", experienceLevel: "BEGINNER", budgetInRupees: "" });
  const [err, setErr] = useState("");
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault(); setErr("");
    if (!f.location.trim()) return setErr("Enter where your land is.");
    if (!(Number(f.landSizeSqft) > 0)) return setErr("Enter your land size in square feet.");
    if (!(Number(f.budgetInRupees) >= 0) || f.budgetInRupees === "") return setErr("Enter your budget in rupees.");
    const ok = await run(async () => {
      await learnerApi.onboard({ name: user.fullName, email: user.email, location: f.location.trim(), landSizeSqft: Number(f.landSizeSqft),
        landType: f.landType, experienceLevel: f.experienceLevel, budgetInRupees: Number(f.budgetInRupees) });
      await refreshUser();
    }, "Your plan is ready");
    if (ok) navigate("/learn");
  };

  return (
    <div className="page page--narrow" data-role="farmer">
      <div className="page__head"><h1>Describe your land</h1><p>We use this to suggest crops. You can add your soil report, water source and a land photo next.</p></div>
      <form className="panel" onSubmit={submit}>
        <ErrorNote>{err}</ErrorNote>
        <Field label="Location"><input value={f.location} onChange={set("location")} placeholder="Village, district" /></Field>
        <div className="form-row">
          <Field label="Land size (sq ft)" hint="1 acre is about 43,560 sq ft."><input type="number" min="1" value={f.landSizeSqft} onChange={set("landSizeSqft")} /></Field>
          <Field label="Budget (₹)"><input type="number" min="0" value={f.budgetInRupees} onChange={set("budgetInRupees")} /></Field>
        </div>
        <div className="form-row">
          <Field label="Type of land"><select value={f.landType} onChange={set("landType")}>
            <option value="OPEN_FIELD">Open field</option><option value="TERRACE">Terrace</option><option value="BACKYARD">Backyard</option><option value="ROOFTOP_CONTAINER">Rooftop containers</option></select></Field>
          <Field label="Experience"><select value={f.experienceLevel} onChange={set("experienceLevel")}>
            <option value="BEGINNER">Beginner</option><option value="HOBBYIST">Hobbyist</option><option value="SERIOUS">Serious farmer</option></select></Field>
        </div>
        <button className="btn btn--primary btn--lg" disabled={busy}>{busy ? "Working out your plan" : "Get my plan"}</button>
      </form>
    </div>
  );
}
