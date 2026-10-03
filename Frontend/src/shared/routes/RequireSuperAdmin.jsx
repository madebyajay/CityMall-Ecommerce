import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { ACCESS_TOKEN_KEY } from "../../api/api";

/**
 * SUPERADMIN-only route guard for platform-owner screens
 * (user directory, role/permission master, deletes).
 */
function RequireSuperAdmin({ children }) {
  const { user, role, initializing } = useAuth();
  const location = useLocation();

  if (initializing) return null;

  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (!token || !user) {
    return <Navigate to="/admin/login" state={{ from: location.pathname }} replace />;
  }
  if (role !== "SUPERADMIN") {
    return <Navigate to="/403" state={{ from: location.pathname }} replace />;
  }
  return children;
}

export default RequireSuperAdmin;
