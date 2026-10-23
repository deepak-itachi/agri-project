import React, { useState } from "react";
import { adminApi } from "../../api";
import { useAction, useLoad } from "../../hooks";
import Modal from "../../components/Modal";
import ImagePicker from "../../components/ImagePicker";
import { ErrorNote, Field, Photo, Pill, Spinner } from "../../components/Ui";
import { useToast } from "../../components/Toast";
import { inr, kg } from "../../format";

function ListingForm({ item, onClose, onSaved }) {
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const [f, setF] = useState({ cropName: item.cropName || "", quantityKg: item.quantityKg ?? 0, pricePerKg: item.pricePerKg ?? 0, location: item.location || "", description: item.description || "", status: item.status });
  const [file, setFile] = useState(null);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const save = async () => {
    const ok = await run(async () => {
      await adminApi.editListing(item.id, { ...f, quantityKg: Number(f.quantityKg), pricePerKg: Number(f.pricePerKg) });
      if (file) { const form = new FormData(); form.append("image", file); await adminApi.replaceListingImage(item.id, form); }
    }, "Listing saved");
    if (ok) onSaved();
  };
  return (
    <Modal title={`Edit ${item.cropName}`} onClose={onClose} wide
      footer={<><button className="btn btn--quiet" onClick={onClose}>Cancel</button><button className="btn btn--primary" disabled={busy} onClick={save}>Save changes</button></>}>
      <p className="muted small">Seller: {item.sellerName} ({item.sellerEmail})</p>
      <div className="form-row">
        <Field label="Photo"><ImagePicker current={item.imageUrl} onChange={setFile} /></Field>
        <div>
          <Field label="Crop"><input value={f.cropName} onChange={set("cropName")} /></Field>
          <div className="form-row"><Field label="Quantity (kg)"><input type="number" value={f.quantityKg} onChange={set("quantityKg")} /></Field>
            <Field label="Price per kg"><input type="number" value={f.pricePerKg} onChange={set("pricePerKg")} /></Field></div>
          <Field label="Location"><input value={f.location} onChange={set("location")} /></Field>
          <Field label="Status"><select value={f.status} onChange={set("status")}><option value="AVAILABLE">On sale</option><option value="SOLD_OUT">Sold out</option><option value="HIDDEN">Hidden</option></select></Field>
        </div>
      </div>
      <Field label="Description"><textarea value={f.description} onChange={set("description")} /></Field>
    </Modal>
  );
}

export default function Listings() {
  const { data, loading, error, reload } = useLoad(() => adminApi.listings(), []);
  const [edit, setEdit] = useState(null);
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const hide = async (l) => { if (window.confirm(`Hide ${l.cropName} from buyers?`) && await run(() => adminApi.hideListing(l.id), "Listing hidden")) reload(); };
  return (
    <>
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading && !data ? <Spinner /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Product</th><th>Seller</th><th>Stock</th><th className="right">Price</th><th>Status</th><th /></tr></thead>
          <tbody>{(data || []).map((l) => (
            <tr key={l.id}>
              <td><div className="cell-user"><div className="thumb"><Photo src={l.imageUrl} alt="" /></div><strong>{l.cropName}</strong></div></td>
              <td>{l.sellerName}<div className="muted small">{l.sellerEmail}</div></td>
              <td className="num">{kg(l.quantityKg)}</td><td className="right num">{inr(l.pricePerKg)}</td><td><Pill value={l.status} /></td>
              <td className="right"><button className="btn btn--quiet btn--sm" onClick={() => setEdit(l)}>Edit</button>{" "}
                <button className="btn btn--quiet btn--sm" disabled={busy} onClick={() => hide(l)}>Hide</button></td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
      {edit && <ListingForm item={edit} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); reload(); }} />}
    </>
  );
}
