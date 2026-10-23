import React from "react";
import { Link } from "react-router-dom";
import { cropImageApi, learnerApi } from "../../api";
import { useAuth } from "../../auth";
import { useLoad } from "../../hooks";
import { Empty, ErrorNote, Photo, Spinner } from "../../components/Ui";
import { BRAND, TNAU } from "../../brand";
import { inr } from "../../format";

export default function LearnerHome() {
  const { user } = useAuth();
  const fid = user.farmerId;
  const { data, loading, error, reload } = useLoad(async () => {
    if (!fid) return null;
    const [farmer, crops, images] = await Promise.all([learnerApi.farmer(fid), learnerApi.recommend(fid), cropImageApi.all()]);
    const img = {};
    images.forEach((i) => { if (!img[i.cropName.toLowerCase()]) img[i.cropName.toLowerCase()] = i.imageUrl; });
    return { farmer, crops, img };
  }, [fid]);

  if (!fid) {
    return (
      <div className="page page--narrow" data-role="farmer">
        <div className="learn-hero"><div className="learn-hero__body">
          <h1>Welcome, {user.fullName.split(" ")[0]}</h1>
          <p>Tell us about your land and we will suggest crops that suit it.</p>
          <Link to="/learn/start" className="btn btn--primary btn--lg">Describe my land</Link>
        </div></div>
        <a className="ref-card" href={TNAU.portal} target="_blank" rel="noreferrer"><strong>TNAU Agritech Portal</strong><span>Reference material used by {BRAND.name}</span></a>
      </div>
    );
  }
  if (loading && !data) return <div className="page"><Spinner /></div>;
  if (error) return <div className="page"><ErrorNote onRetry={reload}>{error}</ErrorNote></div>;
  const { farmer, crops, img } = data;

  return (
    <div className="page" data-role="farmer">
      <div className="learn-hero"><div className="learn-hero__body">
        <h1>Your farm plan</h1>
        <p>{farmer.name}, {farmer.location}</p>
        <div className="facts">
          <span>{farmer.landSizeSqft} sq ft</span><span>{String(farmer.landType).replace(/_/g, " ").toLowerCase()}</span>
          <span>{String(farmer.scale).replace(/_/g, " ").toLowerCase()}</span><span>Budget {inr(farmer.budgetInRupees)}</span>
        </div>
        <p style={{ marginTop: "0.9rem" }}>Suggested method: {farmer.suggestedFarmingMethod}</p>
        <p><Link to="/learn/farm-data" className="btn btn--quiet btn--sm">Add soil report, water and land photo</Link></p>
      </div></div>

      <div className="section__head"><h2>Crops that suit your land</h2></div>
      {crops.length === 0 ? <Empty title="No matching crops yet">Try updating your land details.</Empty> : (
        <div className="grid">{crops.map((c) => (
          <Link key={c.id} to={`/learn/crop/${c.id}`} className="card">
            <div className="card__photo"><Photo src={img[c.cropName.toLowerCase()]} alt={c.cropName} /></div>
            <div className="card__body"><h3 className="card__title">{c.cropName}</h3>
              <span className="card__shop">{c.durationDays} days to harvest</span>
              <div className="card__row"><span className="muted small">Cost {inr(c.estimatedCostPerAcreOrUnit)}</span><span className="small" style={{ fontWeight: 600 }}>View guide</span></div>
            </div>
          </Link>
        ))}</div>
      )}
      <p style={{ marginTop: "1.5rem" }}><Link to="/learn/start" className="link-btn">Update my land details</Link></p>
    </div>
  );
}
