import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import "./AdminLayout.css";

/**
 * Admin-only shell: sidebar + topbar + breadcrumb slot.
 * Visually distinct from the customer storefront on purpose.
 * Contains zero customer logic — no cart, no checkout, no product browsing.
 * Superadmin-only links (Users, Roles) render only for ROLE_SUPERADMIN.
 */
function AdminLayout() {
  const { user, role, isSuperAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/admin/login", { replace: true });
  };

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand">
          CityMall <span>Admin</span>
        </div>
        <nav className="admin-nav">
          <NavLink to="/admin/dashboard" className={({ isActive }) => (isActive ? "active" : "")}>
            Dashboard
          </NavLink>
          <NavLink to="/admin/products" className={({ isActive }) => (isActive ? "active" : "")}>
            Products
          </NavLink>
          <NavLink to="/admin/categories" className={({ isActive }) => (isActive ? "active" : "")}>
            Categories
          </NavLink>
          <NavLink to="/admin/orders" className={({ isActive }) => (isActive ? "active" : "")}>
            Orders
          </NavLink>
          <NavLink to="/admin/coupons" className={({ isActive }) => (isActive ? "active" : "")}>
            Coupons
          </NavLink>
          <NavLink to="/admin/customers" className={({ isActive }) => (isActive ? "active" : "")}>
            Customers
          </NavLink>
          <NavLink to="/admin/sellers" className={({ isActive }) => (isActive ? "active" : "")}>
            Sellers
          </NavLink>
          {isSuperAdmin && (
            <>
              <div className="admin-nav-section">Superadmin</div>
              <NavLink to="/admin/users" className={({ isActive }) => (isActive ? "active" : "")}>
                All Users
              </NavLink>
              <NavLink to="/admin/roles" className={({ isActive }) => (isActive ? "active" : "")}>
                Roles & Permissions
              </NavLink>
            </>
          )}
        </nav>
      </aside>
      <div className="admin-main">
        <header className="admin-topbar">
          <span className="admin-user">
            {user?.userName || user?.userLoginId || "Staff"}
            {role && <em style={{ marginLeft: 8, fontStyle: "normal", opacity: 0.7 }}>({role})</em>}
          </span>
          <button type="button" className="admin-logout" onClick={handleLogout}>
            Logout
          </button>
        </header>
        <main className="admin-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default AdminLayout;
