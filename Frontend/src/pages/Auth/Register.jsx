import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "./Auth.css";

const initialForm = {
  userLoginId: "",
  userName: "",
  firstName: "",
  lastName: "",
  emailId: "",
  mobileNumber: "",
  password: "",
  confirmPassword: "",
};

function Register() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: "" });
    setServerError("");
  };

  const validate = () => {
    const e = {};
    if (!form.userLoginId.trim() || form.userLoginId.trim().length < 4)
      e.userLoginId = "Login ID is required (min 4 chars).";
    if (!form.userName.trim()) e.userName = "Display name is required.";
    if (!form.mobileNumber.trim() || !/^\d{10,15}$/.test(form.mobileNumber.trim()))
      e.mobileNumber = "Enter a valid mobile number.";
    if (form.emailId.trim() && !/^\S+@\S+\.\S+$/.test(form.emailId.trim()))
      e.emailId = "Enter a valid email.";
    if (form.password.length < 8 || !/[A-Za-z]/.test(form.password) || !/\d/.test(form.password))
      e.password = "Min 8 chars with at least 1 letter + 1 number.";
    if (form.password !== form.confirmPassword)
      e.confirmPassword = "Passwords do not match.";
    return e;
  };

  const handleSubmit = async (ev) => {
    ev.preventDefault();
    const v = validate();
    if (Object.keys(v).length > 0) { setErrors(v); return; }
    setSubmitting(true);
    const payload = {
      userLoginId: form.userLoginId.trim(),
      password: form.password,
      userName: form.userName.trim(),
      firstName: form.firstName.trim() || form.userName.trim(),
      lastName: form.lastName.trim() || null,
      emailId: form.emailId.trim() || null,
      mobileNumber: Number(form.mobileNumber.trim()),
    };
    const result = await register(payload);
    setSubmitting(false);
    if (result.success) navigate("/");
    else setServerError(result.message || "Registration failed.");
  };

  const field = (name, label, props = {}) => (
    <div className="auth-field">
      <label>{label}</label>
      <input name={name} value={form[name]} onChange={handleChange} {...props} />
      {errors[name] && <span className="auth-field-error">{errors[name]}</span>}
    </div>
  );

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-logo">City<span>Mall</span></Link>
        <h2 className="auth-title">Create Account</h2>
        {serverError && <p className="auth-error">{serverError}</p>}
        <form onSubmit={handleSubmit} className="auth-form">
          {field("userLoginId", "Login ID *", { type: "text", placeholder: "e.g. rahul123", autoComplete: "username" })}
          {field("userName", "Display Name *", { type: "text", placeholder: "e.g. Rahul Sharma", autoComplete: "name" })}
          <div style={{ display: "flex", gap: 12 }}>
            <div style={{ flex: 1 }}>{field("firstName", "First Name", { type: "text", placeholder: "Rahul" })}</div>
            <div style={{ flex: 1 }}>{field("lastName", "Last Name", { type: "text", placeholder: "Sharma" })}</div>
          </div>
          {field("emailId", "Email", { type: "email", placeholder: "you@example.com", autoComplete: "email" })}
          {field("mobileNumber", "Mobile Number *", { type: "tel", placeholder: "10-digit mobile", autoComplete: "tel" })}
          <div className="auth-field">
            <label>Password *</label>
            <div className="auth-password-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                placeholder="Min 8 chars, letter + number"
                value={form.password}
                onChange={handleChange}
                autoComplete="new-password"
              />
              <button type="button" className="auth-toggle-password" onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? "🙈" : "👁️"}
              </button>
            </div>
            {errors.password && <span className="auth-field-error">{errors.password}</span>}
          </div>
          <div className="auth-field">
            <label>Confirm Password *</label>
            <input
              type={showPassword ? "text" : "password"}
              name="confirmPassword"
              placeholder="Re-enter your password"
              value={form.confirmPassword}
              onChange={handleChange}
              autoComplete="new-password"
            />
            {errors.confirmPassword && <span className="auth-field-error">{errors.confirmPassword}</span>}
          </div>
          <button type="submit" className="auth-submit-btn" disabled={submitting}>
            {submitting ? "Creating account..." : "Create Account"}
          </button>
        </form>
        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}

export default Register;
