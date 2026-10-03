import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "./Auth.css";

function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const redirectTo = location.state?.from || "/";

  const [form, setForm] = useState({ identifier: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [needsForce, setNeedsForce] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError("");
    setNeedsForce(false);
  };

  const doLogin = async (forcedLogin) => {
    setSubmitting(true);
    const result = await login(form.identifier, form.password, forcedLogin);
    setSubmitting(false);
    if (result.success) navigate(redirectTo, { replace: true });
    else if (result.needsForce) {
      setNeedsForce(true);
      setError(result.message || "Already logged in on another device.");
    } else setError(result.message || "Invalid credentials.");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.identifier.trim() || !form.password) {
      setError("Please fill in all fields.");
      return;
    }
    await doLogin(false);
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-logo">City<span>Mall</span></Link>
        <h2 className="auth-title">Sign In</h2>
        {error && <p className="auth-error">{error}</p>}
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="auth-field">
            <label>Login ID / Email / Mobile</label>
            <input
              type="text"
              name="identifier"
              placeholder="Login ID, email or mobile number"
              value={form.identifier}
              onChange={handleChange}
              autoComplete="username"
            />
          </div>
          <div className="auth-field">
            <label>Password</label>
            <div className="auth-password-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                placeholder="Enter your password"
                value={form.password}
                onChange={handleChange}
                autoComplete="current-password"
              />
              <button
                type="button"
                className="auth-toggle-password"
                onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? "🙈" : "👁️"}
              </button>
            </div>
            <Link to="/forgot-password" className="auth-forgot">Forgot password?</Link>
          </div>
          <button type="submit" className="auth-submit-btn" disabled={submitting}>
            {submitting ? "Signing in..." : "Sign In"}
          </button>
          {needsForce && (
            <button
              type="button"
              className="auth-submit-btn"
              style={{ marginTop: 8 }}
              disabled={submitting}
              onClick={() => doLogin(true)}
            >
              {submitting ? "Signing in..." : "Log in here & end other session"}
            </button>
          )}
        </form>
        <p className="auth-switch">
          New to CityMall? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  );
}

export default Login;
