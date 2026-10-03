import { Link, useLocation } from "react-router-dom";

function Forbidden() {
  const location = useLocation();
  return (
    <div style={{ maxWidth: 520, margin: "80px auto", textAlign: "center", padding: 24 }}>
      <h1>403 — Access denied</h1>
      <p>
        You don&apos;t have permission to view{" "}
        <code>{location.state?.from || "this page"}</code>.
      </p>
      <p>
        <Link to="/">Back to storefront</Link> · <Link to="/admin/login">Admin login</Link>
      </p>
    </div>
  );
}

export default Forbidden;
