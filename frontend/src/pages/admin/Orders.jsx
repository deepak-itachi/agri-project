import React from "react";
import { adminApi } from "../../api";
import { useLoad } from "../../hooks";
import { Empty, ErrorNote, Pill, Spinner } from "../../components/Ui";
import { dateTime, inrPrecise, kg } from "../../format";

export default function Orders() {
  const { data, loading, error, reload } = useLoad(() => adminApi.orders(), []);
  if (loading && !data) return <Spinner />;
  if (error) return <ErrorNote onRetry={reload}>{error}</ErrorNote>;
  if (!data.length) return <Empty title="No orders yet" />;
  return (
    <div className="table-wrap"><table>
      <thead><tr><th>Order</th><th>Product</th><th>Buyer</th><th>Seller</th><th className="right">Total</th><th>Status</th></tr></thead>
      <tbody>{data.map((o) => (
        <tr key={o.id}><td>#{o.id}<div className="muted small">{dateTime(o.createdAt)}</div></td><td>{o.cropName}<div className="muted small">{kg(o.quantityKg)}</div></td>
          <td>{o.buyerName}<div className="muted small">{o.buyerEmail}</div></td><td>{o.sellerName}<div className="muted small">{o.sellerEmail}</div></td>
          <td className="right num">{inrPrecise(o.totalAmount)}</td><td><Pill value={o.status} /></td></tr>
      ))}</tbody>
    </table></div>
  );
}
