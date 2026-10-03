import { useEffect, useState } from "react";
import { productAdminApi } from "../api/adminApi";

function AdminCategories() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ name: "", description: "" });
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      setItems(await productAdminApi.categories());
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const list = await productAdminApi.categories();
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
      if (editing) await productAdminApi.updateCategory(editing, form);
      else await productAdminApi.createCategory(form);
      setForm({ name: "", description: "" });
      setEditing(null);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const remove = async (id) => {
    if (!window.confirm(`Delete category #${id}?`)) return;
    try {
      await productAdminApi.deleteCategory(id);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Categories</h1>
      {error && <p className="admin-error">{error}</p>}
      <form onSubmit={submit} className="admin-form">
        <input placeholder="Name *" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <input placeholder="Description *" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required />
        <button className="admin-btn">{editing ? "Update" : "Create"}</button>
        {editing && <button type="button" className="admin-btn secondary" onClick={() => { setEditing(null); setForm({ name: "", description: "" }); }}>Cancel</button>}
      </form>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Actions</th></tr></thead>
          <tbody>
            {items.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.name}</td>
                <td>{c.description}</td>
                <td><div className="admin-row-actions">
                  <button className="admin-btn secondary" onClick={() => { setEditing(c.id); setForm({ name: c.name, description: c.description }); }}>Edit</button>
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

export default AdminCategories;
