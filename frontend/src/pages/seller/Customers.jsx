import React from "react";
import { sellerApi } from "../../api";
import { useLoad } from "../../hooks";
import { Avatar, Empty, ErrorNote, Spinner } from "../../components/Ui";
import { dateTime, inr } from "../../format";

export default function Customers() {
  const { data, loading, error, reload } = useLoad(() => sellerApi.dashboard(), []);
  if (loading && !data) return <Spinner />;
  if (error) return <ErrorNote onRetry={reload}>{error}</ErrorNote>;
  const list = data.customers;
  return list.length === 0 ? <Empty title="No customers yet">People who buy from you will be listed here with their contact details.</Empty> : (
    <div className="table-wrap"><table>
      <thead><tr><th>Customer</th><th>Contact</th><th className="right">Orders</th><th className="right">Spent</th><th>Last order</th></tr></thead>
      <tbody>{list.map((c) => (
        <tr key={c.buyerId}>
          <td><div className="cell-user"><Avatar name={c.name} /><div><strong>{c.name}</strong><span className="small">{c.city || ""}</span></div></div></td>
          <td>{c.email}<div className="muted small">{c.phone}</div></td>
          <td className="right num">{c.orders}</td><td className="right num">{inr(c.totalSpent)}</td><td className="small">{dateTime(c.lastOrderAt)}</td>
        </tr>
      ))}</tbody>
    </table></div>
  );
}
