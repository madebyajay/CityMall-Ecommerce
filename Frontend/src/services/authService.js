import API from "../api/api";

/**
 * Thin wrapper over customer-service identity endpoints.
 * Backend wraps everything in ApiResponse { success, message, statusCode, data }.
 *
 *   POST /api/v1/auth/register  { userLoginId, password, userName, firstName?, lastName?, emailId?, mobileNumber, roleName? }
 *   POST /api/v1/auth/login     { identifier, password, forcedLogin? }  (identifier = loginId | email | mobile)
 *   POST /api/v1/auth/refresh   { refreshToken }
 *   POST /api/v1/auth/logout    (Authorization: Bearer <accessToken>, best-effort)
 *   GET  /api/v1/auth/me        (Bearer required)
 *   GET  /api/v1/auth/validate  (Bearer required)
 *   POST /api/v1/auth/otp/request { identifier }
 *   POST /api/v1/auth/reset-password { identifier, otp, newPassword }
 *   GET  /api/v1/auth/check-block?identifier=
 */

function unwrap(response) {
  return response.data?.data;
}

export function extractErrorMessage(err, fallback) {
  return (
    err?.response?.data?.message ||
    err?.response?.data?.data ||
    err?.message ||
    fallback ||
    "Something went wrong. Please try again."
  );
}

/** True when the backend rejected login due to an active session elsewhere. */
export function isActiveSessionError(err) {
  const msg = err?.response?.data?.message || "";
  return /already logged in|forcedLogin/i.test(msg);
}

export const authApi = {
  register(payload) {
    return API.post("/api/v1/auth/register", payload).then(unwrap);
  },
  login(payload) {
    return API.post("/api/v1/auth/login", payload).then(unwrap);
  },
  logout(accessToken) {
    return API.post(
      "/api/v1/auth/logout",
      {},
      accessToken ? { headers: { Authorization: `Bearer ${accessToken}` } } : undefined
    )
      .then((res) => res.data)
      .catch(() => null); // logout is best-effort; client clears tokens regardless
  },
  refresh(refreshToken) {
    return API.post("/api/v1/auth/refresh", { refreshToken }).then(unwrap);
  },
  me() {
    return API.get("/api/v1/auth/me").then(unwrap);
  },
  validate() {
    return API.get("/api/v1/auth/validate").then((res) => res.data);
  },
  requestOtp(identifier) {
    return API.post("/api/v1/auth/otp/request", { identifier }).then((res) => res.data);
  },
  resetPassword(payload) {
    return API.post("/api/v1/auth/reset-password", payload).then((res) => res.data);
  },
  checkBlock(identifier) {
    return API.get("/api/v1/auth/check-block", { params: { identifier } }).then(unwrap);
  },
};
