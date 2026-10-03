import { useEffect, useState } from "react";
import { superAdminApi, adminRoleApi } from "../api/adminApi";

function AdminRoles() {
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [roleForm, setRoleForm] = useState({ name: "", description: "" });
  const [permForm, setPermForm] = useState({ module: "IDENTITY", apiEndpoint: "", description: "" });
  const [assignRole, setAssignRole] = useState("ADMIN");
  const [assignIds, setAssignIds] = useState([]);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = async () => {
    try {
      const [r, p] = await Promise.all([adminRoleApi.list(), superAdminApi.permissions()]);
      setRoles(r);
      setPermissions(p);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const [r, p] = await Promise.all([adminRoleApi.list(), superAdminApi.permissions()]);
        if (mounted) {
          setRoles(r);
          setPermissions(p);
        }
      } catch (err) {
        if (mounted) setError(err.message);
      }
    })();
    return () => {
      mounted = false;
    };
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

  const toggleAssign = (id) =>
    setAssignIds(assignIds.includes(id) ? assignIds.filter((x) => x !== id) : [...assignIds, id]);

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Roles & Permissions</h1>
      {error && <p className="admin-error">{error}</p>}
      {ok && <p style={{ color: "#2e7d32" }}>{ok}</p>}

      <h3>Roles</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          act(() => superAdminApi.createRole({ name: roleForm.name, description: roleForm.description }),
            "Role created.");
          setRoleForm({ name: "", description: "" });
        }}
        className="admin-form"
      >
        <input placeholder="ROLE NAME *" value={roleForm.name} onChange={(e) => setRoleForm({ ...roleForm, name: e.target.value })} required />
        <input placeholder="Description" value={roleForm.description} onChange={(e) => setRoleForm({ ...roleForm, description: e.target.value })} />
        <button className="admin-btn">Create role</button>
      </form>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Name</th><th>Active</th><th>Actions</th></tr></thead>
          <tbody>
            {roles.map((r) => (
              <tr key={r.id}>
                <td>{r.id}</td>
                <td>{r.name}</td>
                <td>{String(r.activeStatus)}</td>
                <td><div className="admin-row-actions">
                  <button className="admin-btn secondary"
                    onClick={() => act(() => superAdminApi.setRoleActive(r.id, !r.activeStatus), "Role status changed.")}>
                    Enable/Disable
                  </button>
                  <button className="admin-btn danger"
                    onClick={() => window.confirm(`Delete role ${r.name}?`) &&
                      act(() => superAdminApi.deleteRole(r.id), "Role deleted.")}>
                    Delete
                  </button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h3>Permissions</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          act(() => superAdminApi.createPermission(permForm), "Permission created.");
          setPermForm({ module: "IDENTITY", apiEndpoint: "", description: "" });
        }}
        className="admin-form"
      >
        <input placeholder="Module" value={permForm.module} onChange={(e) => setPermForm({ ...permForm, module: e.target.value })} />
        <input placeholder="/api/v1/... *" value={permForm.apiEndpoint} onChange={(e) => setPermForm({ ...permForm, apiEndpoint: e.target.value })} required />
        <input placeholder="Description" value={permForm.description} onChange={(e) => setPermForm({ ...permForm, description: e.target.value })} />
        <button className="admin-btn">Create permission</button>
      </form>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Module</th><th>Endpoint</th><th>Actions</th></tr></thead>
          <tbody>
            {permissions.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td>
                <td>{p.module}</td>
                <td>{p.apiEndpoint}</td>
                <td><div className="admin-row-actions">
                  <button className="admin-btn secondary"
                    onClick={() => act(() => superAdminApi.revokePermission(assignRole, p.apiEndpoint),
                      `Revoked ${p.apiEndpoint} from ${assignRole}.`)}>
                    Revoke from {assignRole}
                  </button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h3>Assign permissions to role</h3>
      <div className="admin-toolbar">
        <select value={assignRole} onChange={(e) => setAssignRole(e.target.value)}>
          {["ADMIN", "SELLER", "CUSTOMER", ...roles.map((r) => r.name)].filter((v, i, a) => a.indexOf(v) === i).map((r) => (
            <option key={r} value={r}>{r}</option>
          ))}
        </select>
        <button className="admin-btn"
          disabled={assignIds.length === 0}
          onClick={() => {
            act(() => superAdminApi.assignPermissions(assignRole, assignIds), `Assigned to ${assignRole}.`);
            setAssignIds([]);
          }}>
          Assign selected ({assignIds.length})
        </button>
      </div>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>Select</th><th>Endpoint</th></tr></thead>
          <tbody>
            {permissions.map((p) => (
              <tr key={p.id}>
                <td><input type="checkbox" checked={assignIds.includes(p.id)} onChange={() => toggleAssign(p.id)} /></td>
                <td>{p.apiEndpoint}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AdminRoles;
