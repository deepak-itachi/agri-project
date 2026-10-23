import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { buyerApi, errorMessage, marketApi } from "../api";
import { useAuth } from "../auth";
import { useDebounced, useLoad } from "../hooks";
import ProductCard from "../components/ProductCard";
import Modal from "../components/Modal";
import { Empty, ErrorNote, Field, Photo, Spinner } from "../components/Ui";
import { useToast } from "../components/Toast";
import { inr, kg, dateOnly } from "../format";

function ProductModal({ item, onClose }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [qty, setQty] = useState("1");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const q = Number(qty);
  const valid = q > 0 && q <= item.quantityKg;
  const total = valid ? q * item.pricePerKg : 0;

  const buy = async () => {
    setError("");
    if (!user) { navigate("/signin/buyer", { state: { from: "/market" } }); return; }
    if (user.role !== "BUYER") { setError("Only buyer accounts can place orders. Sign out and sign in as a buyer."); return; }
    if (!valid) { setError(`Enter a quantity between 0 and ${item.quantityKg} kg.`); return; }
    setBusy(true);
    try {
      const order = await buyerApi.createOrder(item.id, q);
      toast("Order created. Complete the payment to confirm it.");
      navigate(`/checkout/${order.id}`);
    } catch (e) { setError(errorMessage(e)); setBusy(false); }
  };

  return (
    <Modal title={item.cropName} onClose={onClose} wide>
      <div className="product">
        <div className="product__photo"><Photo src={item.imageUrl} alt={item.cropName} /></div>
        <div>
          <div className="price">{inr(item.pricePerKg)}<small> / kg</small></div>
          <dl>
            <dt>Sold by</dt><dd>{item.sellerName}</dd>
            <dt>Available</dt><dd>{kg(item.quantityKg)}</dd>
            {item.location && (<><dt>Location</dt><dd>{item.location}</dd></>)}
            {item.availableFrom && (<><dt>Ready from</dt><dd>{dateOnly(item.availableFrom)}</dd></>)}
          </dl>
          {item.description && <p className="muted">{item.description}</p>}
          <div className="buybox">
            <ErrorNote>{error}</ErrorNote>
            <Field label="Quantity (kg)">
              <input type="number" min="0.5" step="0.5" max={item.quantityKg} value={qty} onChange={(e) => setQty(e.target.value)} />
            </Field>
            <div className="buybox__total"><span>Total</span><strong>{inr(total)}</strong></div>
            <button type="button" className="btn btn--primary btn--lg btn--block" data-role="buyer" disabled={busy || item.quantityKg <= 0} onClick={buy}>
              {user ? (busy ? "Creating order" : "Buy now") : "Sign in as buyer to buy"}
            </button>
          </div>
        </div>
      </div>
    </Modal>
  );
}

export default function Market() {
  const [text, setText] = useState("");
  const q = useDebounced(text.trim(), 300);
  const { data, loading, error, reload } = useLoad(() => marketApi.listings(q), [q]);
  const [open, setOpen] = useState(null);

  return (
    <>
      <section className="market-head">
        <div className="market-head__inner">
          <h1>Marketplace</h1>
          <p>Produce listed by farmers, with the photos they uploaded. Prices are per kg.</p>
          <div className="searchbar">
            <input type="search" placeholder="Search tomato, mango, paddy" value={text} onChange={(e) => setText(e.target.value)} aria-label="Search produce" />
          </div>
        </div>
      </section>
      <div className="page">
        <ErrorNote onRetry={reload}>{error}</ErrorNote>
        {loading && !data ? <Spinner /> : data && data.length === 0 ? (
          <Empty title={q ? `Nothing matches "${q}"` : "No produce listed yet"}>
            {q ? "Try a different crop name." : "Sellers will appear here as soon as they add a product."}
          </Empty>
        ) : (
          <div className="grid">{(data || []).map((i) => <ProductCard key={i.id} item={i} onOpen={setOpen} />)}</div>
        )}
      </div>
      {open && <ProductModal item={open} onClose={() => setOpen(null)} />}
    </>
  );
}
