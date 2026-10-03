import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminRoleApi, adminUserApi } from "../api/adminApi";
import { useAuth } from "../../../context/AuthContext";

/**
 * Staff landing: live identity stats from GET /api/v1/admin/dashboard/summary
 * plus user/role counts. Order/revenue tiles stay stubbed until the backend
 * wires Feign for them (backend returns 0s today).
 */
function AdminDashboard() {
  const { isSuperAdmin } = useAuth();
  const [summary, setSummary] = useState(null);
  const [roles, setRoles] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const [dash, roleList] = await Promise.all([
          adminUserApi.dashboard(),
          adminRoleApi.list().catch(() => []),
        ]);
        if (!mounted) return;
        setSummary(dash);
        setRoles(Array.isArray(roleList) ? roleList : []);
      } catch (err) {
        if (mounted) setError(err.message);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Dashboard</h1>
      {error && <p className="admin-error">{error}</p>}
      <div className="admin-stats">
        <div className="admin-card"><strong>Total customers:</strong> {summary?.totalCustomers ?? "—"}</div>
        <div className="admin-card"><strong>Blocked customers:</strong> {summary?.blockedCustomers ?? "—"}</div>
        <div className="admin-card"><strong>Orders today:</strong> {summary?.totalOrdersToday ?? 0}</div>
        <div className="admin-card"><strong>Revenue today:</strong> ₹{summary?.revenueToday ?? 0}</div>
        <div className="admin-card"><strong>Roles:</strong> {roles.length || "—"}</div>
      </div>
      <div className="admin-card">
        <strong>Quick links:</strong>
        <div className="admin-toolbar" style={{ marginTop: 8 }}>
          <Link className="admin-btn" to="/admin/products">Products</Link>
          <Link className="admin-btn" to="/admin/orders">Orders</Link>
          <Link className="admin-btn" to="/admin/customers">Customers</Link>
          <Link className="admin-btn" to="/admin/coupons">Coupons</Link>
          {isSuperAdmin && <Link className="admin-btn" to="/admin/users">All users</Link>}
        </div>
      </div>
    </div>
  );
}

export default AdminDashboard;
