import React, { useState } from "react";
import { adminApi, learnerApi } from "../../api";
import { useAction, useLoad } from "../../hooks";
import Modal from "../../components/Modal";
import { Empty, ErrorNote, Field, Spinner } from "../../components/Ui";
import { useToast } from "../../components/Toast";

const BLANK = { cropName: "", suitableLandTypes: "OPEN_FIELD", suitableScale: "SMALL_SCALE", durationDays: 90, estimatedCostPerAcreOrUnit: 0, estimatedYieldPerAcreOrUnit: 0,
  landPreparationSteps: "", sowingInstructions: "", wateringSchedule: "", fertilizerPlan: "", pestWatch: "", harvestGuide: "" };
const TEXTS = [["landPreparationSteps", "Land preparation"], ["sowingInstructions", "Sowing"], ["wateringSchedule", "Watering"], ["fertilizerPlan", "Fertiliser"], ["pestWatch", "Pest watch"], ["harvestGuide", "Harvest"]];

function GuideForm({ item, onClose, onSaved }) {
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const [f, setF] = useState({ ...BLANK, ...(item || {}) });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const save = async () => {
    if (!f.cropName.trim()) { toast("Enter the crop name.", "error"); return; }
    const body = { ...f, durationDays: Number(f.durationDays), estimatedCostPerAcreOrUnit: Number(f.estimatedCostPerAcreOrUnit), estimatedYieldPerAcreOrUnit: Number(f.estimatedYieldPerAcreOrUnit) };
    if (await run(() => (item ? adminApi.updateGuide(item.id, body) : adminApi.createGuide(body)), "Guide saved")) onSaved();
  };
  return (
    <Modal title={item ? `Edit ${item.cropName}` : "New crop guide"} onClose={onClose} wide
      footer={<><button className="btn btn--quiet" onClick={onClose}>Cancel</button><button className="btn btn--primary" disabled={busy} onClick={save}>Save guide</button></>}>
      <div className="form-row">
        <Field label="Crop name"><input value={f.cropName} onChange={set("cropName")} /></Field>
        <Field label="Days to harvest"><input type="number" value={f.durationDays} onChange={set("durationDays")} /></Field>
        <Field label="Land types" hint="Comma separated: OPEN_FIELD, TERRACE, BACKYARD, ROOFTOP_CONTAINER"><input value={f.suitableLandTypes} onChange={set("suitableLandTypes")} /></Field>
        <Field label="Scale"><select value={f.suitableScale} onChange={set("suitableScale")}><option value="SMALL_SCALE">Small</option><option value="MEDIUM_SCALE">Medium</option><option value="LARGE_SCALE">Large</option></select></Field>
        <Field label="Estimated cost (₹)"><input type="number" value={f.estimatedCostPerAcreOrUnit} onChange={set("estimatedCostPerAcreOrUnit")} /></Field>
        <Field label="Estimated yield (kg)"><input type="number" value={f.estimatedYieldPerAcreOrUnit} onChange={set("estimatedYieldPerAcreOrUnit")} /></Field>
      </div>
      {TEXTS.map(([k, l]) => <Field key={k} label={l}><textarea value={f[k] || ""} onChange={set(k)} /></Field>)}
    </Modal>
  );
}

export default function Guides() {
  const { data, loading, error, reload } = useLoad(() => learnerApi.allGuides(), []);
  const [edit, setEdit] = useState(null);
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const del = async (g) => { if (window.confirm(`Delete the ${g.cropName} guide?`) && await run(() => adminApi.deleteGuide(g.id), "Guide deleted")) reload(); };
  return (
    <>
      <div className="toolbar"><span className="muted">Learners see these guides after crop suggestions.</span><button className="btn btn--primary" onClick={() => setEdit("new")}>New crop guide</button></div>
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading && !data ? <Spinner /> : data && data.length === 0 ? <Empty title="No guides yet" /> : (
        <div className="guide-list">{(data || []).map((g) => (
          <div className="guide-row" key={g.id}><div><strong>{g.cropName}</strong><div className="muted small">{g.durationDays} days · {g.suitableScale} · {g.suitableLandTypes}</div></div>
            <div className="guide-row__actions"><button className="btn btn--quiet btn--sm" onClick={() => setEdit(g)}>Edit</button>
              <button className="btn btn--quiet btn--sm" disabled={busy} onClick={() => del(g)}>Delete</button></div></div>
        ))}</div>
      )}
      {edit && <GuideForm item={edit === "new" ? null : edit} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); reload(); }} />}
    </>
  );
}
