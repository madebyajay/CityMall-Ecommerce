/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useCallback, useEffect, useState } from "react";
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY } from "../api/api";
import { authApi, extractErrorMessage, isActiveSessionError } from "../services/authService";
import { getNormalizedRole } from "../shared/auth/roles";

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function persistSession(auth) {
  if (!auth) return null;
  if (auth.accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
  if (auth.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
  if (auth.user) localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
  return auth.user ?? null;
}

function clearSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);
  // loading=true only during the initial session restore; per-action loading lives in pages.
  const [initializing, setInitializing] = useState(
    () => !!localStorage.getItem(ACCESS_TOKEN_KEY)
  );

  // On app start: if tokens exist, validate via GET /api/v1/auth/me.
  // If access token expired, the api interceptor silently refreshes first.
  useEffect(() => {
    if (!localStorage.getItem(ACCESS_TOKEN_KEY)) {
      setInitializing(false);
      return;
    }
    authApi
      .me()
      .then((profile) => {
        if (profile) {
          localStorage.setItem(USER_KEY, JSON.stringify(profile));
          setUser(profile);
        }
      })
      .catch(() => {
        clearSession();
        setUser(null);
      })
      .finally(() => setInitializing(false));
  }, []);

  const login = useCallback(async (identifier, password, forcedLogin = false) => {
    try {
      const auth = await authApi.login({ identifier: identifier.trim(), password, forcedLogin });
      const profile = persistSession(auth);
      setUser(profile);
      return { success: true, forced: auth?.forced === true };
    } catch (err) {
      if (!forcedLogin && isActiveSessionError(err)) {
        return {
          success: false,
          needsForce: true,
          message: extractErrorMessage(err, "Already logged in on another device."),
        };
      }
      return { success: false, message: extractErrorMessage(err, "Invalid credentials.") };
    }
  }, []);

  const register = useCallback(async (payload) => {
    try {
      const auth = await authApi.register(payload);
      const profile = persistSession(auth);
      setUser(profile);
      return { success: true };
    } catch (err) {
      return { success: false, message: extractErrorMessage(err, "Registration failed.") };
    }
  }, []);

  const logout = useCallback(async () => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    try {
      await authApi.logout(token);
    } finally {
      clearSession();
      setUser(null);
    }
  }, []);

  const refreshProfile = useCallback(async () => {
    try {
      const profile = await authApi.me();
      if (profile) {
        localStorage.setItem(USER_KEY, JSON.stringify(profile));
        setUser(profile);
      }
      return profile;
    } catch {
      return null;
    }
  }, []);

  const role = getNormalizedRole(user);
  const isAdmin = role === "ADMIN";
  const isSuperAdmin = role === "SUPERADMIN";
  const isStaff = isAdmin || isSuperAdmin;
  const isSeller = role === "SELLER";

  const loginAdmin = useCallback(async (identifier, password) => {
    try {
      // Single backend login (POST /api/v1/auth/login) for staff and shoppers alike.
      // Staff separation is enforced client-side by verifying ADMIN/SUPERADMIN here
      // AND server-side via hasRole guards on /api/v1/admin/** and /api/v1/superadmin/**.
      const auth = await authApi.login({ identifier: identifier.trim(), password });
      const profile = auth?.user ?? null;
      const profileRole = getNormalizedRole(profile);
      if (profileRole !== "ADMIN" && profileRole !== "SUPERADMIN") {
        return { success: false, message: "This account does not have staff access." };
      }
      const stored = persistSession(auth);
      setUser(stored);
      return { success: true };
    } catch (err) {
      return { success: false, message: extractErrorMessage(err, "Invalid staff credentials.") };
    }
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        role,
        isAdmin,
        isSuperAdmin,
        isStaff,
        isSeller,
        isAuthenticated: !!user,
        initializing,
        login,
        loginAdmin,
        register,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
