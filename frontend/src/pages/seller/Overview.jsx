import React from "react";
import { Link } from "react-router-dom";
import { sellerApi } from "../../api";
import { useLoad } from "../../hooks";
import { Empty, ErrorNote, Pill, Spinner, Stat } from "../../components/Ui";
import { dateTime, inr, kg } from "../../format";

export default function Overview() {
  const { data, loading, error, reload } = useLoad(() => sellerApi.dashboard(), []);
  if (loading && !data) return <Spinner />;
  if (error) return <ErrorNote onRetry={reload}>{error}</ErrorNote>;
  const { stats, recentOrders, cropBreakdown } = data;
  const max = Math.max(1, ...cropBreakdown.map((c) => c.revenue));

  return (
    <>
      <div className="privacy-note">Buyers see only your shop name. Their details below are visible to you alone.</div>
      <div className="stats">
        <Stat tone="lead" label="Net business done" value={inr(stats.netBusiness)} note={`${stats.paidOrders} paid orders`} />
        <Stat label="Waiting for payment" value={inr(stats.pendingAmount)} />
        <Stat label="Sold so far" value={kg(stats.kgSold)} />
        <Stat label="Customers" value={stats.customers} />
        <Stat label="Products on sale" value={stats.activeListings} />
      </div>
      <div className="two-col">
        <div className="panel">
          <div className="panel__head"><h2>Recent orders</h2><Link to="/seller/orders" className="link-btn">All orders</Link></div>
          {recentOrders.length === 0 ? (
            <Empty title="No orders yet">Add a product with a good photo so buyers can find it.</Empty>
          ) : (
            <div className="table-wrap"><table>
              <thead><tr><th>Product</th><th>Customer</th><th className="right">Total</th><th>Status</th><th>When</th></tr></thead>
              <tbody>{recentOrders.map((o) => (
                <tr key={o.id}><td>{o.cropName} <span className="muted small">{kg(o.quantityKg)}</span></td><td>{o.customer?.name}</td>
                  <td className="right num">{inr(o.totalAmount)}</td><td><Pill value={o.status} /></td><td className="small">{dateTime(o.createdAt)}</td></tr>
              ))}</tbody>
            </table></div>
          )}
        </div>
        <div className="panel">
          <div className="panel__head"><h2>Earnings by crop</h2></div>
          {cropBreakdown.length === 0 ? <p className="muted">Paid orders will show here.</p> : (
            <div className="bars">{cropBreakdown.map((c) => (
              <div key={c.cropName}>
                <div className="bar__top"><strong>{c.cropName}</strong><span className="num">{inr(c.revenue)} · {kg(c.kgSold)}</span></div>
                <div className="bar__track"><div className="bar__fill" style={{ width: `${(c.revenue / max) * 100}%` }} /></div>
              </div>
            ))}</div>
          )}
        </div>
      </div>
    </>
  );
}
