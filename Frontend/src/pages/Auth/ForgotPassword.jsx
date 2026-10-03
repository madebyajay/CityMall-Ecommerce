import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi, extractErrorMessage } from "../../services/authService";
import "./Auth.css";

const STEPS = { IDENTIFIER: 0, OTP: 1, DONE: 2 };

function ForgotPassword() {
  const navigate = useNavigate();
  const [step, setStep] = useState(STEPS.IDENTIFIER);
  const [identifier, setIdentifier] = useState("");
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const sendOtp = async (e) => {
    e.preventDefault();
    if (!identifier.trim()) {
      setError("Enter your login ID, email or mobile number.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      await authApi.requestOtp(identifier.trim());
      setInfo("OTP sent. It is valid for 5 minutes.");
      setStep(STEPS.OTP);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not send OTP."));
    } finally {
      setSubmitting(false);
    }
  };

  const reset = async (e) => {
    e.preventDefault();
    if (!otp.trim()) {
      setError("Enter the OTP.");
      return;
    }
    if (password.length < 8 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      setError("Min 8 chars with at least 1 letter + 1 number.");
      return;
    }
    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      await authApi.resetPassword({ identifier: identifier.trim(), otp: otp.trim(), newPassword: password });
      setStep(STEPS.DONE);
    } catch (err) {
      setError(extractErrorMessage(err, "Password reset failed."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-logo">City<span>Mall</span></Link>
        <h2 className="auth-title">Reset Password</h2>
        {error && <p className="auth-error">{error}</p>}
        {info && step !== STEPS.DONE && <p style={{ fontSize: 13, color: "#2e7d32" }}>{info}</p>}

        {step === STEPS.IDENTIFIER && (
          <form onSubmit={sendOtp} className="auth-form">
            <div className="auth-field">
              <label>Login ID / Email / Mobile</label>
              <input
                type="text"
                placeholder="Account identifier"
                value={identifier}
                onChange={(e) => { setIdentifier(e.target.value); setError(""); }}
                autoComplete="username"
              />
            </div>
            <button type="submit" className="auth-submit-btn" disabled={submitting}>
              {submitting ? "Sending..." : "Send OTP"}
            </button>
          </form>
        )}

        {step === STEPS.OTP && (
          <form onSubmit={reset} className="auth-form">
            <div className="auth-field">
              <label>OTP</label>
              <input
                type="text"
                placeholder="6-digit code"
                value={otp}
                onChange={(e) => { setOtp(e.target.value); setError(""); }}
                inputMode="numeric"
              />
            </div>
            <div className="auth-field">
              <label>New Password</label>
              <input
                type="password"
                placeholder="Min 8 chars, letter + number"
                value={password}
                onChange={(e) => { setPassword(e.target.value); setError(""); }}
                autoComplete="new-password"
              />
            </div>
            <div className="auth-field">
              <label>Confirm New Password</label>
              <input
                type="password"
                placeholder="Re-enter new password"
                value={confirm}
                onChange={(e) => { setConfirm(e.target.value); setError(""); }}
                autoComplete="new-password"
              />
            </div>
            <button type="submit" className="auth-submit-btn" disabled={submitting}>
              {submitting ? "Resetting..." : "Reset Password"}
            </button>
            <button
              type="button"
              className="auth-submit-btn"
              style={{ marginTop: 8, background: "#64748b" }}
              disabled={submitting}
              onClick={sendOtp}
            >
              Resend OTP
            </button>
          </form>
        )}

        {step === STEPS.DONE && (
          <>
            <p style={{ fontSize: 14, color: "#2e7d32" }}>
              Password reset successfully. Please sign in with your new password.
            </p>
            <button type="button" className="auth-submit-btn" onClick={() => navigate("/login")}>
              Go to Sign In
            </button>
          </>
        )}

        <p className="auth-switch">
          <Link to="/login">Back to Sign In</Link>
        </p>
      </div>
    </div>
  );
}

export default ForgotPassword;
