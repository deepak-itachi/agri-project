import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { buyerApi, errorMessage } from "../api";
import { useLoad } from "../hooks";
import { ErrorNote, Photo, Spinner } from "../components/Ui";
import { inrPrecise, kg } from "../format";

function loadRazorpay() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve();
    const s = document.createElement("script");
    s.src = "https://checkout.razorpay.com/v1/checkout.js";
    s.onload = resolve;
    s.onerror = () => reject(new Error("Could not load the payment window. Check your internet connection."));
    document.body.appendChild(s);
  });
}

export default function Checkout() {
  const { orderId } = useParams();
  const { data, loading, error, reload } = useLoad(async () => {
    const [orders, cfg] = await Promise.all([buyerApi.orders(), buyerApi.paymentConfig()]);
    return { order: orders.find((o) => String(o.id) === String(orderId)), cfg };
  }, [orderId]);
  const [method, setMethod] = useState("UPI");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");
  const [done, setDone] = useState(false);

  useEffect(() => { window.scrollTo(0, 0); }, []);

  if (loading && !data) return <div className="page"><Spinner /></div>;
  if (error) return <div className="page"><ErrorNote onRetry={reload}>{error}</ErrorNote></div>;
  const { order, cfg } = data;
  if (!order) return <div className="page page--narrow"><div className="empty"><h3>Order not found</h3><Link to="/buyer/orders" className="btn btn--primary" data-role="buyer">Go to my orders</Link></div></div>;

  const paid = done || order.status !== "PENDING_PAYMENT";

  const payDemo = async () => {
    setBusy(true); setErr("");
    try { await buyerApi.demoPay(order.id, method); setDone(true); reload(); }
    catch (e) { setErr(errorMessage(e)); } finally { setBusy(false); }
  };

  const payReal = async () => {
    setBusy(true); setErr("");
    try {
      await loadRazorpay();
      const p = await buyerApi.startPayment(order.id);
      const rz = new window.Razorpay({
        key: p.razorpayKeyId,
        order_id: p.gatewayOrderId,
        amount: Math.round(p.amountRupees * 100),
        currency: "INR",
        name: "AgriSetu",
        description: `${order.cropName}, ${kg(order.quantityKg)}`,
        handler: async (resp) => {
          try {
            const r = await buyerApi.verify({
              razorpayOrderId: resp.razorpay_order_id,
              razorpayPaymentId: resp.razorpay_payment_id,
              razorpaySignature: resp.razorpay_signature,
            });
            if (r.verified) { setDone(true); reload(); } else setErr("We could not confirm this payment. If money left your account, contact support with your order number.");
          } catch (e) { setErr(errorMessage(e)); }
          setBusy(false);
        },
        modal: { ondismiss: () => setBusy(false) },
      });
      rz.open();
    } catch (e) { setErr(errorMessage(e)); setBusy(false); }
  };

  if (paid) {
    return (
      <div className="page page--narrow" data-role="buyer">
        <div className="panel success">
          <h2>{order.status === "CANCELLED" ? "This order was cancelled" : "Payment received"}</h2>
          {order.status !== "CANCELLED" && <p>Order #{order.id} for {order.cropName} ({kg(order.quantityKg)}) is confirmed. {order.sellerName} will ship it, and you can follow its status in My orders.</p>}
          <p><Link to="/buyer/orders" className="btn btn--primary">View my orders</Link> <Link to="/market" className="btn btn--quiet">Keep shopping</Link></p>
        </div>
      </div>
    );
  }

  return (
    <div className="page" data-role="buyer">
      <div className="page__head"><h1>Checkout</h1></div>
      <div className="checkout">
        <div className="panel">
          <h2>Pay for order #{order.id}</h2>
          <ErrorNote>{err}</ErrorNote>
          {cfg.demoMode ? (
            <>
              <div className="note note--warn">Demo payment. No real money moves and no card details are collected. This is for showing the project flow.</div>
              <div className="methods" role="radiogroup" aria-label="Payment method">
                {[
                  ["UPI", "UPI (GPay, PhonePe, Paytm)", "Pay from any UPI app."],
                  ["CARD", "Debit or credit card", "Visa, Mastercard, RuPay."],
                  ["NETBANKING", "Net banking", "Pay from your bank account."],
                ].map(([v, t, d]) => (
                  <label className="method" key={v}>
                    <input type="radio" name="m" checked={method === v} onChange={() => setMethod(v)} />
                    <div><strong>{t}</strong><span>{d}</span></div>
                  </label>
                ))}
              </div>
              <button className="btn btn--primary btn--lg btn--block" disabled={busy} onClick={payDemo}>{busy ? "Processing" : `Pay ${inrPrecise(order.totalAmount)} (demo)`}</button>
            </>
          ) : (
            <>
              <p className="muted">You will pay in the secure Razorpay window. Choose UPI (GPay, PhonePe, Paytm), card or net banking there.</p>
              <div className="apps"><em>GPay</em><em>PhonePe</em><em>Paytm</em><em>Cards</em><em>Net banking</em></div>
              <p />
              <button className="btn btn--primary btn--lg btn--block" disabled={busy} onClick={payReal}>{busy ? "Opening payment window" : `Pay ${inrPrecise(order.totalAmount)}`}</button>
            </>
          )}
        </div>
        <aside className="panel">
          <h3>Order summary</h3>
          <div className="summary__item">
            <div className="thumb"><Photo src={order.imageUrl} alt={order.cropName} /></div>
            <div><strong>{order.cropName}</strong><div className="muted small">Sold by {order.sellerName}</div></div>
          </div>
          <div className="summary__lines">
            <div><span>Quantity</span><span>{kg(order.quantityKg)}</span></div>
            <div><span>Price per kg</span><span>{inrPrecise(order.pricePerKg)}</span></div>
            <div className="total"><span>Total</span><span>{inrPrecise(order.totalAmount)}</span></div>
          </div>
        </aside>
      </div>
    </div>
  );
}
