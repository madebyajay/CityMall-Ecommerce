import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { orderApiService } from "../../services/orderService";
import Loader from "../../components/Loader/Loader";
import "./Orders.css";

const STATUS_COLORS = {
  PENDING:         { bg: "#e3f2fd", color: "#1565c0" },
  PAYMENT_PENDING: { bg: "#fff3e0", color: "#e65100" },
  PAID:            { bg: "#e3f2fd", color: "#1565c0" },
  CONFIRMED:       { bg: "#e8f5e9", color: "#2e7d32" },
  SHIPPED:         { bg: "#fff3e0", color: "#e65100" },
  DELIVERED:       { bg: "#e8f5e9", color: "#2e7d32" },
  CANCELLED:       { bg: "#fdecea", color: "#c62828" },
};

function Orders() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [linesByOrder, setLinesByOrder] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user?.userId) {
      setLoading(false);
      return;
    }
    let mounted = true;
    (async () => {
      try {
        const list = await orderApiService.myOrders(user.userId);
        if (!mounted) return;
        setOrders(Array.isArray(list) ? list : []);
        const pairs = await Promise.all(
          (Array.isArray(list) ? list : []).map(async (o) => [
            o.id,
            await orderApiService.orderLines(o.id),
          ])
        );
        if (mounted) setLinesByOrder(Object.fromEntries(pairs));
      } catch (err) {
        if (mounted) setError(err.message);
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [user]);

  if (loading) return <Loader />;

  return (
    <div className="container orders-page">

      <h2 className="orders-title">My Orders</h2>
      {error && <p className="orders-error">{error}</p>}

      {orders.length === 0 && !error ? (
        <div className="orders-empty">
          <span>📦</span>
          <p>No orders yet.</p>
          <button onClick={() => navigate("/products")}>Start Shopping</button>
        </div>
      ) : (
        <div className="orders-list">
          {orders.map((order) => {
            const statusStyle = STATUS_COLORS[order.status] || {};
            const lines = linesByOrder[order.id] || [];
            return (
              <div key={order.id} className="order-card">

                {/* Header */}
                <div className="order-card-header">
                  <div>
                    <p className="order-id">Order # {order.orderNumber || order.id}</p>
                    <p className="order-date">
                      {order.paymentMethod ? `Paid via ${String(order.paymentMethod).replace(/_/g, " ")}` : ""}
                    </p>
                  </div>
                  <div className="order-header-right">
                    <span
                      className="order-status"
                      style={{ background: statusStyle.bg, color: statusStyle.color }}
                    >
                      {String(order.status || "").replace(/_/g, " ")}
                    </span>
                    <span className="order-total">₹{Number(order.amount ?? 0).toLocaleString()}</span>
                  </div>
                </div>

                {/* Items */}
                {lines.length > 0 && (
                  <div className="order-items">
                    {lines.map((item, i) => (
                      <div key={i} className="order-item">
                        <div>
                          <p className="order-item-name">{item.productName || `Product #${item.productId}`}</p>
                          <p className="order-item-meta">
                            Qty: {item.quantity} · ₹{Number(item.price ?? 0).toLocaleString()}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default Orders;
