import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../../../context/CartContext";
import { useAuth } from "../../../context/AuthContext";
import "./Navbar.css";

function Navbar() {
  const { cartItems } = useCart();
  const { user, role, isStaff, logout } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");

  const totalItems = cartItems.reduce((sum, item) => sum + item.quantity, 0);
  const displayName = user?.userName || user?.userLoginId || user?.name || "Account";

  const handleSearch = (e) => {
    e.preventDefault();
    if (query.trim()) navigate(`/products?q=${encodeURIComponent(query.trim())}`);
  };

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <nav className="navbar">
      <Link to="/" className="navbar-logo">
        City<span>Mall</span>
      </Link>
      <form className="navbar-search" onSubmit={handleSearch}>
        <input
          type="text"
          placeholder="Search for products, brands and more..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit">&#128269;</button>
      </form>
      <div className="navbar-actions">
        {isStaff && (
          <Link to="/admin" className="navbar-action-item" title={`Staff: ${role}`}>
            <span className="navbar-action-icon">&#128736;</span>
            <span className="navbar-action-label">Admin</span>
          </Link>
        )}
        {user ? (
          <div className="navbar-user">
            <span className="navbar-username" title={user?.emailId || user?.email || ""}>
              &#128100; {displayName}
            </span>
            <button
              className="navbar-action-item"
              style={{ border: "none", background: "none", cursor: "pointer" }}
              onClick={() => navigate("/profile")}
              title="My profile"
            >
              <span className="navbar-action-label">Profile</span>
            </button>
            <button className="navbar-logout-btn" onClick={handleLogout}>Logout</button>
          </div>
        ) : (
          <Link to="/login" className="navbar-action-item">
            <span className="navbar-action-icon">&#128100;</span>
            <span className="navbar-action-label">Sign In</span>
          </Link>
        )}
        <Link to="/orders" className="navbar-action-item">
          <span className="navbar-action-icon">&#128230;</span>
          <span className="navbar-action-label">Orders</span>
        </Link>
        <Link to="/cart" className="navbar-action-item">
          <span className="navbar-action-icon">
            &#128722;
            {totalItems > 0 && <span className="cart-badge">{totalItems}</span>}
          </span>
          <span className="navbar-action-label">Cart</span>
        </Link>
      </div>
    </nav>
  );
}

export default Navbar;
