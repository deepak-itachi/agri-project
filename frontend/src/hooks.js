import { useCallback, useEffect, useRef, useState } from "react";
import { errorMessage } from "./api";

/** Loads data on mount (and whenever deps change). Returns { data, error, loading, reload, setData }. */
export function useLoad(fn, deps = []) {
  const [state, setState] = useState({ data: null, error: "", loading: true });
  const fnRef = useRef(fn);
  fnRef.current = fn;
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const reload = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: "" }));
    return Promise.resolve()
      .then(() => fnRef.current())
      .then((data) => { if (alive.current) setState({ data, error: "", loading: false }); })
      .catch((e) => { if (alive.current) setState({ data: null, error: errorMessage(e, "Could not load this page."), loading: false }); });
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { reload(); }, deps);

  const setData = useCallback((data) => setState((s) => ({ ...s, data })), []);
  return { ...state, reload, setData };
}

export function useDebounced(value, ms = 300) {
  const [v, setV] = useState(value);
  useEffect(() => { const t = setTimeout(() => setV(value), ms); return () => clearTimeout(t); }, [value, ms]);
  return v;
}

/** Helper for save buttons: tracks busy state and shows errors through a toast. */
export function useAction(toast) {
  const [busy, setBusy] = useState(false);
  const run = useCallback(async (fn, okMessage) => {
    setBusy(true);
    try {
      const r = await fn();
      if (okMessage) toast(okMessage);
      return r === undefined ? true : r;
    } catch (e) {
      toast(errorMessage(e), "error");
      return false;
    } finally { setBusy(false); }
  }, [toast]);
  return [busy, run];
}
