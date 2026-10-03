import {
  ACCESS_TOKEN_KEY,
  CUSTOMER_API_URL,
  ORDER_API_URL,
  PRODUCT_API_URL,
  createApi,
} from "../../../api/api";
import { extractErrorMessage } from "../../../services/authService";

/**
 * Admin + Superadmin API layer across all microservices.
 * - Identity (customer-service :8090): /api/v1/admin/*, /api/v1/superadmin/*, /api/v1/roles, /api/v1/users
 * - Catalog (product-service :8050): /api/v1/admin/products/*, /api/v1/admin/categories/*
 * - Fulfillment (order-service :8070): /api/v1/admin/orders/*, /api/v1/admin/coupons/*
 * Bearer ADMIN JWT for /admin/*; SUPERADMIN JWT for /superadmin/* and role delete.
 */

const identityApi = createApi(CUSTOMER_API_URL);
const catalogApi = createApi(PRODUCT_API_URL);
const fulfillmentApi = createApi(ORDER_API_URL);

function unwrap(response) {
  return response.data?.data;
}

function fail(err, fallback) {
  throw new Error(extractErrorMessage(err, fallback));
}

// Keep the old 403 behaviour: a staff session that loses rights drops to admin login.
[identityApi, catalogApi, fulfillmentApi].forEach((instance) =>
  instance.interceptors.response.use(
    (res) => res,
    (error) => {
      if (error.response?.status === 403 && typeof window !== "undefined") {
        const path = window.location.pathname || "";
        if (path.startsWith("/admin") && !path.startsWith("/admin/login")) {
          window.location.assign("/admin/login");
        }
      }
      return Promise.reject(error);
    }
  )
);

export function getStoredToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

/* ---------------- identity: admin ---------------- */

