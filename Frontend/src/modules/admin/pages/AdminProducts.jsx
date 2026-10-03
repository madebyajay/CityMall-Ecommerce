import { useEffect, useState } from "react";
import { productAdminApi } from "../api/adminApi";

const emptyForm = { name: "", description: "", availableQuantity: "", price: "", categoryId: "" };

function AdminProducts() {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [keyword, setKeyword] = useState("");
  const [stock, setStock] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const load = async () => {
    try {
      const [prods, cats] = await Promise.all([
        productAdminApi.list({ keyword: keyword || undefined, size: 50 }),
        productAdminApi.categories().catch(() => []),
      ]);
      setItems(prods.items);
      setCategories(cats);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim(),
        availableQuantity: Number(form.availableQuantity),
        price: Number(form.price),
        categoryId: Number(form.categoryId),
      };
      if (editing) await productAdminApi.update(editing, payload);
      else await productAdminApi.create(payload);
      setForm(emptyForm);
      setEditing(null);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id) => {
    if (!window.confirm(`Delete product #${id}?`)) return;
    try {
      await productAdminApi.remove(id);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const toggleActive = async (p) => {
    try {
      await productAdminApi.setActive(p.id, !(p.active ?? true));
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const adjust = async (id) => {
    const delta = Number(stock[id]);
    if (!Number.isFinite(delta) || delta === 0) return;
    try {
      await productAdminApi.adjustStock(id, delta, "Manual adjustment from admin panel");
      setStock({ ...stock, [id]: "" });
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Products</h1>
      {error && <p className="admin-error">{error}</p>}

      <div className="admin-toolbar">
        <input placeholder="Search keyword + Enter" value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && load()} />
        <button className="admin-btn secondary" onClick={load}>Search</button>
      </div>

      <form onSubmit={submit} className="admin-form">
        <input placeholder="Name *" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <input placeholder="Description *" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required />
        <input placeholder="Quantity *" type="number" min="0" value={form.availableQuantity} onChange={(e) => setForm({ ...form, availableQuantity: e.target.value })} required />
        <input placeholder="Price *" type="number" min="0" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
        <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
          <option value="">Category *</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <button className="admin-btn" disabled={busy}>{editing ? "Update product" : "Create product"}</button>
        {editing && <button type="button" className="admin-btn secondary" onClick={() => { setEditing(null); setForm(emptyForm); }}>Cancel</button>}
      </form>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th>Actions</th></tr></thead>
          <tbody>
            {items.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td>
                <td>{p.name}</td>
                <td>{p.categoryName}</td>
                <td>₹{p.price}</td>
                <td>
                  {p.availableQuantity}{" "}
                  <input size="4" placeholder="±qty" value={stock[p.id] ?? ""}
                    onChange={(e) => setStock({ ...stock, [p.id]: e.target.value })} />
                  <button className="admin-btn secondary" onClick={() => adjust(p.id)}>Apply</button>
                </td>
                <td><div className="admin-row-actions">
                  <button className="admin-btn secondary" onClick={() => {
                    setEditing(p.id);
                    setForm({
                      name: p.name, description: p.description,
                      availableQuantity: p.availableQuantity, price: p.price, categoryId: p.categoryId,
                    });
                  }}>Edit</button>
                  <button className="admin-btn secondary" onClick={() => toggleActive(p)}>Enable/Disable</button>
                  <button className="admin-btn danger" onClick={() => remove(p.id)}>Delete</button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AdminProducts;
