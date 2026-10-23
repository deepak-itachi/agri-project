import React from "react";
import { Link } from "react-router-dom";
import { buyerApi } from "../api";
import { useLoad, useAction } from "../hooks";
import { Empty, ErrorNote, Photo, Pill, Spinner } from "../components/Ui";
import { useToast } from "../components/Toast";
import { dateTime, inrPrecise, kg } from "../format";

export default function BuyerOrders() {
  const { data, loading, error, reload } = useLoad(() => buyerApi.orders(), []);
  const toast = useToast();
  const [busy, run] = useAction(toast);

  const cancel = async (o) => {
    if (!window.confirm(`Cancel order #${o.id}?`)) return;
    if (await run(() => buyerApi.cancel(o.id), "Order cancelled")) reload();
  };

  return (
    <div className="page" data-role="buyer">
      <div className="page__head"><h1>My orders</h1><p>Everything you have bought, with its current status.</p></div>
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading && !data ? <Spinner /> : data && data.length === 0 ? (
        <Empty title="No orders yet" action={<Link to="/market" className="btn btn--primary">Browse produce</Link>}>When you buy something it will show up here.</Empty>
      ) : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Product</th><th>Quantity</th><th className="right">Total</th><th>Status</th><th>Placed</th><th /></tr></thead>
            <tbody>
              {(data || []).map((o) => (
                <tr key={o.id}>
                  <td><div className="cell-user"><div className="thumb"><Photo src={o.imageUrl} alt="" /></div><div><strong>{o.cropName}</strong><span className="small">Sold by {o.sellerName}</span></div></div></td>
                  <td className="num">{kg(o.quantityKg)}</td>
                  <td className="right num">{inrPrecise(o.totalAmount)}</td>
                  <td><Pill value={o.status} /></td>
                  <td className="small">{dateTime(o.createdAt)}</td>
                  <td className="right">
                    {o.status === "PENDING_PAYMENT" && (
                      <>
                        <Link to={`/checkout/${o.id}`} className="btn btn--primary btn--sm">Pay now</Link>{" "}
                        <button className="btn btn--quiet btn--sm" disabled={busy} onClick={() => cancel(o)}>Cancel</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
