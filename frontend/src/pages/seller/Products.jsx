import React, { useState } from "react";
import { sellerApi } from "../../api";
import { useAction, useLoad } from "../../hooks";
import Modal from "../../components/Modal";
import ImagePicker from "../../components/ImagePicker";
import { Empty, ErrorNote, Field, Photo, Pill, Spinner } from "../../components/Ui";
import { useToast } from "../../components/Toast";
import { inr, kg } from "../../format";

const BLANK = { cropName: "", quantityKg: "", pricePerKg: "", location: "", description: "", availableFrom: "", status: "AVAILABLE" };

function ProductForm({ item, onClose, onSaved }) {
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const [f, setF] = useState(item ? {
    cropName: item.cropName || "", quantityKg: item.quantityKg ?? "", pricePerKg: item.pricePerKg ?? "", location: item.location || "",
    description: item.description || "", availableFrom: item.availableFrom || "", status: item.status === "HIDDEN" ? "HIDDEN" : item.status || "AVAILABLE",
  } : BLANK);
  const [file, setFile] = useState(null);
  const [removeImage, setRemoveImage] = useState(false);
  const [err, setErr] = useState("");
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const save = async () => {
    setErr("");
    if (!f.cropName.trim()) return setErr("Enter the crop name.");
    if (!(Number(f.quantityKg) >= 0) || f.quantityKg === "") return setErr("Enter how many kg you have.");
    if (!(Number(f.pricePerKg) > 0)) return setErr("Enter a price per kg greater than zero.");
    const form = new FormData();
    form.append("cropName", f.cropName.trim());
    form.append("quantityKg", f.quantityKg);
    form.append("pricePerKg", f.pricePerKg);
    if (f.location) form.append("location", f.location);
    if (f.description) form.append("description", f.description);
    if (f.availableFrom) form.append("availableFrom", f.availableFrom);
    if (item) { form.append("status", f.status); if (removeImage && !file) form.append("removeImage", "true"); }
    if (file) form.append("image", file);
    const ok = await run(() => sellerApi.saveListing(item?.id, form), item ? "Product updated" : "Product added");
    if (ok) onSaved();
  };

  return (
    <Modal title={item ? "Edit product" : "Add a product"} onClose={onClose} wide
      footer={<><button className="btn btn--quiet" onClick={onClose}>Cancel</button><button className="btn btn--primary" disabled={busy} onClick={save}>{busy ? "Saving" : "Save product"}</button></>}>
      <ErrorNote>{err}</ErrorNote>
      <div className="form-row">
        <div>
          <Field label="Product photo" hint="Buyers will see this exact photo.">
            <ImagePicker current={removeImage ? null : item?.customImage ? item.imageUrl : null} onChange={(x) => { setFile(x); setRemoveImage(false); }}
              fallbackNote="No photo? We show a standard picture of the crop." />
          </Field>
          {item?.customImage && !file && (
            <label className="check"><input type="checkbox" checked={removeImage} onChange={(e) => setRemoveImage(e.target.checked)} /> Remove my photo</label>
          )}
        </div>
        <div>
          <Field label="Crop name"><input value={f.cropName} onChange={set("cropName")} placeholder="Tomato" /></Field>
          <div className="form-row">
            <Field label="Quantity (kg)"><input type="number" min="0" step="0.5" value={f.quantityKg} onChange={set("quantityKg")} /></Field>
            <Field label="Price per kg (₹)"><input type="number" min="0" step="0.5" value={f.pricePerKg} onChange={set("pricePerKg")} /></Field>
          </div>
          <Field label="Location"><input value={f.location} onChange={set("location")} placeholder="Village or town" /></Field>
          <Field label="Ready from"><input type="date" value={f.availableFrom} onChange={set("availableFrom")} /></Field>
        </div>
      </div>
      <Field label="Description"><textarea value={f.description} onChange={set("description")} placeholder="Variety, how it was grown, packing" /></Field>
      {item && (
        <Field label="Visibility">
          <select value={f.status} onChange={set("status")}>
            <option value="AVAILABLE">On sale</option><option value="SOLD_OUT">Sold out</option><option value="HIDDEN">Hidden from buyers</option>
          </select>
        </Field>
      )}
    </Modal>
  );
}

export default function Products() {
  const { data, loading, error, reload } = useLoad(() => sellerApi.listings(), []);
  const [editing, setEditing] = useState(null); // null | "new" | item
  const toast = useToast();
  const [busy, run] = useAction(toast);

  const hide = async (l) => {
    if (!window.confirm(`Remove ${l.cropName} from the marketplace?`)) return;
    if (await run(() => sellerApi.deleteListing(l.id), "Product removed")) reload();
  };

  return (
    <>
      <div className="toolbar">
        <p className="muted" style={{ margin: 0 }}>Photos you add here appear on the buyer marketplace straight away.</p>
        <button className="btn btn--primary" onClick={() => setEditing("new")}>Add a product</button>
      </div>
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading && !data ? <Spinner /> : data && data.length === 0 ? (
        <Empty title="You have not listed anything yet" action={<button className="btn btn--primary" onClick={() => setEditing("new")}>Add your first product</button>}>
          Add the crop, how much you have, your price and a photo.
        </Empty>
      ) : (
        <div className="mini-grid">
          {(data || []).map((l) => (
            <div className="mini" key={l.id}>
              <div className="mini__photo"><Photo src={l.imageUrl} alt={l.cropName} /><span className="mini__status"><Pill value={l.status} /></span></div>
              <div className="mini__body">
                <h3>{l.cropName}</h3>
                <span className="muted small">{kg(l.quantityKg)} · {inr(l.pricePerKg)} / kg</span>
                {!l.customImage && <span className="muted small">Using a standard crop photo</span>}
                <div className="mini__actions">
                  <button className="btn btn--quiet btn--sm" onClick={() => setEditing(l)}>Edit</button>
                  <button className="btn btn--quiet btn--sm" disabled={busy} onClick={() => hide(l)}>Remove</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      {editing && <ProductForm item={editing === "new" ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload(); }} />}
    </>
  );
}
