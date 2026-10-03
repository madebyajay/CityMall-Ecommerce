import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { ACCESS_TOKEN_KEY } from "../../api/api";

/**
 * Staff route guard (ADMIN + SUPERADMIN) — the real gate (not just hidden nav links).
 * 1. Requires a valid JWT in storage.
 * 2. Requires role ADMIN or SUPERADMIN (from /me profile or JWT `role` claim).
 * Redirects BEFORE rendering any admin component, so there is no flash of admin UI.
 */
function RequireAdmin({ children }) {
  const { user, role, isStaff, initializing } = useAuth();
  const location = useLocation();

  if (initializing) return null;

  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (!token || !user) {
    return <Navigate to="/admin/login" state={{ from: location.pathname }} replace />;
  }
  if (!isStaff && role !== "ADMIN" && role !== "SUPERADMIN") {
    return <Navigate to="/403" state={{ from: location.pathname }} replace />;
  }
  return children;
}

export default RequireAdmin;
