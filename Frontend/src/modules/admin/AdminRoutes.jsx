import { Navigate, Route, Routes } from "react-router-dom";
import RequireAdmin from "../../shared/routes/RequireAdmin";
import RequireSuperAdmin from "../../shared/routes/RequireSuperAdmin";
import AdminLayout from "./layout/AdminLayout";
import AdminLogin from "./pages/AdminLogin";
import AdminDashboard from "./pages/AdminDashboard";
import AdminProducts from "./pages/AdminProducts";
import AdminCategories from "./pages/AdminCategories";
import AdminOrders from "./pages/AdminOrders";
import AdminCoupons from "./pages/AdminCoupons";
import AdminCustomers from "./pages/AdminCustomers";
import AdminSellers from "./pages/AdminSellers";
import AdminUsers from "./pages/AdminUsers";
import AdminRoles from "./pages/AdminRoles";

/**
 * Entire admin route tree. Imported via React.lazy from App.jsx so customers
 * never download this chunk. Every /admin/* page (except /admin/login)
 * renders inside AdminLayout + RequireAdmin — never inside CustomerLayout.
 * Superadmin screens add RequireSuperAdmin on top.
 */
function AdminRoutes() {
  return (
    <Routes>
      <Route path="login" element={<AdminLogin />} />
      <Route
        element={
          <RequireAdmin>
            <AdminLayout />
          </RequireAdmin>
        }
      >
        <Route path="dashboard" element={<AdminDashboard />} />
        <Route path="products" element={<AdminProducts />} />
        <Route path="categories" element={<AdminCategories />} />
        <Route path="orders" element={<AdminOrders />} />
        <Route path="coupons" element={<AdminCoupons />} />
        <Route path="customers" element={<AdminCustomers />} />
        <Route path="sellers" element={<AdminSellers />} />
        <Route
          path="users"
          element={
            <RequireSuperAdmin>
              <AdminUsers />
            </RequireSuperAdmin>
          }
        />
        <Route
          path="roles"
          element={
            <RequireSuperAdmin>
              <AdminRoles />
            </RequireSuperAdmin>
          }
        />
        <Route index element={<Navigate to="dashboard" replace />} />
      </Route>
    </Routes>
  );
}

export default AdminRoutes;
