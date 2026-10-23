import React from "react";
import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <div className="page page--narrow">
      <div className="empty">
        <h3>This page does not exist</h3>
        <p>The link may be old or mistyped.</p>
        <Link to="/" className="btn btn--primary">Go to the home page</Link>
      </div>
    </div>
  );
}
