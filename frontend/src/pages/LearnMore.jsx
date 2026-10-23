import React from "react";
import { Link } from "react-router-dom";
import { BRAND, TNAU } from "../brand";

export default function LearnMore() {
  return (
    <div className="page page--narrow">
      <div className="page__head">
        <h1>Learn to farm with {BRAND.name}</h1>
        <p>The learner workspace takes what you know about your land and turns it into a plan.</p>
      </div>
      <div className="steps">
        <div className="panel"><h3>Tell us about your land</h3><p>Location, size, land type (open field, terrace, backyard or rooftop), your experience and your budget.</p></div>
        <div className="panel"><h3>Add soil, water and a photo</h3><p>Upload a soil test report, describe your water source and add a photo of the plot. These make the suggestions more accurate.</p></div>
        <div className="panel"><h3>Get crop suggestions</h3><p>Each suggested crop comes with a photo, cost and yield estimate.</p></div>
        <div className="panel"><h3>Follow the stage-wise guide</h3><p>Land preparation, sowing, watering, fertiliser, pests and harvest, in order.</p></div>
        <div className="panel"><h3>Check the profit</h3><p>Compare estimated cost with expected revenue before you spend anything.</p></div>
      </div>
      <h2 style={{ marginTop: "2.2rem" }}>References we follow</h2>
      <a className="ref-card" href={TNAU.portal} target="_blank" rel="noreferrer"><strong>TNAU Agritech Portal</strong><span>Tamil Nadu Agricultural University, crop and technology information</span></a>
      <a className="ref-card" href={TNAU.agriGuide} target="_blank" rel="noreferrer"><strong>Crop Production Guide, Agriculture (PDF)</strong><span>Field crops: paddy, millets, pulses, oilseeds</span></a>
      <a className="ref-card" href={TNAU.hortiGuide} target="_blank" rel="noreferrer"><strong>Crop Production Guide, Horticulture (PDF)</strong><span>Vegetables, fruits, flowers and spices</span></a>
      <p style={{ marginTop: "2rem" }}><Link to="/signin/learner" className="btn btn--primary" data-role="farmer">Start as a learner</Link></p>
    </div>
  );
}
