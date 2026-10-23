import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { BRAND, TNAU } from "../brand";
import { marketApi } from "../api";
import { useLoad } from "../hooks";
import ProductCard from "../components/ProductCard";
import { Spinner } from "../components/Ui";

const DOORS = [
  { slug: "learner", role: "farmer", title: "I want to learn farming", text: "Tell us about your land. Get crop suggestions with photos, stage-by-stage guides and a profit estimate.", go: "Start learning", photo: "leaves" },
  { slug: "seller", role: "seller", title: "I grow and sell", text: "List your harvest with your own photos, see who buys it, and track what you have earned.", go: "Open seller sign in", photo: "oxen" },
  { slug: "buyer", role: "buyer", title: "I want to buy produce", text: "Buy fresh crops straight from the farmer. Pay by UPI, GPay, PhonePe or card.", go: "Open buyer sign in", photo: "market" },
];

export default function Landing() {
  const navigate = useNavigate();
  const { data, loading } = useLoad(() => marketApi.listings(), []);
  const items = (data || []).filter((l) => l.quantityKg > 0).slice(0, 4);

  return (
    <>
      <section className="hero">
        <div className="hero__inner">
          <h1>{BRAND.tagline}.</h1>
          <p className="hero__lede">
            {BRAND.name} connects people who grow with people who buy, and teaches new farmers how to start.
            Choose how you want to use the site.
          </p>
          <div className="doors">
            {DOORS.map((d) => (
              <Link key={d.slug} to={`/signin/${d.slug}`} className="door" data-role={d.role}>
                <div className="door__photo" style={{ backgroundImage: `url(/images/backgrounds/${d.photo}.jpg)` }} />
                <div className="door__body">
                  <h2>{d.title}</h2>
                  <p>{d.text}</p>
                  <span className="door__go">{d.go}</span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="section">
        <div className="section__head">
          <div>
            <h2>Fresh on the marketplace</h2>
            <p>Every photo is uploaded by the farmer who is selling the crop.</p>
          </div>
          <Link to="/market" className="btn btn--quiet">See all produce</Link>
        </div>
        {loading ? <Spinner /> : items.length === 0 ? (
          <div className="empty"><h3>No produce listed yet</h3><p>Sellers will appear here as soon as they add their first product.</p></div>
        ) : (
          <div className="grid">
            {items.map((i) => <ProductCard key={i.id} item={i} onOpen={() => navigate("/market")} />)}
          </div>
        )}
      </section>

      <section className="section">
        <div className="section__head"><h2>How a sale moves</h2></div>
        <div className="flow">
          <div className="flow__step"><h3>Seller lists a crop</h3><p>Photo, quantity and price per kg, added from the seller workspace.</p></div>
          <div className="flow__step"><h3>Buyer sees it at once</h3><p>The same photo and price appear on the marketplace right away.</p></div>
          <div className="flow__step"><h3>Buyer pays online</h3><p>UPI apps like GPay and PhonePe, or card, through a payment gateway.</p></div>
          <div className="flow__step"><h3>Seller ships, totals update</h3><p>The seller sees the order, the customer and the total business done.</p></div>
        </div>
        <p className="privacy-line">Buyers only see the shop name. A seller's phone, email and payout details are never shown to buyers.</p>
      </section>

      <section className="section">
        <div className="learn-band">
          <div className="learn-band__photo" />
          <div className="learn-band__body">
            <h2>New to farming?</h2>
            <p>Enter your land size, soil report, water source and a photo of your plot. {BRAND.name} suggests crops and walks you through each stage, using Tamil Nadu Agricultural University guides.</p>
            <div className="learn-band__links">
              <Link to="/signin/learner" className="btn btn--primary" data-role="farmer">Create a learner account</Link>
              <a href={TNAU.portal} target="_blank" rel="noreferrer" className="btn btn--ghost">TNAU Agritech Portal</a>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
