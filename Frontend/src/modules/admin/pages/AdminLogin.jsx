import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";

/**
 * Staff login page: own form, own submit handler (loginAdmin).
 * Reuses the single backend POST /api/v1/auth/login, then enforces
 * role ADMIN or SUPERADMIN. Other roles are rejected here before entering /admin/*.
 */
function AdminLogin() {
  const navigate = useNavigate();
  const location = useLocation();
  const { loginAdmin } = useAuth();
  const redirectTo = location.state?.from || "/admin/dashboard";

  const [form, setForm] = useState({ identifier: "", password: "" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.identifier.trim() || !form.password) {
      setError("Please fill in all fields.");
      return;
    }
    setSubmitting(true);
    const result = await loginAdmin(form.identifier, form.password);
    setSubmitting(false);
    if (result.success) navigate(redirectTo, { replace: true });
    else setError(result.message || "Invalid admin credentials.");
  };

  return (
    <div className="admin-login-page">
      <div className="admin-login-card">
        <h2>Staff Sign In</h2>
        <p style={{ fontSize: 13, color: "#64748b" }}>
          Restricted area. Admin or Superadmin role required.
        </p>
        {error && <p className="admin-error">{error}</p>}
        <form onSubmit={handleSubmit}>
          <div>
            <label htmlFor="admin-identifier">Login ID / Email</label>
            <input
              id="admin-identifier"
              type="text"
              value={form.identifier}
              onChange={(e) => setForm({ ...form, identifier: e.target.value })}
              autoComplete="username"
              placeholder="admin"
            />
          </div>
          <div>
            <label htmlFor="admin-password">Password</label>
            <input
              id="admin-password"
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              autoComplete="current-password"
              placeholder="••••••••"
            />
          </div>
          <button type="submit" disabled={submitting}>
            {submitting ? "Signing in..." : "Sign in to Admin"}
          </button>
        </form>
        <p style={{ fontSize: 13, marginTop: 14 }}>
          <Link to="/">Back to storefront</Link>
        </p>
      </div>
    </div>
  );
}

export default AdminLogin;
