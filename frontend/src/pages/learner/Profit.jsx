import React, { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { learnerApi } from "../../api";
import { useLoad, useAction } from "../../hooks";
import { ErrorNote, Field, Spinner, Stat } from "../../components/Ui";
import { useToast } from "../../components/Toast";
import { inr, kg } from "../../format";

export default function Profit() {
  const { id } = useParams();
  const { data: guide, loading, error, reload } = useLoad(() => learnerApi.guide(id), [id]);
  const [mult, setMult] = useState("1");
  const [state, setState] = useState("Tamil Nadu");
  const [res, setRes] = useState(null);
  const toast = useToast();
  const [busy, run] = useAction(toast);

  if (loading && !guide) return <div className="page"><Spinner /></div>;
  if (error) return <div className="page"><ErrorNote onRetry={reload}>{error}</ErrorNote></div>;

  const calc = async (e) => {
    e.preventDefault();
    const m = Number(mult);
    if (!(m > 0)) { toast("Enter an area greater than zero.", "error"); return; }
    const r = await run(() => learnerApi.profit({ cropGuideId: guide.id, landAreaMultiplier: m, state }));
    if (r) setRes(r);
  };

  return (
    <div className="page page--narrow" data-role="farmer">
      <p><Link to={`/learn/crop/${guide.id}`} className="link-btn">Back to {guide.cropName} guide</Link></p>
      <div className="page__head"><h1>Profit estimate: {guide.cropName}</h1></div>
      <form className="panel" onSubmit={calc}>
        <div className="form-row">
          <Field label="Area (acres or units)" hint="1 means one standard unit of this crop."><input type="number" step="0.1" min="0.1" value={mult} onChange={(e) => setMult(e.target.value)} /></Field>
          <Field label="State, for market price"><input value={state} onChange={(e) => setState(e.target.value)} /></Field>
        </div>
        <button className="btn btn--primary" disabled={busy}>{busy ? "Calculating" : "Estimate"}</button>
      </form>
      {res && (
        <div className="panel">
          <div className="result">
            <Stat label="Estimated cost" value={inr(res.estimatedCost)} />
            <Stat label="Expected yield" value={kg(res.estimatedYieldKg)} />
            <Stat label="Market price" value={`${inr(res.marketPricePerKg)} / kg`} />
            <Stat label="Expected revenue" value={inr(res.estimatedRevenue)} />
            <Stat label="Estimated profit" value={<span className={res.estimatedProfit >= 0 ? "profit-pos" : "profit-neg"}>{inr(res.estimatedProfit)}</span>} />
          </div>
          <p className="muted">{res.note}</p>
        </div>
      )}
    </div>
  );
}
