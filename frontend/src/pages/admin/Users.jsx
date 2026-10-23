import React, { useState } from "react";
import { adminApi } from "../../api";
import { useAction, useLoad } from "../../hooks";
import Modal from "../../components/Modal";
import { Avatar, Chips, ErrorNote, Field, Pill, RolePill, Spinner } from "../../components/Ui";
import { useToast } from "../../components/Toast";
import { dateTime } from "../../format";

function UserForm({ user, onClose, onSaved }) {
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const [f, setF] = useState({ fullName: user.fullName || "", email: user.email || "", phone: user.phone || "", city: user.city || "", businessName: user.businessName || "",
    upiId: user.upiId || "", bankAccountNumber: user.bankAccountNumber || "", bankIfsc: user.bankIfsc || "", active: user.active });
  const [pw, setPw] = useState("");
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const seller = user.role === "SELLER";

  const save = async () => { if (await run(() => adminApi.updateUser(user.id, f), "User saved")) onSaved(); };
  const reset = async () => {
    if (pw.length < 8) { toast("The new password needs at least 8 characters.", "error"); return; }
    if (await run(() => adminApi.resetPassword(user.id, pw), "Password reset")) setPw("");
  };

  return (
    <Modal title={`Edit ${user.fullName}`} onClose={onClose} wide
      footer={<><button className="btn btn--quiet" onClick={onClose}>Cancel</button><button className="btn btn--primary" disabled={busy} onClick={save}>Save changes</button></>}>
      <div className="form-row">
        <Field label="Name"><input value={f.fullName} onChange={set("fullName")} /></Field>
        <Field label="Email"><input value={f.email} onChange={set("email")} /></Field>
        <Field label="Phone"><input value={f.phone} onChange={set("phone")} /></Field>
        <Field label="City"><input value={f.city} onChange={set("city")} /></Field>
        {seller && <>
          <Field label="Shop name"><input value={f.businessName} onChange={set("businessName")} /></Field>
          <Field label="UPI ID"><input value={f.upiId} onChange={set("upiId")} /></Field>
          <Field label="Bank account"><input value={f.bankAccountNumber} onChange={set("bankAccountNumber")} /></Field>
          <Field label="IFSC"><input value={f.bankIfsc} onChange={set("bankIfsc")} /></Field>
        </>}
      </div>
      <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Account is active (untick to block sign-in)</label>
      <h3>Reset password</h3>
      <div className="form-row">
        <Field label="New password"><input type="text" value={pw} onChange={(e) => setPw(e.target.value)} autoComplete="off" /></Field>
        <div style={{ alignSelf: "end", marginBottom: "1rem" }}><button type="button" className="btn btn--quiet" disabled={busy} onClick={reset}>Set new password</button></div>
      </div>
    </Modal>
  );
}

export default function Users() {
  const [role, setRole] = useState("ALL");
  const { data, loading, error, reload } = useLoad(() => adminApi.users(role), [role]);
  const [edit, setEdit] = useState(null);
  return (
    <>
      <Chips label="Filter by role" value={role} onChange={setRole} options={[
        { value: "ALL", label: "Everyone" }, { value: "FARMER", label: "Learners" }, { value: "SELLER", label: "Sellers" }, { value: "BUYER", label: "Buyers" }, { value: "ADMIN", label: "Admins" }]} />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading && !data ? <Spinner /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>User</th><th>Role</th><th>Contact</th><th>Last sign-in</th><th>Status</th><th /></tr></thead>
          <tbody>{(data || []).map((u) => (
            <tr key={u.id}>
              <td><div className="cell-user"><Avatar name={u.fullName} /><div><strong>{u.fullName}</strong><span className="small">{u.email}</span></div></div></td>
              <td><RolePill role={u.role} /></td><td className="small">{u.phone}<div className="muted">{u.city}</div></td>
              <td className="small">{dateTime(u.lastLoginAt)}<div className="muted">{u.loginCount} sign-ins</div></td>
              <td><Pill value={u.active ? "AVAILABLE" : "HIDDEN"} tone={u.active ? "ok" : "bad"} /></td>
              <td className="right"><button className="btn btn--quiet btn--sm" onClick={() => setEdit(u)}>Edit</button></td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
      {edit && <UserForm user={edit} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); reload(); }} />}
    </>
  );
}
