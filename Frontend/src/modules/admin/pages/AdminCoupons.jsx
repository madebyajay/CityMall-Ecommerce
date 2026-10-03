import { useEffect, useState } from "react";
import { orderAdminApi } from "../api/adminApi";

const emptyForm = {
  code: "", description: "", discountType: "PERCENTAGE",
  discountValue: "", minOrderAmount: "", validFrom: "", validTo: "",
};

function toLocalInput(value) {
  if (!value) return "";
  const d = new Date(value);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function AdminCoupons() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      setItems(await orderAdminApi.coupons());
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const list = await orderAdminApi.coupons();
        if (mounted) setItems(list);
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
    try {
      const payload = {
        code: form.code.trim().toUpperCase(),
        description: form.description || null,
        discountType: form.discountType,
        discountValue: Number(form.discountValue),
        minOrderAmount: form.minOrderAmount ? Number(form.minOrderAmount) : null,
        validFrom: new Date(form.validFrom).toISOString().slice(0, 19),
        validTo: new Date(form.validTo).toISOString().slice(0, 19),
      };
      if (editing) await orderAdminApi.updateCoupon(editing, payload);
      else await orderAdminApi.createCoupon(payload);
      setForm(emptyForm);
      setEditing(null);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const toggle = async (c) => {
    try {
      await orderAdminApi.setCouponActive(c.id, !c.active);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const remove = async (id) => {
    if (!window.confirm(`Delete coupon #${id}?`)) return;
    try {
      await orderAdminApi.deleteCoupon(id);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const set = (k, v) => setForm({ ...form, [k]: v });

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Coupons</h1>
      {error && <p className="admin-error">{error}</p>}
      <form onSubmit={submit} className="admin-form">
        <input placeholder="CODE *" value={form.code} onChange={(e) => set("code", e.target.value)} required />
        <input placeholder="Description" value={form.description} onChange={(e) => set("description", e.target.value)} />
        <select value={form.discountType} onChange={(e) => set("discountType", e.target.value)}>
          <option>PERCENTAGE</option>
          <option>FLAT</option>
        </select>
        <input placeholder="Discount value *" type="number" min="0.01" step="0.01" value={form.discountValue} onChange={(e) => set("discountValue", e.target.value)} required />
        <input placeholder="Min order amount" type="number" min="0" step="0.01" value={form.minOrderAmount} onChange={(e) => set("minOrderAmount", e.target.value)} />
        <input type="datetime-local" value={form.validFrom} onChange={(e) => set("validFrom", e.target.value)} required />
        <input type="datetime-local" value={form.validTo} onChange={(e) => set("validTo", e.target.value)} required />
        <button className="admin-btn">{editing ? "Update" : "Create"}</button>
        {editing && <button type="button" className="admin-btn secondary" onClick={() => { setEditing(null); setForm(emptyForm); }}>Cancel</button>}
      </form>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Code</th><th>Type</th><th>Value</th><th>Valid To</th><th>Active</th><th>Actions</th></tr></thead>
          <tbody>
            {items.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.code}</td>
                <td>{c.discountType}</td>
                <td>{c.discountValue}</td>
                <td>{c.validTo ? String(c.validTo).slice(0, 16).replace("T", " ") : ""}</td>
                <td>{String(c.active)}</td>
                <td><div className="admin-row-actions">
                  <button className="admin-btn secondary" onClick={() => {
                    setEditing(c.id);
                    setForm({
                      code: c.code, description: c.description || "", discountType: c.discountType,
                      discountValue: c.discountValue, minOrderAmount: c.minOrderAmount || "",
                      validFrom: toLocalInput(c.validFrom), validTo: toLocalInput(c.validTo),
                    });
                  }}>Edit</button>
                  <button className="admin-btn secondary" onClick={() => toggle(c)}>Enable/Disable</button>
                  <button className="admin-btn danger" onClick={() => remove(c.id)}>Delete</button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AdminCoupons;
