import { ACCESS_TOKEN_KEY } from "../../api/api";

function readRoleFromToken() {
  try {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (!token) return null;
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    const raw = payload?.role || payload?.roleName || null;
    return raw ? String(raw).toUpperCase() : null;
  } catch {
    return null;
  }
}

/** Single place that normalizes role from /me profile or the JWT `role` claim. */
export function getNormalizedRole(user) {
  const raw = user?.roleName || user?.role || readRoleFromToken();
  return raw ? String(raw).toUpperCase() : null;
}
