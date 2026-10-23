import React, { useEffect, useRef, useState } from "react";
import { assetUrl } from "../api";

/**
 * Photo chooser with live preview. onChange(file | null). `current` is the existing saved image URL (shown until a new file is picked).
 * Checks type and size in the browser so people get an instant message instead of waiting for a failed upload.
 */
export default function ImagePicker({ current, onChange, fallbackNote, maxMb = 6 }) {
  const input = useRef(null);
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview); }, [preview]);

  const pick = (file) => {
    setError("");
    if (!file) return;
    if (!/^image\/(jpeg|png|webp|gif)$/.test(file.type)) { setError("Please choose a JPG, PNG, WEBP or GIF image."); return; }
    if (file.size > maxMb * 1024 * 1024) { setError(`That image is over ${maxMb} MB. Please choose a smaller one.`); return; }
    if (preview) URL.revokeObjectURL(preview);
    setPreview(URL.createObjectURL(file));
    onChange(file);
  };

  const shown = preview || (current ? assetUrl(current) : null);

  return (
    <div className="picker">
      <div
        className={`picker__drop ${shown ? "picker__drop--has" : ""}`}
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => { e.preventDefault(); pick(e.dataTransfer.files?.[0]); }}
      >
        {shown ? <img src={shown} alt="Selected product" /> : <span>Drop a photo here or choose one</span>}
      </div>
      <div className="picker__row">
        <button type="button" className="btn btn--quiet btn--sm" onClick={() => input.current?.click()}>
          {shown ? "Change photo" : "Choose photo"}
        </button>
        <input ref={input} type="file" accept="image/jpeg,image/png,image/webp,image/gif" hidden
          onChange={(e) => { pick(e.target.files?.[0]); e.target.value = ""; }} />
        {!shown && fallbackNote && <span className="field__hint">{fallbackNote}</span>}
      </div>
      {error && <div className="field__error">{error}</div>}
    </div>
  );
}
