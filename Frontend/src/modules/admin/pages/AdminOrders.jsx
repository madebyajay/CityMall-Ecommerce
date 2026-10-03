import { useEffect, useState } from "react";
import { orderAdminApi } from "../api/adminApi";

const STATUSES = ["PENDING", "PAYMENT_PENDING", "PAID", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"];

function AdminOrders() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [error, setError] = useState("");

  const load = async () => {
    try {
      const res = await orderAdminApi.list({ status, customerId, size: 50 });
      setItems(res.items);
      setTotal(res.total);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setOrderStatus = async (id, next) => {
    try {
      await orderAdminApi.setStatus(id, next);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Orders ({total})</h1>
      {error && <p className="admin-error">{error}</p>}
      <div className="admin-toolbar">
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <input placeholder="Customer ID" value={customerId} onChange={(e) => setCustomerId(e.target.value)} />
        <button className="admin-btn secondary" onClick={load}>Filter</button>
      </div>
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Order No</th><th>Customer</th><th>Amount</th><th>Payment</th><th>Status</th><th>Change</th></tr></thead>
          <tbody>
            {items.map((o) => (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.orderNumber}</td>
                <td>{o.customerId}</td>
                <td>₹{o.amount}</td>
                <td>{o.paymentMethod}</td>
                <td>{o.status}</td>
                <td>
                  <select defaultValue="" onChange={(e) => e.target.value && setOrderStatus(o.id, e.target.value)}>
                    <option value="">Set status…</option>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AdminOrders;
