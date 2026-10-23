import React from "react";
import { adminApi } from "../../api";
import { useLoad } from "../../hooks";
import { ErrorNote, Spinner, Stat } from "../../components/Ui";
import { inr } from "../../format";

export default function Overview() {
  const { data: s, loading, error, reload } = useLoad(() => adminApi.stats(), []);
  if (loading && !s) return <Spinner />;
  if (error) return <ErrorNote onRetry={reload}>{error}</ErrorNote>;
  return (
    <>
      <div className="stats">
        <Stat tone="lead" label="Business done on the site" value={inr(s.totalBusiness)} note={`${s.paidOrders} paid orders of ${s.orders}`} />
        <Stat label="Signed in today" value={s.loginsToday} note={`${s.activeLast30Min} active in the last 30 min`} />
        <Stat label="Learners" value={s.learners} />
        <Stat label="Sellers" value={s.sellers} />
        <Stat label="Buyers" value={s.buyers} />
        <Stat label="Products listed" value={s.listings} note={`${s.availableListings} on sale`} />
      </div>
    </>
  );
}
