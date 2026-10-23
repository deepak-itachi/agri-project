import React from "react";
import { Link } from "react-router-dom";
import Logo from "./Logo";
import { BRAND, TNAU } from "../brand";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer__inner">
        <div className="footer__brand">
          <Logo light />
          <p className="footer__kural" lang="ta">{BRAND.kural}</p>
          <p className="footer__meaning">{BRAND.kuralMeaning}</p>
        </div>
        <div className="footer__col">
          <h4>Use the site</h4>
          <Link to="/signin/learner">Learner sign in</Link>
          <Link to="/signin/seller">Seller sign in</Link>
          <Link to="/signin/buyer">Buyer sign in</Link>
          <Link to="/market">Browse produce</Link>
        </div>
        <div className="footer__col">
          <h4>Crop references</h4>
          <a href={TNAU.portal} target="_blank" rel="noreferrer">TNAU Agritech Portal</a>
          <a href={TNAU.agriGuide} target="_blank" rel="noreferrer">Crop Production Guide, Agriculture</a>
          <a href={TNAU.hortiGuide} target="_blank" rel="noreferrer">Crop Production Guide, Horticulture</a>
        </div>
      </div>
      <div className="footer__base">
        <span>{BRAND.name} is a college project. Crop information follows Tamil Nadu Agricultural University guides.</span>
        <Link to="/signin/admin">Admin sign in</Link>
      </div>
    </footer>
  );
}
