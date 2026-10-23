import React, { useState } from "react";
import { sellerApi } from "../../api";
import { useAction, useLoad } from "../../hooks";
import { Chips, Empty, ErrorNote, Pill, Spinner } from "../../components/Ui";
import { useToast } from "../../components/Toast";
import { dateTime, inrPrecise, kg } from "../../format";

export default function Orders() {
  const { data, loading, error, reload } = useLoad(() => sellerApi.orders(), []);
  const [filter, setFilter] = useState("ALL");
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const rows = (data || []).filter((o) => filter === "ALL" || o.status === filter);
  const count = (s) => (data || []).filter((o) => o.status === s).length;

  const move = async (o, status) => { if (await run(() => sellerApi.setOrderStatus(o.id, status), `Marked ${status.toLowerCase()}`)) reload(); };

  return (
    <>
      <Chips label="Filter orders" value={filter} onChange={setFilter} options={[
        { value: "ALL", label: "All", count: (data || []).length },
        { value: "PAID", label: "To ship", count: count("PAID") },
        { value: "SHIPPED", label: "Shipped", count: count("SHIPPED") },
        { value: "DELIVERED", label: "Delivered", count: count("DELIVERED") },
        { value: "PENDING_PAYMENT", label: "Awaiting payment", count: count("PENDING_PAYMENT") },
      ]} />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading && !data ? <Spinner /> : rows.length === 0 ? <Empty title="No orders here" /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Order</th><th>Product</th><th>Customer</th><th className="right">Total</th><th>Status</th><th /></tr></thead>
          <tbody>{rows.map((o) => (
            <tr key={o.id}>
              <td>#{o.id}<div className="muted small">{dateTime(o.createdAt)}</div></td>
              <td>{o.cropName}<div className="muted small">{kg(o.quantityKg)}</div></td>
              <td>{o.customer?.name}<div className="muted small">{o.customer?.phone || o.customer?.email}{o.customer?.city ? `, ${o.customer.city}` : ""}</div></td>
              <td className="right num">{inrPrecise(o.totalAmount)}</td>
              <td><Pill value={o.status} /></td>
              <td className="right">
                {o.status === "PAID" && <button className="btn btn--primary btn--sm" disabled={busy} onClick={() => move(o, "SHIPPED")}>Mark shipped</button>}
                {o.status === "SHIPPED" && <button className="btn btn--primary btn--sm" disabled={busy} onClick={() => move(o, "DELIVERED")}>Mark delivered</button>}
              </td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
    </>
  );
}
