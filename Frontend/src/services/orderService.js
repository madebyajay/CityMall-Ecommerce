import { orderApi } from "../api/api";
import { extractErrorMessage } from "./authService";

/**
 * Order-service (:8070) shopper wrapper.
 *   POST /api/v1/orders                 { customerId: "<userId as string>", paymentMethod, products: [{productId, quantity}] }
 *   GET  /api/v1/orders/customer/{id}   list my orders
 *   GET  /api/v1/orders/{id}             single order
 *   GET  /api/v1/order-lines/order/{id}  lines (product names) for an order
 * paymentMethod ∈ CREDIT_CARD | UPI | WALLET | MASTER_CARD
 */

function unwrap(response) {
  return response.data?.data;
}

export const ORDER_PAYMENT_METHODS = [
  { value: "UPI", label: "UPI / GPay / PhonePe", icon: "📲" },
  { value: "CREDIT_CARD", label: "Credit / Debit Card", icon: "💳" },
  { value: "WALLET", label: "Wallet", icon: "👛" },
  { value: "MASTER_CARD", label: "Master Card", icon: "💳" },
];

export const orderApiService = {
  async createOrder({ customerId, paymentMethod, products }) {
    try {
      const res = await orderApi.post("/api/v1/orders", {
        customerId: String(customerId),
        paymentMethod,
        products: products.map((p) => ({
          productId: Number(p.productId ?? p.id),
          quantity: Number(p.quantity),
        })),
      });
      return unwrap(res);
    } catch (err) {
      throw new Error(extractErrorMessage(err, "Order could not be placed."));
    }
  },

  async myOrders(customerId) {
    try {
      const res = await orderApi.get(`/api/v1/orders/customer/${customerId}`);
      return unwrap(res) ?? [];
    } catch (err) {
      throw new Error(extractErrorMessage(err, "Could not load orders."));
    }
  },

  async orderById(id) {
    return orderApi.get(`/api/v1/orders/${id}`).then(unwrap);
  },

  async orderLines(orderId) {
    try {
      const res = await orderApi.get(`/api/v1/order-lines/order/${orderId}`);
      return res.data?.data ?? res.data ?? [];
    } catch {
      return [];
    }
  },
};
