import { useEffect, useState } from "react";
import { superAdminApi } from "../api/adminApi";

const ROLES = ["CUSTOMER", "SELLER", "ADMIN", "SUPERADMIN"];

function AdminUsers() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState("");
  const [role, setRole] = useState("");
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = async () => {
    try {
      const res = await superAdminApi.users({ query, role, size: 50 });
      setItems(res.items);
      setTotal(res.total);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const act = async (fn, okMsg) => {
    setError("");
    setOk("");
    try {
      await fn();
      setOk(okMsg);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>All Users ({total})</h1>
      {error && <p className="admin-error">{error}</p>}
      {ok && <p style={{ color: "#2e7d32" }}>{ok}</p>}
      <div className="admin-toolbar">
        <input placeholder="Search login / email / mobile" value={query} onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && load()} />
        <select value={role} onChange={(e) => setRole(e.target.value)}>
          <option value="">All roles</option>
          {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
        <button className="admin-btn secondary" onClick={load}>Search</button>
      </div>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Login</th><th>Name</th><th>Role</th><th>Status</th><th>Role →</th><th>Block</th><th>Delete</th></tr></thead>
          <tbody>
            {items.map((u) => (
              <tr key={u.userId}>
                <td>{u.userId}</td>
                <td>{u.userLoginId}<br /><small>{u.emailId}</small></td>
                <td>{u.userName}</td>
                <td>{u.roleName}</td>
                <td>{u.status} / {String(u.activeStatus)}</td>
                <td>
                  <select defaultValue="" onChange={(e) => e.target.value &&
                    act(() => superAdminApi.updateRole(u.userId, e.target.value), `User ${u.userId} is now ${e.target.value}.`)}>
                    <option value="">Set role…</option>
                    {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                  </select>
                </td>
                <td>
                  <button className="admin-btn danger"
                    onClick={() => act(() => superAdminApi.setBlocked(u.userId, true, "Blocked from admin panel"), `User ${u.userId} blocked.`)}>
                    Block
                  </button>
                </td>
                <td>
                  <button className="admin-btn danger"
                    onClick={() => window.confirm(`Delete user ${u.userLoginId}?`) &&
                      act(() => superAdminApi.deleteUser(u.userId), `User ${u.userId} deleted.`)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AdminUsers;
