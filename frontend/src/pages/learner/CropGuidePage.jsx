import React from "react";
import { Link, useParams } from "react-router-dom";
import { cropImageApi, learnerApi } from "../../api";
import { useLoad } from "../../hooks";
import { ErrorNote, Photo, Spinner } from "../../components/Ui";
import { TNAU } from "../../brand";
import { inr } from "../../format";

const STAGES = [
  ["landPreparationSteps", "Prepare the land"], ["sowingInstructions", "Sow"], ["wateringSchedule", "Water"],
  ["fertilizerPlan", "Feed the crop"], ["pestWatch", "Watch for pests"], ["harvestGuide", "Harvest"],
];

export default function CropGuidePage() {
  const { id } = useParams();
  const { data, loading, error, reload } = useLoad(async () => {
    const guide = await learnerApi.guide(id);
    let image = null;
    try { image = (await cropImageApi.byName(guide.cropName)).imageUrl; } catch { /* default image is used */ }
    return { guide, image };
  }, [id]);
  if (loading && !data) return <div className="page"><Spinner /></div>;
  if (error) return <div className="page"><ErrorNote onRetry={reload}>{error}</ErrorNote></div>;
  const { guide, image } = data;

  return (
    <div className="page page--narrow" data-role="farmer">
      <p><Link to="/learn" className="link-btn">Back to my plan</Link></p>
      <div className="guide-hero">
        <div className="guide-hero__photo"><Photo src={image} alt={guide.cropName} /></div>
        <div>
          <h1>{guide.cropName}</h1>
          <p className="muted">{guide.durationDays} days from sowing to harvest. Estimated cost {inr(guide.estimatedCostPerAcreOrUnit)} for an expected yield of {guide.estimatedYieldPerAcreOrUnit} kg.</p>
          <Link to={`/learn/profit/${guide.id}`} className="btn btn--primary">Estimate my profit</Link>
        </div>
      </div>
      <div className="steps">
        {STAGES.filter(([k]) => guide[k]).map(([k, t]) => <div className="panel" key={k}><h3>{t}</h3><p>{guide[k]}</p></div>)}
      </div>
      <h2 style={{ marginTop: "2rem" }}>Read more from TNAU</h2>
      <a className="ref-card" href={TNAU.agriGuide} target="_blank" rel="noreferrer"><strong>Crop Production Guide, Agriculture</strong><span>Field crops</span></a>
      <a className="ref-card" href={TNAU.hortiGuide} target="_blank" rel="noreferrer"><strong>Crop Production Guide, Horticulture</strong><span>Vegetables, fruits, flowers</span></a>
    </div>
  );
}
