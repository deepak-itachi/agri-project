import React from "react";
import { Link, Navigate } from "react-router-dom";
import { homeFor, useAuth } from "../auth";

const CHOICES = [
  { slug: "learner", role: "farmer", title: "Learner", text: "Plan and learn farming for your land.", photo: "leaves" },
  { slug: "seller", role: "seller", title: "Seller", text: "List crops, ship orders, see your customers.", photo: "oxen" },
  { slug: "buyer", role: "buyer", title: "Buyer", text: "Buy produce directly from farmers.", photo: "market" },
];

export default function ChooseRole() {
  const { user } = useAuth();
  if (user) return <Navigate to={homeFor(user.role)} replace />;
  return (
    <div className="choose">
      <h1>Who is signing in?</h1>
      <p>Each type of account has its own window, so what you see matches what you do.</p>
      <div className="choose__grid">
        {CHOICES.map((c) => (
          <Link key={c.slug} to={`/signin/${c.slug}`} className="door" data-role={c.role}>
            <div className="door__photo" style={{ backgroundImage: `url(/images/backgrounds/${c.photo}.jpg)` }} />
            <div className="door__body"><h2>{c.title}</h2><p>{c.text}</p><span className="door__go">Continue as {c.title.toLowerCase()}</span></div>
          </Link>
        ))}
      </div>
    </div>
  );
}
