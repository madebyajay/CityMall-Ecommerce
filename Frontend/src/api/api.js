import axios from "axios";

/**
 * Service base URLs (one per backend microservice — no gateway/discovery).
 * Override per-env via Frontend/.env:
 *   VITE_CUSTOMER_API_URL=http://localhost:8090
 *   VITE_PRODUCT_API_URL=http://localhost:8050
 *   VITE_ORDER_API_URL=http://localhost:8070
 *   VITE_PAYMENT_API_URL=http://localhost:8060
 */
export const CUSTOMER_API_URL =
  import.meta.env.VITE_CUSTOMER_API_URL || "http://localhost:8090";
export const PRODUCT_API_URL =
  import.meta.env.VITE_PRODUCT_API_URL || "http://localhost:8050";
export const ORDER_API_URL =
  import.meta.env.VITE_ORDER_API_URL || "http://localhost:8070";
export const PAYMENT_API_URL =
  import.meta.env.VITE_PAYMENT_API_URL || "http://localhost:8060";

export const ACCESS_TOKEN_KEY = "citymall_access_token";
export const REFRESH_TOKEN_KEY = "citymall_refresh_token";
export const USER_KEY = "citymall_user";

const API = axios.create({
  baseURL: CUSTOMER_API_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

// Attach Bearer token to every request (except refresh itself, which uses refresh token in body).
API.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401 (expired access token), try one silent refresh then retry the original request.
let refreshPromise = null;

async function tryRefresh() {
  if (!refreshPromise) {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) return Promise.reject(new Error("No refresh token"));
    refreshPromise = axios
      .post(`${CUSTOMER_API_URL}/api/v1/auth/refresh`, { refreshToken })
      .then((res) => {
        const data = res.data?.data;
        if (!data?.accessToken) throw new Error("Refresh failed");
        localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
        if (data.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
        if (data.user) localStorage.setItem(USER_KEY, JSON.stringify(data.user));
        return data.accessToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

function isAuthCall(url = "") {
  return (
    url.includes("/api/v1/auth/login") ||
    url.includes("/api/v1/auth/refresh") ||
    url.includes("/api/v1/auth/register")
  );
}

API.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const url = original?.url || "";

    if (status === 401 && !original._retry && !isAuthCall(url)) {
      original._retry = true;
      try {
        const newToken = await tryRefresh();
        original.headers.Authorization = `Bearer ${newToken}`;
        return API(original);
      } catch {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        // Keep USER_KEY cleanup to AuthContext logout; just reject here.
      }
    }
    return Promise.reject(error);
  }
);

/**
 * Factory for the other microservice clients (product / order / payment).
 * Same Bearer-token + silent-refresh behavior as the customer client.
 * Product/order public storefront endpoints need no token; admin ones do.
 */
export function createApi(baseURL) {
  const instance = axios.create({
    baseURL,
    headers: { "Content-Type": "application/json" },
    timeout: 15000,
  });

  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  instance.interceptors.response.use(
    (res) => res,
    async (error) => {
      const original = error.config;
      if (error.response?.status === 401 && !original._retry) {
        original._retry = true;
        try {
          const newToken = await tryRefresh();
          original.headers.Authorization = `Bearer ${newToken}`;
          return instance(original);
        } catch {
          /* fall through and reject */
        }
      }
      return Promise.reject(error);
    }
  );

  return instance;
}

export const productApi = createApi(PRODUCT_API_URL);
export const orderApi = createApi(ORDER_API_URL);
export const paymentApi = createApi(PAYMENT_API_URL);

export default API;
