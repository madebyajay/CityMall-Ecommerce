import { useEffect, useState } from "react";
import { adminUserApi } from "../api/adminApi";

function AdminCustomers() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [reason, setReason] = useState({});
  const [error, setError] = useState("");

  const load = async () => {
    try {
      const res = await adminUserApi.customers({ size: 50 });
      setItems(res.items);
      setTotal(res.total);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const res = await adminUserApi.customers({ size: 50 });
        if (mounted) {
          setItems(res.items);
          setTotal(res.total);
        }
      } catch (err) {
        if (mounted) setError(err.message);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  const toggleBlock = async (u) => {
    const blocked = !(u.activeStatus === false || u.status === "INACTIVE");
    try {
      await adminUserApi.blockCustomer(u.userId, blocked, reason[u.userId] || "");
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Customers ({total})</h1>
      {error && <p className="admin-error">{error}</p>}
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead><tr><th>ID</th><th>Login</th><th>Name</th><th>Email</th><th>Mobile</th><th>Status</th><th>Active</th><th>Reason</th><th>Action</th></tr></thead>
          <tbody>
            {items.map((u) => {
              const isBlocked = u.activeStatus === false || u.status === "INACTIVE";
              return (
                <tr key={u.userId}>
                  <td>{u.userId}</td>
                  <td>{u.userLoginId}</td>
                  <td>{u.userName}</td>
                  <td>{u.emailId}</td>
                  <td>{u.mobileNumber}</td>
                  <td>{u.status}</td>
                  <td>{String(u.activeStatus)}</td>
                  <td>
                    <input size="12" placeholder="reason (optional)" value={reason[u.userId] || ""}
                      onChange={(e) => setReason({ ...reason, [u.userId]: e.target.value })} />
                  </td>
                  <td>
                    <button className={isBlocked ? "admin-btn" : "admin-btn danger"} onClick={() => toggleBlock(u)}>
                      {isBlocked ? "Unblock" : "Block"}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AdminCustomers;