export const adminUserApi = {
  async customers(params = {}) {
    try {
      const res = await identityApi.get("/api/v1/admin/customers", { params });
      const body = res.data?.data ?? res.data;
      return { items: body?.content ?? body ?? [], total: body?.totalElements ?? body?.length ?? 0 };
    } catch (err) {
      fail(err, "Could not load customers.");
    }
  },
  async sellers(params = {}) {
    try {
      const res = await identityApi.get("/api/v1/admin/sellers", { params });
      const body = res.data?.data ?? res.data;
      return { items: body?.content ?? body ?? [], total: body?.totalElements ?? body?.length ?? 0 };
    } catch (err) {
      fail(err, "Could not load sellers.");
    }
  },
  async getCustomer(id) {
    try {
      return await identityApi.get(`/api/v1/admin/customers/${id}`).then(unwrap);
    } catch (err) {
      fail(err, "Could not load customer.");
    }
  },
  async blockCustomer(id, blocked, reason) {
    try {
      return await identityApi
        .patch(`/api/v1/admin/customers/${id}/block`, { blocked, reason })
        .then(unwrap);
    } catch (err) {
      fail(err, "Block/unblock failed.");
    }
  },
  async createAdmin(payload) {
    try {
      return await identityApi.post("/api/v1/admin/create-admin", payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not create admin.");
    }
  },
  async createSeller(payload) {
    try {
      return await identityApi.post("/api/v1/admin/create-seller", payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not create seller.");
    }
  },
  async dashboard() {
    try {
      return await identityApi.get("/api/v1/admin/dashboard/summary").then(unwrap);
    } catch (err) {
      fail(err, "Could not load dashboard.");
    }
  },
};

export const adminRoleApi = {
  async list() {
    try {
      return (await identityApi.get("/api/v1/roles").then(unwrap)) ?? [];
    } catch (err) {
      fail(err, "Could not load roles.");
    }
  },
};

/* ---------------- identity: superadmin ---------------- */

export const superAdminApi = {
  async users({ query, role, page = 0, size = 20 } = {}) {
    try {
      const res = await identityApi.get("/api/v1/superadmin/users", {
        params: { query: query || undefined, role: role || undefined, page, size },
      });
      const body = res.data?.data ?? res.data;
      return { items: body?.content ?? [], total: body?.totalElements ?? 0, page: body };
    } catch (err) {
      fail(err, "Could not load users.");
    }
  },
  async getUser(id) {
    try {
      return await identityApi.get(`/api/v1/superadmin/users/${id}`).then(unwrap);
    } catch (err) {
      fail(err, "Could not load user.");
    }
  },
  async updateRole(id, roleName) {
    try {
      return await identityApi.patch(`/api/v1/superadmin/users/${id}/role`, { roleName }).then(unwrap);
    } catch (err) {
      fail(err, "Role update failed.");
    }
  },
  async setBlocked(id, blocked, reason) {
    try {
      return await identityApi
        .patch(`/api/v1/superadmin/users/${id}/block`, { blocked, reason })
        .then(unwrap);
    } catch (err) {
      fail(err, "Block/unblock failed.");
    }
  },
  async deleteUser(id) {
    try {
      await identityApi.delete(`/api/v1/superadmin/users/${id}`);
      return true;
    } catch (err) {
      fail(err, "Delete failed.");
    }
  },
  async createSuperAdmin(payload) {
    try {
      return await identityApi.post("/api/v1/superadmin/create-superadmin", payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not create superadmin.");
    }
  },
  async permissions() {
    try {
      return (await identityApi.get("/api/v1/superadmin/permissions").then(unwrap)) ?? [];
    } catch (err) {
      fail(err, "Could not load permissions.");
    }
  },
  async createPermission(payload) {
    try {
      return await identityApi.post("/api/v1/superadmin/permissions", payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not create permission.");
    }
  },
  async assignPermissions(roleName, permissionIds) {
    try {
      await identityApi.post("/api/v1/superadmin/permissions/assign", { roleName, permissionIds });
      return true;
    } catch (err) {
      fail(err, "Assign failed.");
    }
  },
  async revokePermission(roleName, apiEndpoint) {
    try {
      await identityApi.delete("/api/v1/superadmin/permissions", {
        params: { roleName, apiEndpoint },
      });
      return true;
    } catch (err) {
      fail(err, "Revoke failed.");
    }
  },
  async createRole(payload) {
    try {
      return await identityApi.post("/api/v1/roles", payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not create role.");
    }
  },
  async updateRoleDef(id, payload) {
    try {
      return await identityApi.put(`/api/v1/roles/${id}`, payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not update role.");
    }
  },
  async setRoleActive(id, active) {
    try {
      await identityApi.patch(`/api/v1/roles/${id}/status`, null, { params: { active } });
      return true;
    } catch (err) {
      fail(err, "Could not change role status.");
    }
  },
  async deleteRole(id) {
    try {
      await identityApi.delete(`/api/v1/roles/${id}`);
      return true;
    } catch (err) {
      fail(err, "Role delete failed.");
    }
  },
};

/* ---------------- catalog: product-service admin ---------------- */

export const productAdminApi = {
  async list({ categoryId, active, keyword, page = 0, size = 20 } = {}) {
    try {
      const res = await catalogApi.get("/api/v1/admin/products", {
        params: { categoryId, active, keyword, page, size },
      });
      const body = res.data?.data ?? res.data;
      return { items: body?.content ?? [], total: body?.totalElements ?? 0 };
    } catch (err) {
      fail(err, "Could not load products.");
    }
  },
  async create(payload) {
    try {
      return await catalogApi.post("/api/v1/admin/products", payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not create product.");
    }
  },
  async update(id, payload) {
    try {
      return await catalogApi.put(`/api/v1/admin/products/${id}`, payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not update product.");
    }
  },
  async remove(id) {
    try {
      await catalogApi.delete(`/api/v1/admin/products/${id}`);
      return true;
    } catch (err) {
      fail(err, "Could not delete product.");
    }
  },
  async setActive(id, active) {
    try {
      return await catalogApi.patch(`/api/v1/admin/products/${id}/status`, { active }).then(unwrap);
    } catch (err) {
      fail(err, "Could not change product status.");
    }
  },
  async adjustStock(id, quantityChange, reason) {
    try {
      return await catalogApi
        .patch(`/api/v1/admin/products/${id}/stock`, { quantityChange, reason })
        .then(unwrap);
    } catch (err) {
      fail(err, "Could not adjust stock.");
    }
  },
  async stockMovements(id) {
    try {
      return (await catalogApi.get(`/api/v1/admin/products/${id}/stock-movements`).then(unwrap)) ?? [];
    } catch (err) {
      fail(err, "Could not load stock movements.");
    }
  },
  async categories() {
    try {
      return (await catalogApi.get("/api/v1/admin/categories").then(unwrap)) ?? [];
    } catch (err) {
      fail(err, "Could not load categories.");
    }
  },
  async createCategory(payload) {
    try {
      return await catalogApi.post("/api/v1/admin/categories", payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not create category.");
    }
  },
  async updateCategory(id, payload) {
    try {
      return await catalogApi.put(`/api/v1/admin/categories/${id}`, payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not update category.");
    }
  },
  async deleteCategory(id) {
    try {
      await catalogApi.delete(`/api/v1/admin/categories/${id}`);
      return true;
    } catch (err) {
      fail(err, "Could not delete category.");
    }
  },
};

/* ---------------- fulfillment: order-service admin ---------------- */

export const orderAdminApi = {
  async list({ status, customerId, page = 0, size = 20 } = {}) {
    try {
      const res = await fulfillmentApi.get("/api/v1/admin/orders", {
        params: { status: status || undefined, customerId: customerId || undefined, page, size },
      });
      const body = res.data?.data ?? res.data;
      return { items: body?.content ?? [], total: body?.totalElements ?? 0 };
    } catch (err) {
      fail(err, "Could not load orders.");
    }
  },
  async get(id) {
    try {
      return await fulfillmentApi.get(`/api/v1/admin/orders/${id}`).then(unwrap);
    } catch (err) {
      fail(err, "Could not load order.");
    }
  },
  async setStatus(id, status) {
    try {
      return await fulfillmentApi.patch(`/api/v1/admin/orders/${id}/status`, { status }).then(unwrap);
    } catch (err) {
      fail(err, "Status update failed.");
    }
  },
  async coupons() {
    try {
      const res = await fulfillmentApi.get("/api/v1/admin/coupons").then(unwrap);
      return res?.content ?? res ?? [];
    } catch (err) {
      fail(err, "Could not load coupons.");
    }
  },
  async createCoupon(payload) {
    try {
      return await fulfillmentApi.post("/api/v1/admin/coupons", payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not create coupon.");
    }
  },
  async updateCoupon(id, payload) {
    try {
      return await fulfillmentApi.put(`/api/v1/admin/coupons/${id}`, payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not update coupon.");
    }
  },
  async setCouponActive(id, active) {
    try {
      await fulfillmentApi.patch(`/api/v1/admin/coupons/${id}/status`, { active });
      return true;
    } catch (err) {
      fail(err, "Coupon status change failed.");
    }
  },
  async deleteCoupon(id) {
    try {
      await fulfillmentApi.delete(`/api/v1/admin/coupons/${id}`);
      return true;
    } catch (err) {
      fail(err, "Coupon delete failed.");
    }
  },
};

export default identityApi;
