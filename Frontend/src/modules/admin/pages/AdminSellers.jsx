import { useEffect, useState } from "react";
import { adminUserApi } from "../api/adminApi";

const emptyForm = {
  userLoginId: "", password: "", userName: "", firstName: "",
  lastName: "", emailId: "", mobileNumber: "",
};

function AdminSellers() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = async () => {
    try {
      const res = await adminUserApi.sellers({ size: 50 });
      setItems(res.items);
      setTotal(res.total);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const res = await adminUserApi.sellers({ size: 50 });
        if (mounted) {
          setItems(res.items);
          setTotal(res.total);
        }
      } catch (err) {
        if (mounted) setError(err.message);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setOk("");
    try {
      const created = await adminUserApi.createSeller({
        userLoginId: form.userLoginId.trim(),
        password: form.password,
        userName: form.userName.trim(),
        firstName: form.firstName.trim() || form.userName.trim(),
        lastName: form.lastName.trim() || null,
        emailId: form.emailId.trim() || null,
        mobileNumber: Number(form.mobileNumber),
      });
      setForm(emptyForm);
      setOk(`Seller ${created?.userLoginId} created.`);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const set = (k, v) => setForm({ ...form, [k]: v });

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Sellers ({total})</h1>
      {error && <p className="admin-error">{error}</p>}
      {ok && <p style={{ color: "#2e7d32" }}>{ok}</p>}
      <h3>Onboard seller</h3>
      <form onSubmit={submit} className="admin-form">
        <input placeholder="Login ID *" value={form.userLoginId} onChange={(e) => set("userLoginId", e.target.value)} required />
        <input placeholder="Password * (8+, letter+digit)" type="password" value={form.password} onChange={(e) => set("password", e.target.value)} required />
        <input placeholder="Display name *" value={form.userName} onChange={(e) => set("userName", e.target.value)} required />
        <input placeholder="First name" value={form.firstName} onChange={(e) => set("firstName", e.target.value)} />
        <input placeholder="Last name" value={form.lastName} onChange={(e) => set("lastName", e.target.value)} />
        <input placeholder="Email" value={form.emailId} onChange={(e) => set("emailId", e.target.value)} />
        <input placeholder="Mobile *" value={form.mobileNumber} onChange={(e) => set("mobileNumber", e.target.value)} required />
        <button className="admin-btn">Create seller</button>
      </form>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Login</th><th>Name</th><th>Email</th><th>Mobile</th><th>Status</th></tr></thead>
          <tbody>
            {items.map((u) => (
              <tr key={u.userId}>
                <td>{u.userId}</td>
                <td>{u.userLoginId}</td>
                <td>{u.userName}</td>
                <td>{u.emailId}</td>
                <td>{u.mobileNumber}</td>
                <td>{u.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AdminSellers;
