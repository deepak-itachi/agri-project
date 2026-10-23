import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { homeFor, ROLES, useAuth } from "../auth";
import { Spinner } from "./Ui";

/** Only lets the listed roles in. Signed-out visitors are sent to that role's sign-in window and returned afterwards. */
export default function Guard({ roles, children }) {
  const { user, ready } = useAuth();
  const location = useLocation();

  if (!ready) return <div className="page"><Spinner label="Checking your session" /></div>;
  if (!user) {
    return <Navigate to={`/signin/${ROLES[roles[0]].slug}`} replace state={{ from: location.pathname + location.search }} />;
  }
  if (!roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />;
  return children;
}
