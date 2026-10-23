import React, { useMemo, useState } from "react";
import { adminApi } from "../../api";
import { useLoad } from "../../hooks";
import { Avatar, Chips, ErrorNote, Pill, RolePill, Spinner } from "../../components/Ui";
import { dateTime, timeAgo } from "../../format";

const GROUPS = [["FARMER", "Learners"], ["SELLER", "Sellers"], ["BUYER", "Buyers"]];

export default function Activity() {
  const { data, loading, error, reload } = useLoad(async () => {
    const [users, events] = await Promise.all([adminApi.users(), adminApi.loginEvents()]);
    return { users, events };
  }, []);
  const [role, setRole] = useState("ALL");

  const live = useMemo(() => {
    if (!data) return [];
    const cutoff = Date.now() - 30 * 60 * 1000;
    const latest = new Map();
    [...data.events].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt)).forEach((e) => { if (e.userId) latest.set(e.userId, e); });
    return [...latest.values()].filter((e) => (e.eventType === "LOGIN" || e.eventType === "REGISTER") && new Date(e.createdAt).getTime() > cutoff);
  }, [data]);

  if (loading && !data) return <Spinner />;
  if (error) return <ErrorNote onRetry={reload}>{error}</ErrorNote>;
  const events = data.events.filter((e) => role === "ALL" || e.role === role);

  return (
    <>
      <div className="toolbar"><span className="muted">Updated when you open or refresh this page.</span><button className="btn btn--quiet btn--sm" onClick={reload}>Refresh</button></div>
      <div className="panel">
        <div className="panel__head"><h2>Signed in now</h2><span className="muted small">last 30 minutes</span></div>
        {live.length === 0 ? <p className="muted">Nobody is signed in right now.</p> : (
          <div className="live-list">{live.map((e) => (
            <span className="live-item" key={e.userId}><span className="dot" /> {e.fullName || e.email} <RolePill role={e.role} /></span>
          ))}</div>
        )}
      </div>
      <div className="group-cols" style={{ marginTop: "1.25rem" }}>
        {GROUPS.map(([r, label]) => {
          const list = data.users.filter((u) => u.role === r).sort((a, b) => new Date(b.lastLoginAt || 0) - new Date(a.lastLoginAt || 0));
          return (
            <div className="panel group-col" key={r}>
              <h3>{label} <span className="muted small">{list.length}</span></h3>
              {list.length === 0 ? <p className="muted small">None yet.</p> : list.map((u) => (
                <div className="person" key={u.id}>
                  <div className="cell-user"><Avatar name={u.fullName} size={32} /><div><strong>{u.fullName}</strong><span className="small">{u.email}</span></div></div>
                  <div className="small muted" style={{ textAlign: "right" }}>{u.loginCount} sign-ins<br />{timeAgo(u.lastLoginAt)}</div>
                </div>
              ))}
            </div>
          );
        })}
      </div>
      <h2 style={{ margin: "1.8rem 0 0.8rem" }}>Every sign-in</h2>
      <Chips label="Filter by role" value={role} onChange={setRole} options={[{ value: "ALL", label: "Everyone" }, ...GROUPS.map(([v, l]) => ({ value: v, label: l })), { value: "ADMIN", label: "Admins" }]} />
      <div className="table-wrap"><table>
        <thead><tr><th>Who</th><th>Role</th><th>Event</th><th>When</th><th>IP address</th></tr></thead>
        <tbody>{events.length === 0 ? <tr><td colSpan="5" className="muted">No events.</td></tr> : events.slice(0, 300).map((e) => (
          <tr key={e.id}><td><strong>{e.fullName || "-"}</strong><div className="muted small">{e.email}</div></td><td><RolePill role={e.role} /></td>
            <td><Pill value={e.eventType} /></td><td className="small">{dateTime(e.createdAt)}</td><td className="small">{e.ipAddress}</td></tr>
        ))}</tbody>
      </table></div>
    </>
  );
}
