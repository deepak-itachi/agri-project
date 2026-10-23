import React, { useState } from "react";
import { Link } from "react-router-dom";
import { learnerApi } from "../../api";
import { useAuth } from "../../auth";
import { useAction, useLoad } from "../../hooks";
import { Empty, ErrorNote, Field, Spinner } from "../../components/Ui";
import { useToast } from "../../components/Toast";
import { dateTime } from "../../format";

export default function FarmData() {
  const { user } = useAuth();
  const fid = user.farmerId;
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const { data, loading, error, reload } = useLoad(async () => {
    if (!fid) return null;
    const [soil, water, land] = await Promise.all([learnerApi.soilReports(fid), learnerApi.waterResources(fid), learnerApi.landPhotos(fid)]);
    return { soil, water, land };
  }, [fid]);

  const [soil, setSoil] = useState({ phLevel: "", nitrogenKgPerHa: "", phosphorusKgPerHa: "", potassiumKgPerHa: "", organicCarbonPercent: "", soilTexture: "", labName: "" });
  const [soilFile, setSoilFile] = useState(null);
  const [water, setWater] = useState({ source: "BOREWELL", irrigationMethod: "DRIP", availabilityLitersPerDay: "", waterPh: "", salinityEc: "", notes: "" });
  const [landFile, setLandFile] = useState(null);
  const [caption, setCaption] = useState("");

  if (!fid) return <div className="page page--narrow"><Empty title="Describe your land first" action={<Link to="/learn/start" className="btn btn--primary">Describe my land</Link>}>Soil, water and photos are attached to your land profile.</Empty></div>;
  if (loading && !data) return <div className="page"><Spinner /></div>;
  if (error) return <div className="page"><ErrorNote onRetry={reload}>{error}</ErrorNote></div>;

  const sendSoil = async (e) => {
    e.preventDefault();
    if (!soilFile) { toast("Choose your soil test report file (PDF, PNG, JPG or WEBP).", "error"); return; }
    const form = new FormData();
    form.append("farmerId", fid); form.append("file", soilFile);
    Object.entries(soil).forEach(([k, v]) => { if (v !== "") form.append(k, v); });
    if (await run(() => learnerApi.uploadSoil(form), "Soil report saved")) { setSoilFile(null); e.target.reset(); reload(); }
  };
  const sendWater = async (e) => {
    e.preventDefault();
    const body = { farmerId: fid, source: water.source, irrigationMethod: water.irrigationMethod, notes: water.notes || null,
      availabilityLitersPerDay: water.availabilityLitersPerDay === "" ? null : Number(water.availabilityLitersPerDay),
      waterPh: water.waterPh === "" ? null : Number(water.waterPh), salinityEc: water.salinityEc === "" ? null : Number(water.salinityEc) };
    if (await run(() => learnerApi.saveWater(body), "Water details saved")) reload();
  };
  const sendLand = async (e) => {
    e.preventDefault();
    if (!landFile) { toast("Choose a photo of your land.", "error"); return; }
    const form = new FormData();
    form.append("farmerId", fid); form.append("file", landFile); if (caption) form.append("caption", caption);
    if (await run(() => learnerApi.uploadLand(form), "Land photo saved")) { setLandFile(null); setCaption(""); e.target.reset(); reload(); }
  };
  const s = (k) => (e) => setSoil({ ...soil, [k]: e.target.value });
  const w = (k) => (e) => setWater({ ...water, [k]: e.target.value });

  return (
    <div className="page page--narrow" data-role="farmer">
      <p><Link to="/learn" className="link-btn">Back to my plan</Link></p>
      <div className="page__head"><h1>Soil, water and land</h1><p>The more you add, the better the crop suggestions. Your files stay private to your account.</p></div>

      <form className="panel" onSubmit={sendSoil}>
        <h2>Soil test report</h2>
        <Field label="Report file"><input type="file" accept="application/pdf,image/png,image/jpeg,image/webp" onChange={(e) => setSoilFile(e.target.files?.[0] || null)} /></Field>
        <div className="form-row">
          <Field label="pH"><input type="number" step="0.1" value={soil.phLevel} onChange={s("phLevel")} /></Field>
          <Field label="Soil texture"><input value={soil.soilTexture} onChange={s("soilTexture")} placeholder="Red loam" /></Field>
          <Field label="Nitrogen (kg/ha)"><input type="number" value={soil.nitrogenKgPerHa} onChange={s("nitrogenKgPerHa")} /></Field>
          <Field label="Phosphorus (kg/ha)"><input type="number" value={soil.phosphorusKgPerHa} onChange={s("phosphorusKgPerHa")} /></Field>
          <Field label="Potassium (kg/ha)"><input type="number" value={soil.potassiumKgPerHa} onChange={s("potassiumKgPerHa")} /></Field>
          <Field label="Organic carbon (%)"><input type="number" step="0.01" value={soil.organicCarbonPercent} onChange={s("organicCarbonPercent")} /></Field>
        </div>
        <Field label="Lab name"><input value={soil.labName} onChange={s("labName")} /></Field>
        <button className="btn btn--primary" disabled={busy}>Upload soil report</button>
        {data.soil.length > 0 && <p className="muted small">Saved reports: {data.soil.map((r) => `${r.fileName} (${dateTime(r.uploadedAt)})`).join(", ")}</p>}
      </form>

      <form className="panel" onSubmit={sendWater}>
        <h2>Water resources</h2>
        <div className="form-row">
          <Field label="Main source"><select value={water.source} onChange={w("source")}>
            <option value="BOREWELL">Borewell</option><option value="OPEN_WELL">Open well</option><option value="CANAL">Canal</option><option value="POND_TANK">Pond or tank</option><option value="MUNICIPAL">Municipal supply</option><option value="RAINFED_ONLY">Rain only</option></select></Field>
          <Field label="Irrigation method"><select value={water.irrigationMethod} onChange={w("irrigationMethod")}>
            <option value="DRIP">Drip</option><option value="SPRINKLER">Sprinkler</option><option value="FLOOD">Flood</option><option value="MANUAL">By hand</option></select></Field>
          <Field label="Water per day (litres)"><input type="number" value={water.availabilityLitersPerDay} onChange={w("availabilityLitersPerDay")} /></Field>
          <Field label="Water pH"><input type="number" step="0.1" value={water.waterPh} onChange={w("waterPh")} /></Field>
          <Field label="Salinity EC (dS/m)"><input type="number" step="0.1" value={water.salinityEc} onChange={w("salinityEc")} /></Field>
        </div>
        <Field label="Notes"><textarea value={water.notes} onChange={w("notes")} /></Field>
        <button className="btn btn--primary" disabled={busy}>Save water details</button>
        {data.water.length > 0 && <p className="muted small">{data.water.length} entr{data.water.length === 1 ? "y" : "ies"} saved. Latest: {data.water[data.water.length - 1].source}.</p>}
      </form>

      <form className="panel" onSubmit={sendLand}>
        <h2>Photo of your land</h2>
        <Field label="Photo"><input type="file" accept="image/png,image/jpeg,image/webp" onChange={(e) => setLandFile(e.target.files?.[0] || null)} /></Field>
        <Field label="Caption"><input value={caption} onChange={(e) => setCaption(e.target.value)} placeholder="North corner, near the well" /></Field>
        <button className="btn btn--primary" disabled={busy}>Upload photo</button>
        {data.land.length > 0 && <p className="muted small">{data.land.length} photo{data.land.length === 1 ? "" : "s"} saved.</p>}
      </form>
    </div>
  );
}
