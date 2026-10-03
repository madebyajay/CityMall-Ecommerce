import API from "../api/api";
import { extractErrorMessage } from "./authService";

/**
 * Customer-service self-service wrapper (profile, password, addresses).
 * All routes require Bearer token; id must be self unless staff.
 */

function unwrap(response) {
  return response.data?.data;
}

function fail(err, fallback) {
  throw new Error(extractErrorMessage(err, fallback));
}

export const userApi = {
  async profile() {
    return API.get("/api/v1/users/me").then(unwrap);
  },

  async update(id, payload) {
    try {
      return await API.put(`/api/v1/users/${id}`, payload).then(unwrap);
    } catch (err) {
      fail(err, "Profile update failed.");
    }
  },

  async changePassword(id, { oldPassword, newPassword }) {
    try {
      await API.patch(`/api/v1/users/${id}/password`, { oldPassword, newPassword });
      return true;
    } catch (err) {
      fail(err, "Password change failed.");
    }
  },

  async addresses(userId) {
    try {
      return await API.get(`/api/v1/users/${userId}/addresses`).then(unwrap);
    } catch (err) {
      fail(err, "Could not load addresses.");
    }
  },

  async addAddress(userId, payload) {
    try {
      const res = await API.post(`/api/v1/users/${userId}/addresses`, payload);
      return res.data?.data;
    } catch (err) {
      fail(err, "Could not save address.");
    }
  },

  async updateAddress(userId, addressId, payload) {
    try {
      return await API.put(`/api/v1/users/${userId}/addresses/${addressId}`, payload).then(unwrap);
    } catch (err) {
      fail(err, "Could not update address.");
    }
  },

  async deleteAddress(userId, addressId) {
    try {
      await API.delete(`/api/v1/users/${userId}/addresses/${addressId}`);
      return true;
    } catch (err) {
      fail(err, "Could not delete address.");
    }
  },
};
