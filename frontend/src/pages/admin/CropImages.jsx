import React, { useState } from "react";
import { adminApi } from "../../api";
import { useAction, useLoad } from "../../hooks";
import Modal from "../../components/Modal";
import ImagePicker from "../../components/ImagePicker";
import { Empty, ErrorNote, Field, Photo, Spinner } from "../../components/Ui";
import { useToast } from "../../components/Toast";

function ImageForm({ item, onClose, onSaved }) {
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const [name, setName] = useState(item?.cropName || "");
  const [credit, setCredit] = useState(item?.sourceCredit || "");
  const [file, setFile] = useState(null);
  const save = async () => {
    if (!name.trim()) { toast("Enter the crop name.", "error"); return; }
    if (!item && !file) { toast("Choose a photo for this crop.", "error"); return; }
    const form = new FormData();
    form.append("cropName", name.trim());
    if (credit) form.append("sourceCredit", credit);
    if (file) form.append("image", file); else if (item) form.append("imageUrl", item.imageUrl);
    if (await run(() => adminApi.saveCropImage(item?.id, form), "Crop image saved")) onSaved();
  };
  return (
    <Modal title={item ? `Edit ${item.cropName}` : "Add a crop image"} onClose={onClose}
      footer={<><button className="btn btn--quiet" onClick={onClose}>Cancel</button><button className="btn btn--primary" disabled={busy} onClick={save}>Save</button></>}>
      <Field label="Crop name" hint="Must match the crop guide name to appear with suggestions."><input value={name} onChange={(e) => setName(e.target.value)} /></Field>
      <Field label="Photo"><ImagePicker current={item?.imageUrl} onChange={setFile} /></Field>
      <Field label="Photo credit"><input value={credit} onChange={(e) => setCredit(e.target.value)} /></Field>
    </Modal>
  );
}

export default function CropImages() {
  const { data, loading, error, reload } = useLoad(() => adminApi.cropImages(), []);
  const [edit, setEdit] = useState(null);
  const toast = useToast();
  const [busy, run] = useAction(toast);
  const del = async (i) => { if (window.confirm(`Delete the image for ${i.cropName}?`) && await run(() => adminApi.deleteCropImage(i.id), "Image deleted")) reload(); };
  return (
    <>
      <div className="toolbar"><p className="muted" style={{ margin: 0 }}>These photos show next to suggested crops and as the fallback on products without a seller photo.</p>
        <button className="btn btn--primary" onClick={() => setEdit("new")}>Add a crop image</button></div>
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading && !data ? <Spinner /> : data && data.length === 0 ? <Empty title="No crop images" /> : (
        <div className="mini-grid">{(data || []).map((i) => (
          <div className="mini" key={i.id}><div className="mini__photo"><Photo src={i.imageUrl} alt={i.cropName} /></div>
            <div className="mini__body"><h3>{i.cropName}</h3><span className="muted small">{i.sourceCredit || "No credit"}</span>
              <div className="mini__actions"><button className="btn btn--quiet btn--sm" onClick={() => setEdit(i)}>Edit</button>
                <button className="btn btn--quiet btn--sm" disabled={busy} onClick={() => del(i)}>Delete</button></div></div></div>
        ))}</div>
      )}
      {edit && <ImageForm item={edit === "new" ? null : edit} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); reload(); }} />}
    </>
  );
}
